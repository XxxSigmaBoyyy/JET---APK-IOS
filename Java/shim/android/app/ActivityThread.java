package android.app;
/** Hidden android.app.ActivityThread: apps and native libs (JNI_OnLoad of Signal/Element X deps) reach it reflectively for the process Application. */
public final class ActivityThread {
    private static final ActivityThread sCurrent = new ActivityThread();
    private ActivityThread() {}
    public static ActivityThread currentActivityThread() { return sCurrent; }
    public static Application currentApplication() { return Application.jetCurrent; }
    public static String currentPackageName() { Application a = Application.jetCurrent; return a == null ? null : a.getPackageName(); }
    public static String currentProcessName() { return currentPackageName(); }
    public static String currentOpPackageName() { return currentPackageName(); }
    public Application getApplication() { return Application.jetCurrent; }
    public static boolean isSystem() { return false; }
    public android.os.Looper getLooper() { return android.os.Looper.getMainLooper(); }
}
