package android.os;
/** Headless power manager: newWakeLock must return a usable WakeLock (Telegram LoginActivity). */
public class PowerManager {
    public static final int PARTIAL_WAKE_LOCK = 1;
    public static final int SCREEN_DIM_WAKE_LOCK = 6;
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 10;
    public static final int FULL_WAKE_LOCK = 26;
    public static final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;
    public static final int ON_AFTER_RELEASE = 0x20000000;
    public boolean isScreenOn() { return true; }
    public boolean isInteractive() { return true; }
    public boolean isPowerSaveMode() { return false; }
    public boolean isDeviceIdleMode() { return false; }
    public boolean isIgnoringBatteryOptimizations(String pkg) { return true; }
    public void reboot(String reason) {}
    public WakeLock newWakeLock(int levelAndFlags, String tag) {
        return new WakeLock(levelAndFlags, tag == null ? "jet" : tag);
    }
    public static class WakeLock {
        private final int flags; private final String tag;
        private boolean refCounted = true; private int count; private boolean held;
        WakeLock(int flags, String tag) { this.flags = flags; this.tag = tag; }
        public void setReferenceCounted(boolean value) { refCounted = value; }
        public void acquire() { held = true; if (refCounted) count++; }
        public void acquire(long timeout) { acquire(); }
        public void release() {
            if (refCounted) { if (count > 0) count--; if (count == 0) held = false; }
            else held = false;
        }
        public void release(int flags) { release(); }
        public boolean isHeld() { return held; }
        public String toString() { return "WakeLock{" + tag + " held=" + held + "}"; }
    }
}
