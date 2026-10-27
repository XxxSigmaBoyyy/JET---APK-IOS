package android.util;
/** API 30 CloseGuard: records the open() call site; warnIfOpen() logs it, like AOSP with leak reporting enabled. */
public final class CloseGuard {
    private Throwable mSite;
    public CloseGuard() {}
    public void open(String closeMethodName) { if (closeMethodName == null) throw new NullPointerException("closeMethodName == null"); mSite = new Throwable("Explicit termination method '" + closeMethodName + "' not called"); }
    public void close() { mSite = null; }
    public void warnIfOpen() { if (mSite != null) System.err.println("[jet-closeguard] A resource was acquired at attached stack trace but never released: " + mSite.getMessage()); }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
