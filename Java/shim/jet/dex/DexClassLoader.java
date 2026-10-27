package jet.dex;

import com.googlecode.d2j.dex.ClassVisitorFactory;
import com.googlecode.d2j.dex.Dex2Asm;
import com.googlecode.d2j.dex.DexExceptionHandler;
import com.googlecode.d2j.dex.ExDex2Asm;
import com.googlecode.d2j.dex.LambadaNameSafeClassAdapter;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.converter.IR2JConverter;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.dex2jar.ir.IrMethod;
import org.objectweb.asm.*;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.*;

/**
 * Lazy, per-class DEX -> JVM class loader (stock APK, no ahead-of-time conversion, no prebuilt module).
 * findClass(name) reads exactly that class_def from the app's classes*.dex with dex2jar's reader, lowers it through
 * dex2jar's IR (the same transform pipeline as d2j-dex2jar's defaults) to JVM bytecode, post-processes it like the
 * harness (FixJarFrames / JarItfFix / System.loadLibrary -> partner interpreter) and defines it. Translated classes are
 * cached on disk (cacheDir) so later launches skip translation. Java resources come from the APK itself.
 */
public final class DexClassLoader extends ClassLoader {
    static { registerAsParallelCapable(); }
    static final int READ = DexFileReader.SKIP_DEBUG;

    final class Dex { final DexFileReader r; final DexFileNode meta = new DexFileNode(); Map<String, Object> clz; Dex(DexFileReader r) { this.r = r; } }
    final List<Dex> dexes = new ArrayList<>();
    final Map<String, Object[]> index = new HashMap<>();       // binary name -> {Dex, Integer idx}
    final Set<String> interfaces = new HashSet<>();             // internal names
    final Map<String, String[]> supers = new HashMap<>();       // internal name -> interfaces
    final File cacheDir; final ZipFile apk; final File apkFile;
    public static final AtomicLong translated = new AtomicLong(), cacheHits = new AtomicLong(), translateNanos = new AtomicLong();
    public static volatile String nativesOwner = "dev/jet/nativeapi/Natives";

    public DexClassLoader(File apk, File cacheDir, ClassLoader parent) throws IOException {
        super(parent);
        this.apkFile = apk; this.apk = new ZipFile(apk); this.cacheDir = cacheDir;
        if (cacheDir != null) cacheDir.mkdirs();
        List<String> names = new ArrayList<>();
        for (Enumeration<? extends ZipEntry> e = this.apk.entries(); e.hasMoreElements(); ) { String n = e.nextElement().getName(); if (n.matches("classes\\d*\\.dex")) names.add(n); }
        names.sort(Comparator.comparingInt(n -> n.equals("classes.dex") ? 1 : Integer.parseInt(n.substring(7, n.length() - 4))));
        for (String n : names) {
            byte[] b; try (InputStream in = this.apk.getInputStream(this.apk.getEntry(n))) { b = in.readAllBytes(); }
            Dex d = new Dex(new DexFileReader(b));
            // class headers only (no code): names, access flags, supertypes, InnerClass/EnclosingClass annotations
            d.r.accept(d.meta, DexFileReader.SKIP_CODE | DexFileReader.SKIP_DEBUG | DexFileReader.SKIP_FIELD_CONSTANT | DexFileReader.IGNORE_READ_EXCEPTION);
            int i = 0;
            for (String cn : d.r.getClassNames()) {
                // dex2jar's LambadaNameSafeClassAdapter maps '-' to '_' in every type name (D8 $-CC / -$$Lambda classes)
                String bin = cn.substring(1, cn.length() - 1).replace('/', '.').replace('-', '_');
                index.putIfAbsent(bin, new Object[]{d, i});   // first dex wins (ART multidex order)
                i++;
            }
            if (d.meta.clzs != null) for (DexClassNode c : d.meta.clzs) {
                String in = c.className.substring(1, c.className.length() - 1).replace('-', '_');
                if ((c.access & Opcodes.ACC_INTERFACE) != 0) interfaces.add(in);
                String[] is = c.interfaceNames == null ? new String[0] : new String[c.interfaceNames.length];
                for (int k = 0; k < is.length; k++) is[k] = c.interfaceNames[k].substring(1, c.interfaceNames[k].length() - 1).replace('-', '_');
                supers.put(in, is);
            }
            dexes.add(d);
        }
    }

    public int classCount() { return index.size(); }
    public boolean hasClass(String name) { return index.containsKey(name); }

    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // Framework (shim + JDK) first for platform namespaces; the app's own copies of everything else win (as on ART,
                // where the boot class path only holds the framework).
                boolean platform = name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("jet.") || name.startsWith("dev.jet.")
                        || name.startsWith("android.") || name.startsWith("dalvik.") || name.startsWith("libcore.") || name.startsWith("com.android.internal.")
                        || name.startsWith("org.objectweb.asm.") || name.startsWith("com.googlecode.");
                if (platform || !index.containsKey(name)) {
                    try { c = getParent().loadClass(name); }
                    catch (ClassNotFoundException e) {
                        if (!index.containsKey(name)) {
                            // framework class the shim lacks: SDK-model stub so the app links (logged in SdkStubs.MISSING)
                            byte[] st = name.startsWith("android.") || name.startsWith("com.android.") || name.startsWith("dalvik.")
                                    ? SdkStubs.generate(name, n -> loadClass(n, false)) : null;
                            if (st == null) throw e;
                            c = defineClass(name, st, 0, st.length);
                        }
                    }
                }
                if (c == null) c = findClass(name);
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = classBytes(name);
        if (b == null) throw new ClassNotFoundException(name);
        return defineClass(name, b, 0, b.length);
    }

    /** JVM class file for a DEX class (cached), or null if the APK has no such class. */
    public byte[] classBytes(String name) throws ClassNotFoundException {
        Object[] e = index.get(name);
        if (e == null) return null;
        File cf = cacheDir == null ? null : new File(cacheDir, name.replace('.', '/') + ".class");
        if (cf != null && cf.isFile()) { try { cacheHits.incrementAndGet(); return Files.readAllBytes(cf.toPath()); } catch (IOException ignore) {} }
        long t0 = System.nanoTime();
        byte[] raw;
        try { raw = translate((Dex) e[0], (Integer) e[1]); }
        catch (Throwable t) { throw new ClassNotFoundException(name + " (dex translation failed: " + t + ")", t); }
        byte[] out = postProcess(raw);
        translateNanos.addAndGet(System.nanoTime() - t0); translated.incrementAndGet();
        if (cf != null) { try { cf.getParentFile().mkdirs(); File tmp = new File(cf.getPath() + ".tmp"); Files.write(tmp.toPath(), out); Files.move(tmp.toPath(), cf.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (IOException ignore) {} }
        return out;
    }

    // ---- dex2jar per class, same IR pipeline as Dex2jar defaults (reuseReg/topoSort/optimizeSynchronized off) ----
    static final DexExceptionHandler HANDLER = new DexExceptionHandler() {
        public void handleFileException(Exception e) { System.err.println("[jet.dex] " + e); }
        public void handleMethodTranslateException(Method m, DexMethodNode n, MethodVisitor mv, Exception e) {
            System.err.println("[jet.dex] method " + m + ": " + e);
            // like d2j: replace the body with `throw new RuntimeException("d2j fail translate: ...")`
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException"); mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("d2j fail translate: " + e); mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(Opcodes.ATHROW); mv.visitMaxs(-1, -1); mv.visitEnd();
        }
    };
    static final class D2A extends ExDex2Asm {
        D2A() { super(HANDLER); }
        @Override public void optimize(IrMethod irMethod) {
            T_cleanLabel.transform(irMethod);
            T_deadCode.transform(irMethod); T_removeLocal.transform(irMethod); T_removeConst.transform(irMethod);
            T_zero.transform(irMethod);
            if (T_npe.transformReportChanged(irMethod)) { T_deadCode.transform(irMethod); T_removeLocal.transform(irMethod); T_removeConst.transform(irMethod); }
            T_new.transform(irMethod); T_fillArray.transform(irMethod); T_agg.transform(irMethod); T_multiArray.transform(irMethod); T_voidInvoke.transform(irMethod);
            T_deadCode.transform(irMethod); T_removeLocal.transform(irMethod); T_removeConst.transform(irMethod);
            T_type.transform(irMethod); T_unssa.transform(irMethod); T_ir2jRegAssign.transform(irMethod); T_trimEx.transform(irMethod);
        }
        @Override public void ir2j(IrMethod irMethod, MethodVisitor mv, ClzCtx clzCtx) {
            new IR2JConverter().optimizeSynchronized(false).clzCtx(clzCtx).ir(irMethod).asm(mv).convert();
        }
    }

    byte[] translate(Dex d, int idx) {
        Map<String, Object> clz;
        synchronized (d) {
            if (d.clz == null) d.clz = collect(d.meta);
            clz = d.clz;
        }
        DexFileNode one = new DexFileNode();
        synchronized (d) { d.r.accept(one, idx, READ); }   // DexFileReader shares one ByteBuffer: reads are serialised per dex
        if (one.clzs == null || one.clzs.isEmpty()) throw new IllegalStateException("class_def " + idx + " unreadable");
        final byte[][] out = new byte[1][];
        ClassVisitorFactory cvf = new ClassVisitorFactory() {
            public ClassVisitor create(String name) {
                final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                final LambadaNameSafeClassAdapter rca = new LambadaNameSafeClassAdapter(cw);
                return new ClassVisitor(Opcodes.ASM9, rca) { @Override public void visitEnd() { super.visitEnd(); out[0] = cw.toByteArray(); } };
            }
        };
        convert(new D2A(), one, one.clzs.get(0), cvf, clz);
        if (out[0] == null) throw new IllegalStateException("no class emitted");
        return out[0];
    }

    // Dex2Asm.collectClzInfo / convertClass(Map<String, Clz>) take the package-private Clz type: call them reflectively.
    static java.lang.reflect.Method COLLECT, CONVERT;
    @SuppressWarnings("unchecked")
    static Map<String, Object> collect(DexFileNode meta) {
        try {
            if (COLLECT == null) { COLLECT = Dex2Asm.class.getDeclaredMethod("collectClzInfo", DexFileNode.class); COLLECT.setAccessible(true); }
            return (Map<String, Object>) COLLECT.invoke(null, meta);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    static void convert(Dex2Asm d2a, DexFileNode file, DexClassNode c, ClassVisitorFactory cvf, Map<String, Object> clz) {
        try {
            if (CONVERT == null) { CONVERT = Dex2Asm.class.getMethod("convertClass", DexFileNode.class, DexClassNode.class, ClassVisitorFactory.class, Map.class); }
            CONVERT.invoke(d2a, file, c, cvf, clz);
        } catch (java.lang.reflect.InvocationTargetException e) { Throwable t = e.getCause(); if (t instanceof RuntimeException) throw (RuntimeException) t; throw new RuntimeException(t); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    // ---- post-processing (harness FixJarFrames + JarItfFix + JetAppClassLoader.rewriteLoads) ----
    static final String[] BOX = {null, "java/lang/Boolean", "java/lang/Character", "java/lang/Byte", "java/lang/Short", "java/lang/Integer", "java/lang/Float", "java/lang/Long", "java/lang/Double"};
    byte[] postProcess(byte[] raw) {
        ClassReader cr = new ClassReader(raw);
        final String self = cr.getClassName();
        final boolean[] anon = {false}, enclNoMethod = {false};
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public void visitOuterClass(String o, String nm, String d) { if (nm == null) enclNoMethod[0] = true; }
            @Override public void visitInnerClass(String nm, String outer, String in, int acc) { if (nm.equals(self) && in == null) anon[0] = true; }
        }, ClassReader.SKIP_CODE);
        final boolean dropEncl = enclNoMethod[0] && !anon[0];
        final boolean isItf = (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
        // Interfaces with default/static methods (other than <clinit>) need class file >= 52; everything else stays 49.
        final boolean[] java8 = {false};
        if (isItf) cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int acc, String mn, String d, String sg, String[] ex) {
                if ((acc & Opcodes.ACC_ABSTRACT) == 0 && !mn.equals("<clinit>")) java8[0] = true; return null; }
        }, ClassReader.SKIP_CODE);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public void visit(int version, int access, String name, String sig, String superName, String[] itfs) {
                // Dex has no StackMapTable: emit pre-Java-6 class files (old type-inferencing verifier) unless the class needs
                // Java 8 features (interface default/static methods), which become 52 and need -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote (no StackMapTable).
                super.visit(java8[0] ? Opcodes.V1_8 : Opcodes.V1_5, access, name, sig, superName, itfs);
            }
            @Override public void visitOuterClass(String o, String nm, String d) { if (!dropEncl) super.visitOuterClass(o, nm, d); }
            @Override public void visitNestHost(String h) {}
            @Override public void visitNestMember(String m) {}
            @Override public void visitPermittedSubclass(String p) {}
            @Override public RecordComponentVisitor visitRecordComponent(String n, String d, String s) { return null; }
            @Override public MethodVisitor visitMethod(int acc, String mn, String desc, String sig, String[] ex) {
                if ((acc & Opcodes.ACC_NATIVE) != 0) { nativeStub(super.visitMethod(acc & ~Opcodes.ACC_NATIVE, mn, desc, sig, ex), self, acc, mn, desc); return null; }
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(acc, mn, desc, sig, ex)) {
                    @Override public void visitFrame(int t, int nl, Object[] l, int ns, Object[] s) {}
                    @Override public void visitLdcInsn(Object v) {
                        if (v instanceof Type) {   // d8 Array.newInstance(float.class...) -> ldc class "F": legalise to Float.TYPE
                            Type t = (Type) v; String in = t.getSort() == Type.OBJECT ? t.getInternalName() : null;
                            int k = in != null && in.length() == 1 ? "ZCBSIFJD".indexOf(in.charAt(0)) : -1;
                            if (k >= 0) { super.visitFieldInsn(Opcodes.GETSTATIC, BOX[k + 1], "TYPE", "Ljava/lang/Class;"); return; }
                        }
                        super.visitLdcInsn(v);
                    }
                    @Override public void visitMethodInsn(int op, String owner, String name, String d, boolean itf) {
                        if (op == Opcodes.INVOKESTATIC && owner.equals("java/lang/System") && d.equals("(Ljava/lang/String;)V")
                                && (name.equals("loadLibrary") || name.equals("load"))) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, nativesOwner, name, "(Ljava/lang/String;)Z", false);
                            super.visitInsn(Opcodes.POP); return;
                        }
                        if ((op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKESPECIAL) && supers.containsKey(owner)) itf = interfaces.contains(owner);
                        if (op == Opcodes.INVOKESPECIAL && itf && !owner.equals(self) && interfaces.contains(owner)) {
                            String[] direct = supers.get(self);
                            if (direct != null && !Arrays.asList(direct).contains(owner))
                                for (String dd : direct) if (itfExtends(dd, owner, 0)) { owner = dd; break; }
                        }
                        super.visitMethodInsn(op, owner, name, d, itf);
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
    /** `native` app method -> Java body dispatching to the guest JNI function (dev.jet.nativeapi.Natives.jni). */
    static void nativeStub(MethodVisitor mv, String owner, int acc, String name, String desc) {
        boolean st = (acc & Opcodes.ACC_STATIC) != 0;
        Type[] at = Type.getArgumentTypes(desc); Type rt = Type.getReturnType(desc);
        mv.visitCode();
        mv.visitLdcInsn(Type.getObjectType(owner)); mv.visitLdcInsn(name); mv.visitLdcInsn(desc);
        if (st) mv.visitInsn(Opcodes.ACONST_NULL); else mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(at.length); mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        int slot = st ? 0 : 1;
        for (int i = 0; i < at.length; i++) {
            Type t = at[i];
            mv.visitInsn(Opcodes.DUP); mv.visitLdcInsn(i); mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot);
            int k = "ZCBSIFJD".indexOf(t.getDescriptor().charAt(0));
            if (t.getSort() <= Type.DOUBLE && k >= 0)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, BOX[k + 1], "valueOf", "(" + t.getDescriptor() + ")L" + BOX[k + 1] + ";", false);
            mv.visitInsn(Opcodes.AASTORE);
            slot += t.getSize();
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, nativesOwner, "jni", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        if (rt.getSort() == Type.VOID) { mv.visitInsn(Opcodes.POP); mv.visitInsn(Opcodes.RETURN); }
        else if (rt.getSort() <= Type.DOUBLE) {
            int k = "ZCBSIFJD".indexOf(rt.getDescriptor().charAt(0)); String box = BOX[k + 1];
            String[] unb = {"booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue"};
            mv.visitTypeInsn(Opcodes.CHECKCAST, box);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, box, unb[k], "()" + rt.getDescriptor(), false);
            mv.visitInsn(rt.getOpcode(Opcodes.IRETURN));
        } else { mv.visitTypeInsn(Opcodes.CHECKCAST, rt.getSort() == Type.ARRAY ? rt.getDescriptor() : rt.getInternalName()); mv.visitInsn(Opcodes.ARETURN); }
        mv.visitMaxs(-1, -1); mv.visitEnd();
    }

    boolean itfExtends(String c, String target, int depth) {
        if (depth > 32) return false;
        String[] is = supers.get(c); if (is == null) return false;
        for (String i : is) if (i.equals(target) || itfExtends(i, target, depth + 1)) return true;
        return false;
    }

    // ---- resources: the APK backs ClassLoader.getResource (Java resources at the APK root) ----
    @Override protected URL findResource(String name) {
        if (apk.getEntry(name) == null) return null;
        try { return new URL("jar:" + apkFile.toURI() + "!/" + name); } catch (IOException e) { return null; }
    }
    @Override protected Enumeration<URL> findResources(String name) {
        URL u = findResource(name); return Collections.enumeration(u == null ? Collections.emptyList() : Collections.singletonList(u));
    }
}
