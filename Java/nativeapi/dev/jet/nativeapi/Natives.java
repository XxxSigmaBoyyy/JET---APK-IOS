package dev.jet.nativeapi;

import java.io.File;

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
                    return loadPath(f.getAbsolutePath());
                }
            }
        }
        System.err.println("[jet.natives] loadLibrary(" + name + "): not found under " + dirs);
        return false;
    }

    /** Absolute-path System.load substitute. */
    public static boolean load(String absPath) {
        return loadPath(absPath);
    }

    public static String javaSym(String classBinaryName, String method) {
        return "Java_" + classBinaryName.replace('.', '_').replace('/', '_') + "_" + method;
    }
}
