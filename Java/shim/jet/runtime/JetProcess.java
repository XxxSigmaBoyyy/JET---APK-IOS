package jet.runtime;
/** Process exit with Android semantics: the app process just dies (no JVM shutdown hooks, no native atexit / C++ static destructors).
 *  See native/jetproc/jetproc.c. Falls back to System.exit if the helper is unavailable. Linux-only harness helper. */
public final class JetProcess {
    private JetProcess() {}
    private static native void nativeExit(int code);
    public static void exit(int code) {
        System.out.flush(); System.err.flush();
        try { System.load(new java.io.File(System.getProperty("jet.proc.lib", "native/jetproc/libjetproc.so")).getAbsolutePath()); nativeExit(code); }
        catch (Throwable t) { System.err.println("[jet-proc] _exit helper unavailable (" + t + "), using System.exit"); }
        System.exit(code);
    }
}
