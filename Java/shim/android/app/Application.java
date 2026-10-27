package android.app;
import android.content.Context;
import android.content.res.Resources;
public class Application extends android.content.ContextWrapper implements android.content.ComponentCallbacks2 {
    /** The process's Application (manifest android:name), created by the harness before the first Activity, like ActivityThread. */
    public static volatile Application jetCurrent;
    public Application() { super(null); }
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity a, android.os.Bundle b); void onActivityStarted(Activity a); void onActivityResumed(Activity a); void onActivityPaused(Activity a); void onActivityStopped(Activity a); void onActivitySaveInstanceState(Activity a, android.os.Bundle b); void onActivityDestroyed(Activity a);
        // API 29+ pre/post hooks (AndroidX Lifecycle's ReportFragment.LifecycleCallbacks relies on these at SDK >= 29)
        default void onActivityPreCreated(Activity a, android.os.Bundle b) {} default void onActivityPostCreated(Activity a, android.os.Bundle b) {}
        default void onActivityPreStarted(Activity a) {} default void onActivityPostStarted(Activity a) {}
        default void onActivityPreResumed(Activity a) {} default void onActivityPostResumed(Activity a) {}
        default void onActivityPrePaused(Activity a) {} default void onActivityPostPaused(Activity a) {}
        default void onActivityPreStopped(Activity a) {} default void onActivityPostStopped(Activity a) {}
        default void onActivityPreSaveInstanceState(Activity a, android.os.Bundle b) {} default void onActivityPostSaveInstanceState(Activity a, android.os.Bundle b) {}
        default void onActivityPreDestroyed(Activity a) {} default void onActivityPostDestroyed(Activity a) {}
    }
    public interface OnProvideAssistDataListener { void onProvideAssistData(Activity a, android.os.Bundle data); }
    private final java.util.List<ActivityLifecycleCallbacks> jetCallbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks c) { jetCallbacks.add(c); }
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks c) { jetCallbacks.remove(c); }
    public java.util.List<ActivityLifecycleCallbacks> jetLifecycleCallbacks() { return jetCallbacks; }
    public static String getProcessName() { return jetCurrent != null ? jetCurrent.getPackageName() : "app"; }
    public void onCreate() {}
    public void onTerminate() {}
    public void jetAttach(Resources r) { mResources = r; attachBaseContext(new android.content.Context(r)); }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {}
    public void onLowMemory() {}
    public void onTrimMemory(int level) {}
}
