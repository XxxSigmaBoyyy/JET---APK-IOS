package jet.dex;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Framework classes the shim does not implement yet: synthesise them from the Android SDK model (android.jar member
 * signatures, code stripped) so app code links, like the harness StubClassLoader. Every public/protected method returns a
 * default and is logged once (STUB_CALLS); constructors chain to the best matching shim superclass constructor; constant
 * fields keep their values; enums get their real shape. Generic: nothing here is app-specific.
 */
public final class SdkStubs {
    private SdkStubs() {}
    public static final List<String> MISSING = Collections.synchronizedList(new ArrayList<>());
    public static final Set<String> STUB_CALLS = Collections.synchronizedSet(new LinkedHashSet<>());
    public static void jetStubCall(String sig) { if (STUB_CALLS.add(sig)) System.err.println("[STUB-CALL] " + sig); }
    public static Object jetAlloc(Class<?> c) {
        try { java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); return ((sun.misc.Unsafe) f.get(null)).allocateInstance(c); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    static final Map<String, String> SDK_URIS = Map.of(
        "android.provider.Settings$System.DEFAULT_NOTIFICATION_URI", "content://settings/system/notification_sound",
        "android.provider.Settings$System.DEFAULT_RINGTONE_URI", "content://settings/system/ringtone",
        "android.provider.Settings$System.DEFAULT_ALARM_ALERT_URI", "content://settings/system/alarm_alert",
        "android.provider.Settings$System.CONTENT_URI", "content://settings/system",
        "android.provider.Settings$Secure.CONTENT_URI", "content://settings/secure",
        "android.provider.Settings$Global.CONTENT_URI", "content://settings/global");
    public static android.net.Uri jetSdkUri(String cls, String field) {
        String v = SDK_URIS.get(cls + "." + field);
        return android.net.Uri.parse(v != null ? v : "content://jet.sdkstub/" + cls + "/" + field);
    }

    static ZipFile model;
    static synchronized ClassNode modelClass(String internal) {
        try {
            if (model == null) {
                String p = System.getProperty("jet.android.model", "");
                if (p.isEmpty() || !new File(p).isFile()) return null;
                model = new ZipFile(p);
            }
            ZipEntry e = model.getEntry(internal + ".class");
            if (e == null) return null;
            ClassNode cn = new ClassNode();
            try (InputStream in = model.getInputStream(e)) { new ClassReader(in).accept(cn, ClassReader.SKIP_CODE); }
            return cn;
        } catch (IOException ex) { return null; }
    }

    public interface Resolver { Class<?> load(String binaryName) throws ClassNotFoundException; }

    /** Class file for a missing framework class, or null if the SDK model does not have it. */
    public static byte[] generate(String name, Resolver res) {
        String internal = name.replace('.', '/');
        ClassNode cn = modelClass(internal);
        if (cn == null) return null;
        MISSING.add(name); System.err.println("[MISSING-CLASS] " + name + " (SDK stub)");
        boolean itf = (cn.access & Opcodes.ACC_INTERFACE) != 0;
        String superInternal = itf ? "java/lang/Object" : (cn.superName != null ? cn.superName : "java/lang/Object");
        String[] ifaces = cn.interfaces.toArray(new String[0]);
        Class<?> sup;
        try { sup = itf ? Object.class : res.load(superInternal.replace('/', '.')); } catch (ClassNotFoundException e) { sup = null; }
        if (sup == null) superInternal = "java/lang/Object";
        if (sup == null) sup = Object.class;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int acc = itf ? (Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)
                      : (Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | (cn.access & (Opcodes.ACC_ENUM | Opcodes.ACC_ABSTRACT)));
        cw.visit(Opcodes.V1_8, acc, internal, null, superInternal, ifaces);
        for (FieldNode f : cn.fields) {
            if ((f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) continue;
            int fa = f.access & ~Opcodes.ACC_ENUM; if (itf) fa |= Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            cw.visitField(fa, f.name, f.desc, null, f.value).visitEnd();
        }
        final boolean isEnum = !itf && "java/lang/Enum".equals(superInternal);
        List<String> enumConsts = new ArrayList<>();
        if (isEnum) {
            for (FieldNode f : cn.fields) if ((f.access & Opcodes.ACC_ENUM) != 0) enumConsts.add(f.name);
            String arr = "[L" + internal + ";";
            cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, "$VALUES", arr, null, null).visitEnd();
            MethodVisitor ec = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
            ec.visitCode(); ec.visitVarInsn(Opcodes.ALOAD, 0); ec.visitVarInsn(Opcodes.ALOAD, 1); ec.visitVarInsn(Opcodes.ILOAD, 2);
            ec.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false); ec.visitInsn(Opcodes.RETURN); ec.visitMaxs(0, 0); ec.visitEnd();
            MethodVisitor vs = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()" + arr, null, null);
            vs.visitCode(); vs.visitFieldInsn(Opcodes.GETSTATIC, internal, "$VALUES", arr);
            vs.visitMethodInsn(Opcodes.INVOKEVIRTUAL, arr, "clone", "()Ljava/lang/Object;", false); vs.visitTypeInsn(Opcodes.CHECKCAST, arr);
            vs.visitInsn(Opcodes.ARETURN); vs.visitMaxs(0, 0); vs.visitEnd();
            MethodVisitor vo = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + internal + ";", null, null);
            vo.visitCode(); vo.visitLdcInsn(Type.getObjectType(internal)); vo.visitVarInsn(Opcodes.ALOAD, 0);
            vo.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false); vo.visitTypeInsn(Opcodes.CHECKCAST, internal);
            vo.visitInsn(Opcodes.ARETURN); vo.visitMaxs(0, 0); vo.visitEnd();
        }
        MethodVisitor ci = null;
        if (isEnum && !enumConsts.isEmpty()) {
            ci = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); ci.visitCode();
            ci.visitLdcInsn(enumConsts.size()); ci.visitTypeInsn(Opcodes.ANEWARRAY, internal);
            for (int k = 0; k < enumConsts.size(); k++) {
                ci.visitInsn(Opcodes.DUP); ci.visitLdcInsn(k);
                ci.visitTypeInsn(Opcodes.NEW, internal); ci.visitInsn(Opcodes.DUP); ci.visitLdcInsn(enumConsts.get(k)); ci.visitLdcInsn(k);
                ci.visitMethodInsn(Opcodes.INVOKESPECIAL, internal, "<init>", "(Ljava/lang/String;I)V", false);
                ci.visitInsn(Opcodes.DUP); ci.visitFieldInsn(Opcodes.PUTSTATIC, internal, enumConsts.get(k), "L" + internal + ";");
                ci.visitInsn(Opcodes.AASTORE);
            }
            ci.visitFieldInsn(Opcodes.PUTSTATIC, internal, "$VALUES", "[L" + internal + ";");
        }
        for (FieldNode f : cn.fields) {
            if (f.value != null || !f.desc.equals("Landroid/net/Uri;") || (f.access & Opcodes.ACC_STATIC) == 0 || (f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) continue;
            if (ci == null) { ci = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); ci.visitCode(); }
            ci.visitLdcInsn(name); ci.visitLdcInsn(f.name);
            ci.visitMethodInsn(Opcodes.INVOKESTATIC, "jet/dex/SdkStubs", "jetSdkUri", "(Ljava/lang/String;Ljava/lang/String;)Landroid/net/Uri;", false);
            ci.visitFieldInsn(Opcodes.PUTSTATIC, internal, f.name, f.desc);
        }
        if (ci != null) { ci.visitInsn(Opcodes.RETURN); ci.visitMaxs(0, 0); ci.visitEnd(); }
        boolean anyCtor = false;
        for (MethodNode m : cn.methods) {
            if ((m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 || (m.access & Opcodes.ACC_SYNTHETIC) != 0) continue;
            if (m.name.equals("<clinit>")) continue;
            if (isEnum && (m.name.equals("<init>") || m.name.equals("values") || m.name.equals("valueOf") && m.desc.startsWith("(Ljava/lang/String;)"))) continue;
            if (m.name.equals("<init>")) {
                if (itf) continue;
                java.lang.reflect.Constructor<?> best = null; int bestLen = -1; Type[] mine = Type.getArgumentTypes(m.desc);
                for (java.lang.reflect.Constructor<?> c : sup.getDeclaredConstructors()) {
                    if (java.lang.reflect.Modifier.isPrivate(c.getModifiers())) continue;
                    Class<?>[] ps = c.getParameterTypes(); if (ps.length > mine.length) continue;
                    boolean ok = true; for (int i = 0; i < ps.length && ok; i++) ok = Type.getType(ps[i]).equals(mine[i]);
                    if (ok && ps.length > bestLen) { best = c; bestLen = ps.length; }
                }
                if (best == null) continue;
                MethodVisitor mv = cw.visitMethod(m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_VARARGS), "<init>", m.desc, null, null);
                mv.visitCode(); mv.visitVarInsn(Opcodes.ALOAD, 0);
                int slot = 1; for (int i = 0; i < bestLen; i++) { mv.visitVarInsn(mine[i].getOpcode(Opcodes.ILOAD), slot); slot += mine[i].getSize(); }
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternal, "<init>", Type.getConstructorDescriptor(best), false);
                mv.visitInsn(Opcodes.RETURN); mv.visitMaxs(0, 0); mv.visitEnd(); anyCtor = true;
                continue;
            }
            boolean st = (m.access & Opcodes.ACC_STATIC) != 0;
            if (!st && inheritsConcrete(sup, m.name, m.desc)) continue;
            if (itf && !st) { cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, m.name, m.desc, null, null).visitEnd(); continue; }
            if (!st && (m.access & Opcodes.ACC_ABSTRACT) != 0 && (acc & Opcodes.ACC_ABSTRACT) != 0) { cw.visitMethod(m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT), m.name, m.desc, null, null).visitEnd(); continue; }
            MethodVisitor mv = cw.visitMethod(m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC | Opcodes.ACC_VARARGS), m.name, m.desc, null, null);
            mv.visitCode();
            mv.visitLdcInsn(name + "." + m.name + m.desc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "jet/dex/SdkStubs", "jetStubCall", "(Ljava/lang/String;)V", false);
            Type rt = Type.getReturnType(m.desc);
            switch (rt.getSort()) {
                case Type.VOID: mv.visitInsn(Opcodes.RETURN); break;
                case Type.BOOLEAN: case Type.CHAR: case Type.BYTE: case Type.SHORT: case Type.INT: mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); break;
                case Type.LONG: mv.visitInsn(Opcodes.LCONST_0); mv.visitInsn(Opcodes.LRETURN); break;
                case Type.FLOAT: mv.visitInsn(Opcodes.FCONST_0); mv.visitInsn(Opcodes.FRETURN); break;
                case Type.DOUBLE: mv.visitInsn(Opcodes.DCONST_0); mv.visitInsn(Opcodes.DRETURN); break;
                default:
                    String rin = rt.getSort() == Type.OBJECT ? rt.getInternalName() : "";
                    if (!st && rin.equals(internal)) mv.visitVarInsn(Opcodes.ALOAD, 0);
                    else if (st && rin.equals(internal) && !itf && (cn.access & Opcodes.ACC_ABSTRACT) == 0 && !isEnum) {
                        mv.visitLdcInsn(Type.getObjectType(internal));
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "jet/dex/SdkStubs", "jetAlloc", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, internal);
                    }
                    else if (rin.equals("java/util/List") || rin.equals("java/util/Collection") || rin.equals("java/lang/Iterable")) mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false);
                    else if (rin.equals("java/util/Set")) mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptySet", "()Ljava/util/Set;", false);
                    else if (rin.equals("java/util/Map")) mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false);
                    else if (rt.getSort() == Type.ARRAY && rt.getDimensions() == 1 && rt.getElementType().getSort() != Type.OBJECT) { mv.visitInsn(Opcodes.ICONST_0); mv.visitIntInsn(Opcodes.NEWARRAY, new int[]{0, Opcodes.T_BOOLEAN, Opcodes.T_CHAR, Opcodes.T_BYTE, Opcodes.T_SHORT, Opcodes.T_INT, Opcodes.T_FLOAT, Opcodes.T_LONG, Opcodes.T_DOUBLE}[rt.getElementType().getSort()]); }
                    else if (rt.getSort() == Type.ARRAY && rt.getDimensions() == 1 && rt.getElementType().getInternalName().startsWith("java/")) { mv.visitInsn(Opcodes.ICONST_0); mv.visitTypeInsn(Opcodes.ANEWARRAY, rt.getElementType().getInternalName()); }
                    else mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
            }
            mv.visitMaxs(0, 0); mv.visitEnd();
        }
        if (!itf && !anyCtor && !isEnum) {
            boolean noArg; try { sup.getDeclaredConstructor(); noArg = true; } catch (NoSuchMethodException e) { noArg = false; }
            if (noArg) {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode(); mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternal, "<init>", "()V", false);
                mv.visitInsn(Opcodes.RETURN); mv.visitMaxs(0, 0); mv.visitEnd();
            }
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
    static boolean inheritsConcrete(Class<?> c, String name, String desc) {
        for (Class<?> k = c; k != null; k = k.getSuperclass())
            for (java.lang.reflect.Method m : k.getDeclaredMethods())
                if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc) && !java.lang.reflect.Modifier.isPrivate(m.getModifiers())) return true;
        return false;
    }

    /** Build the SDK model: android.jar classes with code stripped (signatures, constants, hierarchy only). */
    public static void main(String[] a) throws IOException {
        int n = 0;
        try (ZipFile in = new ZipFile(a[0]); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(a[1]))) {
            out.setLevel(9);
            for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements(); ) {
                ZipEntry ze = e.nextElement(); String nm = ze.getName();
                if (!nm.endsWith(".class") || !(nm.startsWith("android/") || nm.startsWith("com/android/") || nm.startsWith("dalvik/"))) continue;
                ClassReader cr; try (InputStream is = in.getInputStream(ze)) { cr = new ClassReader(is); }
                ClassWriter cw = new ClassWriter(0);
                cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                    @Override public MethodVisitor visitMethod(int acc, String n2, String d, String s, String[] ex) {
                        if ((acc & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) return null;
                        return super.visitMethod(acc, n2, d, null, null);   // signature only (read back with SKIP_CODE)
                    }
                    @Override public FieldVisitor visitField(int acc, String n2, String d, String s, Object v) {
                        return (acc & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 ? null : super.visitField(acc, n2, d, null, v);
                    }
                    @Override public AnnotationVisitor visitAnnotation(String d, boolean v) { return null; }
                    @Override public void visitSource(String s, String d) {}
                    @Override public void visitInnerClass(String n2, String o, String i, int acc) {}
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                out.putNextEntry(new ZipEntry(nm)); out.write(cw.toByteArray()); out.closeEntry(); n++;
            }
        }
        System.out.println("sdk model: " + n + " classes -> " + a[1] + " (" + new File(a[1]).length() / 1024 + " KB)");
    }
}
