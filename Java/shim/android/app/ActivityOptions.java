package android.app;
/** ActivityOptions: a value object serialised to a Bundle (animations are not rendered headless). */
public class ActivityOptions {
    public static final String EXTRA_USAGE_TIME_REPORT = "android.activity.usage_time";
    public static final String EXTRA_USAGE_TIME_REPORT_PACKAGES = "android.usage_time_packages";
    public static final int MODE_BACKGROUND_ACTIVITY_START_ALLOWED = 1;
    public static final int MODE_BACKGROUND_ACTIVITY_START_DENIED = 2;
    public static final int MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED = 0;
    private final android.os.Bundle mB = new android.os.Bundle();
    ActivityOptions() {}
    public static ActivityOptions makeBasic() { return new ActivityOptions(); }
    public static ActivityOptions makeCustomAnimation(android.content.Context c, int enter, int exit) { ActivityOptions o = new ActivityOptions(); o.mB.putInt("android:activity.animEnterRes", enter); o.mB.putInt("android:activity.animExitRes", exit); return o; }
    public static ActivityOptions makeCustomAnimation(android.content.Context c, int enter, int exit, int bg) { return makeCustomAnimation(c, enter, exit); }
    public static ActivityOptions makeScaleUpAnimation(android.view.View v, int x, int y, int w, int h) { return new ActivityOptions(); }
    public static ActivityOptions makeClipRevealAnimation(android.view.View v, int x, int y, int w, int h) { return new ActivityOptions(); }
    public static ActivityOptions makeThumbnailScaleUpAnimation(android.view.View v, android.graphics.Bitmap b, int x, int y) { return new ActivityOptions(); }
    public static ActivityOptions makeSceneTransitionAnimation(Activity a, android.view.View v, String name) { return new ActivityOptions(); }
    public static ActivityOptions makeTaskLaunchBehind() { return new ActivityOptions(); }
    public android.os.Bundle toBundle() { return new android.os.Bundle(mB); }
    public void update(ActivityOptions o) { if (o != null) mB.putAll(o.mB); }
    public ActivityOptions setLaunchDisplayId(int id) { mB.putInt("android.activity.launchDisplayId", id); return this; }
    public int getLaunchDisplayId() { return mB.getInt("android.activity.launchDisplayId", -1); }
    public ActivityOptions setLaunchBounds(android.graphics.Rect r) { return this; } public android.graphics.Rect getLaunchBounds() { return null; }
    public ActivityOptions setLockTaskEnabled(boolean b) { return this; } public boolean getLockTaskMode() { return false; }
    public ActivityOptions setShareIdentityEnabled(boolean b) { mB.putBoolean("android:activity.shareIdentity", b); return this; } public boolean isShareIdentityEnabled() { return mB.getBoolean("android:activity.shareIdentity", false); }
    public ActivityOptions setPendingIntentBackgroundActivityStartMode(int m) { mB.putInt("android.pendingIntent.backgroundActivityAllowed", m); return this; }
    public int getPendingIntentBackgroundActivityStartMode() { return mB.getInt("android.pendingIntent.backgroundActivityAllowed", 0); }
    public ActivityOptions setPendingIntentBackgroundActivityLaunchAllowed(boolean b) { return setPendingIntentBackgroundActivityStartMode(b ? 1 : 2); }
    public boolean isPendingIntentBackgroundActivityLaunchAllowed() { return getPendingIntentBackgroundActivityStartMode() != 2; }
    public ActivityOptions setSplashScreenStyle(int s) { mB.putInt("android.activity.splashScreenStyle", s); return this; } public int getSplashScreenStyle() { return mB.getInt("android.activity.splashScreenStyle", 0); }
    public ActivityOptions setAppVerificationBundle(android.os.Bundle b) { return this; }
    public void requestUsageTimeReport(PendingIntent p) {}
    @Override public String toString() { return "ActivityOptions(" + hashCode() + ")"; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private int jet_PendingIntentCreatorBackgroundActivityStartMode;
    public static android.app.ActivityOptions makeSceneTransitionAnimation(android.app.Activity p0, android.util.Pair[] p1) { jet.runtime.ShimStubs.hit("android.app.ActivityOptions.makeSceneTransitionAnimation(Landroid/app/Activity;[Landroid/util/Pair;)Landroid/app/ActivityOptions;"); return null; }
    public static android.app.ActivityOptions makeLaunchIntoPip(android.app.PictureInPictureParams p0) { jet.runtime.ShimStubs.hit("android.app.ActivityOptions.makeLaunchIntoPip(Landroid/app/PictureInPictureParams;)Landroid/app/ActivityOptions;"); return null; }
    public android.app.ActivityOptions setPendingIntentCreatorBackgroundActivityStartMode(int p0) { jet.runtime.ShimStubs.hit("android.app.ActivityOptions.setPendingIntentCreatorBackgroundActivityStartMode(I)Landroid/app/ActivityOptions;"); return null; }
    public int getPendingIntentCreatorBackgroundActivityStartMode() { return jet_PendingIntentCreatorBackgroundActivityStartMode; }
    // ---- END generated SDK surface ----
}
