package dev.jet.nativeapi;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM-facing entry for APK native libraries (arm64-v8a .so).
 * System.loadLibrary / Runtime.load0 should be redirected here (bytecode rewrite or
 * PathClassLoader.findLibrary). Partner implements jet_native_* in C; this class is
 * bound via JNI from jet_jni_bridge.c.
 */
public final class Natives {
    private Natives() {}

    /** Load one APK .so (absolute path) and run its JNI_OnLoad via the interpreter. */
    public static native boolean loadPath(String absPath);

    /** Guest process cwd (getcwd, relative paths) for the partner runtime; must be absolute. */
    public static native boolean setCwd(String absDir);
    /** Loader used when guest JNI FindClass misses on the system loader (Android: the loadLibrary caller's loader). */
    public static native void setAppLoader(ClassLoader cl);
    private static ClassLoader loaderSet;
    private static String cwdSet;
    /** Point the guest cwd at the package's absolute getFilesDir() (jet.files.dir, else jet.data.dir/jet.package/files). */
    static synchronized void ensureCwd() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null && cl != loaderSet && cl != ClassLoader.getSystemClassLoader()) {
            try { setAppLoader(cl); loaderSet = cl; } catch (UnsatisfiedLinkError e) { System.err.println("[jet.natives] setAppLoader unavailable: " + e); }
        }
        String d = System.getProperty("jet.files.dir");
        if (d == null) {
            String data = System.getProperty("jet.data.dir"), pkg = System.getProperty("jet.package");
            if (data == null || pkg == null || pkg.isEmpty()) return;
            d = new File(new File(data, pkg), "files").getAbsolutePath();
        }
        File f = new File(d).getAbsoluteFile(); f.mkdirs();
        if (f.getPath().equals(cwdSet)) return;
        try { if (setCwd(f.getPath())) { cwdSet = f.getPath(); System.err.println("[jet.natives] guest cwd=" + cwdSet); } }
        catch (UnsatisfiedLinkError e) { System.err.println("[jet.natives] setCwd unavailable: " + e); }
    }

    /** jet_native_sym — e.g. Java_com_foo_Bar_baz when RegisterNatives was not used. */
    public static native long resolve(String libPath, String sym);

    /** jet_native_call; x up to 8 GP args, d up to 8 FP; returns x0. */
    public static native long call(long guestFn, long[] x, double[] d);

    /**
     * Resolve lib&lt;name&gt;.so under jet.native.lib.dir (colon-separated) and loadPath.
     * Drop-in body for rewritten System.loadLibrary(name) call sites.
     */
    public static synchronized boolean loadLibrary(String name) {
        if (name == null || name.isEmpty()) return false;
        String dirs = System.getProperty("jet.native.lib.dir", ".");
        for (String dir : dirs.split(":")) {
            if (dir.isEmpty()) continue;
            String[] cands = {
                dir + "/lib" + name + ".so",
                dir + "/" + name + ".so",
                dir + "/arm64-v8a/lib" + name + ".so",
                dir + "/lib/arm64-v8a/lib" + name + ".so",
            };
            for (String p : cands) {
                File f = new File(p);
                if (f.isFile()) {
                    System.err.println("[jet.natives] loadLibrary(" + name + ") -> " + f.getAbsolutePath());
                    ensureCwd();
                    return loadPath(f.getAbsolutePath());
                }
            }
        }
        System.err.println("[jet.natives] loadLibrary(" + name + "): not found under " + dirs);
        return false;
    }

    /** Absolute-path System.load substitute. */
    public static boolean load(String absPath) {
        ensureCwd();
        return loadPath(absPath);
    }

    // ---- Java -> guest JNI dispatch. jet.dex.DexClassLoader rewrites every `native` method of a translated app class
    // into `return (T) Natives.jni(Owner.class, "name", "desc", this|null, new Object[]{args...})`.

    /** Guest function registered by JNI_OnLoad's RegisterNatives for cls/name/sig (0 if none). */
    public static native long registered(String cls, String name, String sig);
    /** Symbol lookup across every loaded guest library (0 if none). */
    public static native long resolveAny(String sym);
    /** Call a guest JNI function: x0 = guest JNIEnv, x1 = receiver (object or class), then the arguments of `desc`
     *  (integer-class args from prims, float/double raw bits from fbits, references from refs, in order).
     *  Returns x0, or the raw bits of d0 for F/D. */
    static native long invoke0(long fn, Object recv, String desc, long[] prims, long[] fbits, Object[] refs);
    /** Same for reference-returning methods (x0 is a host reference). */
    static native Object invokeRef0(long fn, Object recv, String desc, long[] prims, long[] fbits, Object[] refs);

    static final ConcurrentHashMap<String, Long> FN = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Boolean> SEEN = new ConcurrentHashMap<>();
    /** The first native method an app called, for diagnostics (null until then). */
    public static volatile String firstCall;

    /** -Djet.natives.stats=true: wall time spent inside each guest JNI method (per thread), printed at exit. */
    static final boolean STATS = Boolean.getBoolean("jet.natives.stats");
    static final ConcurrentHashMap<String, long[]> STAT = new ConcurrentHashMap<>();
    static {
        if (STATS) Runtime.getRuntime().addShutdownHook(new Thread(Natives::dumpStats, "jet-natives-stats"));
    }
    public static void dumpStats() {
        java.util.List<java.util.Map.Entry<String, long[]>> l = new java.util.ArrayList<>(STAT.entrySet());
        l.sort((x, y) -> Long.compare(y.getValue()[1], x.getValue()[1]));
        System.err.println("[jet.natives] stats: top guest JNI time (thread | method | calls | ms)");
        for (int i = 0; i < Math.min(25, l.size()); i++)
            System.err.println("[jet.natives]   " + l.get(i).getKey() + " | " + l.get(i).getValue()[0] + " | " + l.get(i).getValue()[1] / 1000000);
    }

    public static Object jni(Class<?> owner, String name, String desc, Object self, Object[] args) {
        if (!STATS) return jni0(owner, name, desc, self, args);
        long t0 = System.nanoTime();
        try { return jni0(owner, name, desc, self, args); }
        finally {
            long dt = System.nanoTime() - t0; String t = Thread.currentThread().getName();
            if (t.startsWith("DispatchQueuePool")) t = "DispatchQueuePool*";
            long[] a = STAT.computeIfAbsent(t + " | " + owner.getName() + "." + name, k -> new long[2]);
            synchronized (a) { a[0]++; a[1] += dt; }
        }
    }

    static Object jni0(Class<?> owner, String name, String desc, Object self, Object[] args) {
        String cls = owner.getName().replace('.', '/');
        String key = cls + "." + name + desc;
        if (firstCall == null) firstCall = key;
        Long fn = FN.get(key);
        if (fn == null) { fn = lookup(cls, name, desc); FN.put(key, fn); }
        if (SEEN.putIfAbsent(key, Boolean.TRUE) == null)
            System.err.println("[jet.natives] first call " + key + " -> " + (fn == 0 ? "UNBOUND" : "guest 0x" + Long.toHexString(fn)) + strArgs(args));
        if (fn == 0) throw new UnsatisfiedLinkError("'" + key + "' (no guest JNI function registered or exported)");
        int n = args.length, pi = 0, fi = 0, ri = 0;
        long[] prims = new long[n]; long[] fbits = new long[n]; Object[] refs = new Object[n];
        int i = desc.indexOf('(') + 1, a = 0;
        while (desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            Object v = args[a++];
            switch (c) {
                case 'Z': prims[pi++] = ((Boolean) v) ? 1 : 0; i++; break;
                case 'B': prims[pi++] = (Byte) v; i++; break;
                case 'C': prims[pi++] = (Character) v; i++; break;
                case 'S': prims[pi++] = (Short) v; i++; break;
                case 'I': prims[pi++] = (Integer) v; i++; break;
                case 'J': prims[pi++] = (Long) v; i++; break;
                case 'F': fbits[fi++] = Float.floatToRawIntBits((Float) v) & 0xffffffffL; i++; break;
                case 'D': fbits[fi++] = Double.doubleToRawLongBits((Double) v); i++; break;
                default: {
                    refs[ri++] = v;
                    while (desc.charAt(i) == '[') i++;
                    if (desc.charAt(i) == 'L') i = desc.indexOf(';', i);
                    i++;
                }
            }
        }
        char r = desc.charAt(i + 1);
        Object recv = self != null ? self : owner;
        if (r == 'L' || r == '[') return invokeRef0(fn, recv, desc, prims, fbits, refs);
        long v = invoke0(fn, recv, desc, prims, fbits, refs);
        switch (r) {
            case 'V': return null;
            case 'Z': return (v & 0xff) != 0;
            case 'B': return (byte) v;
            case 'C': return (char) v;
            case 'S': return (short) v;
            case 'I': return (int) v;
            case 'J': return v;
            case 'F': return Float.intBitsToFloat((int) v);
            case 'D': return Double.longBitsToDouble(v);
            default: throw new IllegalStateException(desc);
        }
    }

    static long lookup(String cls, String name, String desc) {
        long fn = registered(cls, name, desc);
        if (fn != 0) return fn;
        String shortSym = "Java_" + mangle(cls) + "_" + mangle(name);
        fn = resolveAny(shortSym);
        if (fn != 0) return fn;
        return resolveAny(shortSym + "__" + mangle(desc.substring(1, desc.indexOf(')'))));
    }

    /** JNI name mangling (JNI spec, "Resolving Native Method Names"). */
    static String mangle(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '.') b.append('_');
            else if (c == '_') b.append("_1");
            else if (c == ';') b.append("_2");
            else if (c == '[') b.append("_3");
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) b.append(c);
            else b.append(String.format("_0%04x", (int) c));
        }
        return b.toString();
    }

    public static String javaSym(String classBinaryName, String method) {
        return "Java_" + classBinaryName.replace('.', '_').replace('/', '_') + "_" + method;
    }

    /** String arguments of a first call (paths, keys), for diagnostics. */
    static String strArgs(Object[] args) {
        StringBuilder b = new StringBuilder();
        for (Object o : args) if (o instanceof String) {
            String v = (String) o;
            b.append(b.length() == 0 ? " args=[" : ", ").append('"').append(v.length() > 160 ? v.substring(0, 160) + "..." : v).append('"');
        }
        return b.length() == 0 ? "" : b.append(']').toString();
    }
}
