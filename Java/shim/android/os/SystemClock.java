package android.os;
/**
 * Virtual clock: wall time plus an offset that the host's main-looper pump advances when it fast-forwards to the next
 * delayed message. Handler delays, CountDownTimer, Scroller and animators all read this clock, so they agree.
 */
public final class SystemClock {
    private static volatile long sOffset;
    public static long elapsedRealtime() { return System.currentTimeMillis() + sOffset; }
    public static long uptimeMillis() { return System.currentTimeMillis() + sOffset; }
    public static long elapsedRealtimeNanos() { return System.nanoTime() + sOffset * 1_000_000L; }
    public static long uptimeNanos() { return elapsedRealtimeNanos(); }
    public static long currentThreadTimeMillis() { return uptimeMillis(); }
    public static boolean setCurrentTimeMillis(long m) { return false; }
    public static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    /** Host only: advance virtual time. */
    public static synchronized void jetAdvance(long ms) { if (ms > 0) sOffset += ms; }
    public static long jetOffset() { return sOffset; }
}
