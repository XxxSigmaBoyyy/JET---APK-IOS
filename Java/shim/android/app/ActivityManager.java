package android.app;
public class ActivityManager {
    /** API 28+: the user has not restricted this app's background activity on the JET host. */
    public boolean isBackgroundRestricted() { return false; }
    public static class TaskDescription { public TaskDescription() {} public TaskDescription(String l) {} public TaskDescription(String l, android.graphics.Bitmap i) {} public TaskDescription(String l, android.graphics.Bitmap i, int c) {} public TaskDescription(String l, int icon, int c) {} public TaskDescription(String l, int icon) {}
        public String getLabel() { return null; } public int getPrimaryColor() { return 0; }
        public static final class Builder { public Builder() {} public Builder setLabel(String l) { return this; } public Builder setIcon(int i) { return this; } public Builder setPrimaryColor(int c) { return this; } public TaskDescription build() { return new TaskDescription(); } } }
    public static class MemoryInfo { public long availMem = 2L << 30, totalMem = 4L << 30, threshold = 256L << 20; public boolean lowMemory; }
    public static class RunningAppProcessInfo { public static final int IMPORTANCE_FOREGROUND = 100; public int importance = IMPORTANCE_FOREGROUND, pid; public String processName; public String[] pkgList; }
    public void getMemoryInfo(MemoryInfo o) {} public int getMemoryClass() { return 256; } public int getLargeMemoryClass() { return 512; } public boolean isLowRamDevice() { return false; }
    public java.util.List<RunningAppProcessInfo> getRunningAppProcesses() { return new java.util.ArrayList<>(); } public static boolean isUserAMonkey() { return false; } public static boolean isRunningInTestHarness() { return false; }
    public static void getMyMemoryState(RunningAppProcessInfo o) { o.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND; }
    public java.util.List<?> getAppTasks() { return new java.util.ArrayList<>(); }
    // API 30: the process has no recorded previous exits (fresh install every run)
    public java.util.List<ApplicationExitInfo> getHistoricalProcessExitReasons(String pkg, int pid, int max) { return java.util.Collections.emptyList(); }
    public void setProcessStateSummary(byte[] state) {}
}
