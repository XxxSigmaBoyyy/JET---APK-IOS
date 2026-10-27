package android.app;
import java.util.*;
/** Headless AlarmManager: alarms are recorded (keyed by PendingIntent / listener tag) and never fire. */
public class AlarmManager {
    public static final int RTC_WAKEUP = 0;
    public static final String ACTION_NEXT_ALARM_CLOCK_CHANGED = "android.app.action.NEXT_ALARM_CLOCK_CHANGED";
    public static final String ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";
    public static final int ELAPSED_REALTIME = 3;
    public static final int ELAPSED_REALTIME_WAKEUP = 2;
    public static final long INTERVAL_DAY = 86400000l;
    public static final long INTERVAL_FIFTEEN_MINUTES = 900000l;
    public static final long INTERVAL_HALF_DAY = 43200000l;
    public static final long INTERVAL_HALF_HOUR = 1800000l;
    public static final long INTERVAL_HOUR = 3600000l;
    public static final int RTC = 1;
    public interface OnAlarmListener { void onAlarm(); }
    public static final class AlarmClockInfo implements android.os.Parcelable {
        private final long mTrigger; private final PendingIntent mShow;
        public AlarmClockInfo(long triggerTime, PendingIntent showIntent) { mTrigger = triggerTime; mShow = showIntent; }
        public long getTriggerTime() { return mTrigger; } public PendingIntent getShowIntent() { return mShow; }
        public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
        public static final android.os.Parcelable.Creator<AlarmClockInfo> CREATOR = null;
    }
    private final Map<Object, long[]> mAlarms = new LinkedHashMap<>(); private AlarmClockInfo mClock;
    AlarmManager() {}
    private void rec(Object k, int type, long at, long interval) { if (k == null) return; synchronized (mAlarms) { mAlarms.put(k, new long[]{type, at, interval}); } System.err.println("[jet-alarm] set type=" + type + " at=" + at + " interval=" + interval + " (recorded, never fires)"); }
    public void set(int type, long at, PendingIntent op) { rec(op, type, at, 0); }
    public void set(int type, long at, String tag, OnAlarmListener l, android.os.Handler h) { rec(l, type, at, 0); }
    public void setRepeating(int type, long at, long interval, PendingIntent op) { rec(op, type, at, interval); }
    public void setInexactRepeating(int type, long at, long interval, PendingIntent op) { rec(op, type, at, interval); }
    public void setWindow(int type, long start, long len, PendingIntent op) { rec(op, type, start, 0); }
    public void setWindow(int type, long start, long len, String tag, OnAlarmListener l, android.os.Handler h) { rec(l, type, start, 0); }
    public void setWindow(int type, long start, long len, String tag, java.util.concurrent.Executor e, OnAlarmListener l) { rec(l, type, start, 0); }
    public void setExact(int type, long at, PendingIntent op) { rec(op, type, at, 0); }
    public void setExact(int type, long at, String tag, OnAlarmListener l, android.os.Handler h) { rec(l, type, at, 0); }
    public void setAndAllowWhileIdle(int type, long at, PendingIntent op) { rec(op, type, at, 0); }
    public void setExactAndAllowWhileIdle(int type, long at, PendingIntent op) { rec(op, type, at, 0); }
    public void setAlarmClock(AlarmClockInfo info, PendingIntent op) { mClock = info; rec(op, RTC_WAKEUP, info.getTriggerTime(), 0); }
    public AlarmClockInfo getNextAlarmClock() { return mClock; }
    public void cancel(PendingIntent op) { synchronized (mAlarms) { mAlarms.remove(op); } }
    public void cancel(OnAlarmListener l) { synchronized (mAlarms) { mAlarms.remove(l); } }
    public void cancelAll() { synchronized (mAlarms) { mAlarms.clear(); } }
    public boolean canScheduleExactAlarms() { return true; }
    public void setTime(long millis) {} public void setTimeZone(String tz) {}
    public int jetAlarmCount() { synchronized (mAlarms) { return mAlarms.size(); } }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
