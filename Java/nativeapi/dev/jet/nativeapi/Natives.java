package dev.jet.nativeapi;

/**
 * JVM-facing entry for APK native libraries.
 * Shim {@code System.loadLibrary}/{@code Runtime.load0} should call {@link #loadPath}
 * with the extracted arm64-v8a {@code .so} absolute path.
 * Partner implements the C side ({@code jet_native_*}); this class is bound via JNI from
 * {@code jet_jni_bridge.c}.
 */
public final class Natives {
    private Natives() {}
    /** Load one APK {@code .so} (already extracted on disk) and run its {@code JNI_OnLoad}. */
    public static native boolean loadPath(String absPath);
    /** {@code jet_native_sym} — e.g. {@code Java_com_foo_Bar_baz} when RegisterNatives was not used. */
    public static native long resolve(String libPath, String sym);
    /** {@code jet_native_call}; {@code x} up to 8 GP args, {@code d} up to 8 FP; returns x0. */
    public static native long call(long guestFn, long[] x, double[] d);

    /** Build the standard JNI export name for a method (dots/slashes → underscore). */
    public static String javaSym(String classBinaryName, String method) {
        return "Java_" + classBinaryName.replace('.', '_').replace('/', '_') + "_" + method;
    }
}
