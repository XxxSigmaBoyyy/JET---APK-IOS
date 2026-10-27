package android.app;
/** Headless device: no lock screen and no secure credential set (as on a fresh emulator). */
public class KeyguardManager {
    KeyguardManager() {}
    public interface OnKeyguardExitResult { void onKeyguardExitResult(boolean success); }
    public abstract static class KeyguardDismissCallback { public void onDismissError() {} public void onDismissSucceeded() {} public void onDismissCancelled() {} }
    public boolean isKeyguardLocked() { return false; } public boolean isKeyguardSecure() { return false; }
    public boolean isDeviceLocked() { return false; } public boolean isDeviceSecure() { return false; }
    public boolean inKeyguardRestrictedInputMode() { return false; }
    public void requestDismissKeyguard(Activity a, KeyguardDismissCallback cb) { if (cb != null) cb.onDismissSucceeded(); }
    public android.content.Intent createConfirmDeviceCredentialIntent(CharSequence title, CharSequence desc) { return null; /* AOSP: null when no secure lock screen */ }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public void exitKeyguardSecurely(android.app.KeyguardManager.OnKeyguardExitResult p0) { jet.runtime.ShimStubs.hit("android.app.KeyguardManager.exitKeyguardSecurely(Landroid/app/KeyguardManager$OnKeyguardExitResult;)V"); }
    // ---- END generated SDK surface ----
}
