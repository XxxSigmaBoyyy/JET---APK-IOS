package android.os;
/** Host process facts (Linux-only harness): one app process, pid/tid from the JVM, app uid 10000 + no isolation, 64-bit. */
public class Process {
    public static final int ROOT_UID = 0, SYSTEM_UID = 1000, PHONE_UID = 1001, SHELL_UID = 2000, FIRST_APPLICATION_UID = 10000, LAST_APPLICATION_UID = 19999,
        BLUETOOTH_UID = 1002, WIFI_UID = 1010, NFC_UID = 1027, INVALID_UID = -1, LAST_ISOLATED_UID = 99999, FIRST_ISOLATED_UID = 99000;
    public static final int SIGNAL_QUIT = 3, SIGNAL_KILL = 9, SIGNAL_USR1 = 10;
    public static final int THREAD_PRIORITY_DEFAULT = 0, THREAD_PRIORITY_LOWEST = 19, THREAD_PRIORITY_BACKGROUND = 10, THREAD_PRIORITY_FOREGROUND = -2,
        THREAD_PRIORITY_DISPLAY = -4, THREAD_PRIORITY_URGENT_DISPLAY = -8, THREAD_PRIORITY_VIDEO = -10, THREAD_PRIORITY_AUDIO = -16, THREAD_PRIORITY_URGENT_AUDIO = -19,
        THREAD_PRIORITY_MORE_FAVORABLE = -1, THREAD_PRIORITY_LESS_FAVORABLE = 1;
    private static final long START_ELAPSED = SystemClock.elapsedRealtime(), START_UPTIME = SystemClock.uptimeMillis();
    private static final java.util.Map<Long, Integer> PRIO = new java.util.concurrent.ConcurrentHashMap<>();
    public Process() {}
    public static final long getElapsedCpuTime() {
        try { java.lang.management.ThreadMXBean b = java.lang.management.ManagementFactory.getThreadMXBean(); long t = 0; for (long id : b.getAllThreadIds()) { long c = b.getThreadCpuTime(id); if (c > 0) t += c; } return t / 1_000_000L; }
        catch (Throwable e) { return (System.nanoTime() - START_NANOS) / 1_000_000L; } // no java.management on iOS
    }
    private static final long START_NANOS = System.nanoTime();
    public static long getStartElapsedRealtime() { return START_ELAPSED; }
    public static long getStartUptimeMillis() { return START_UPTIME; }
    public static long getStartRequestedElapsedRealtime() { return START_ELAPSED; }
    public static long getStartRequestedUptimeMillis() { return START_UPTIME; }
    public static final boolean is64Bit() { return true; }
    public static final int myPid() { return (int) ProcessHandle.current().pid(); }
    public static final int myTid() { return (int) Thread.currentThread().getId(); }
    public static final int myUid() { return FIRST_APPLICATION_UID; }
    public static UserHandle myUserHandle() { return UserHandle.getUserHandleForUid(myUid()); }
    public static boolean isApplicationUid(int uid) { int a = uid % 100000; return a >= FIRST_APPLICATION_UID && a <= LAST_APPLICATION_UID; }
    public static final boolean isIsolated() { return false; }
    public static final boolean isSdkSandbox() { return false; }
    public static final int getUidForName(String name) { return -1; }
    public static final int getGidForName(String name) { return -1; }
    private static void checkPrio(int p) { if (p < -20 || p > 19) throw new IllegalArgumentException("Priority out of range: " + p); }
    public static final void setThreadPriority(int tid, int priority) { checkPrio(priority); PRIO.put((long) tid, priority); }
    public static final int[] getExclusiveCores() { return new int[0]; }
    public static final void setThreadPriority(int priority) { checkPrio(priority); PRIO.put(Thread.currentThread().getId(), priority); }
    public static final int getThreadPriority(int tid) { return PRIO.getOrDefault((long) tid, THREAD_PRIORITY_DEFAULT); }
    public static final boolean supportsProcesses() { return true; }
    public static String myProcessName() { String p = System.getProperty("jet.package"); return p != null ? p : "app"; }
    /** Android would kill the app here. The harness must still write its report, so a self-kill is logged and ignored (Linux-only harness choice). */
    public static final void killProcess(int pid) { if (pid == myPid()) System.err.println("[jet-proc] killProcess(self) requested (ignored by harness)"); }
    public static final void sendSignal(int pid, int signal) { if (pid == myPid() && signal == SIGNAL_KILL) killProcess(pid); }
}
