package jet.gl;
/** Loads the host GLES/EGL JNI bridge (native/jetgl/libjetgl.so over Mesa). Linux-only: the iPhone build binds the same Java surface to arm64 GLES. */
public final class JetGL {
    private static boolean loaded; private static Throwable error;
    public static synchronized void load() {
        if (loaded || error != null) return;
        String p = System.getProperty("jet.gl.lib", new java.io.File("native/jetgl/libjetgl.so").getAbsolutePath());
        try { System.load(p); loaded = true; System.err.println("[JETGL] loaded " + p); }
        catch (Throwable t) { error = t; System.err.println("[JETGL] cannot load " + p + ": " + t); }
    }
    public static boolean available() { load(); return loaded; }
}
