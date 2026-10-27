package android.app.job;
import android.content.ComponentName;
import android.net.Uri;
public class JobInfo {
    public static final int NETWORK_TYPE_NONE = 0, NETWORK_TYPE_ANY = 1, NETWORK_TYPE_UNMETERED = 2, NETWORK_TYPE_NOT_ROAMING = 3, NETWORK_TYPE_CELLULAR = 4;
    public static final int BACKOFF_POLICY_LINEAR = 0, BACKOFF_POLICY_EXPONENTIAL = 1, PRIORITY_MIN = 100, PRIORITY_LOW = 200, PRIORITY_DEFAULT = 300, PRIORITY_HIGH = 400, PRIORITY_MAX = 500;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000L, MAX_BACKOFF_DELAY_MILLIS = 18000000L;
    public static final class TriggerContentUri {
        public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1;
        private final Uri uri; private final int flags;
        public TriggerContentUri(Uri uri, int flags) { this.uri = uri; this.flags = flags; }
        public Uri getUri() { return uri; } public int getFlags() { return flags; }
    }
    final int id; final ComponentName service; final java.util.List<TriggerContentUri> triggers = new java.util.ArrayList<>(); android.os.PersistableBundle extras = new android.os.PersistableBundle();
    long minLatency, periodic, maxDelay, triggerMaxDelay, triggerUpdateDelay; boolean persisted, charging, idle; int network;
    JobInfo(int id, ComponentName s) { this.id = id; service = s; }
    public int getId() { return id; } public ComponentName getService() { return service; } public android.os.PersistableBundle getExtras() { return extras; }
    public TriggerContentUri[] getTriggerContentUris() { return triggers.isEmpty() ? null : triggers.toArray(new TriggerContentUri[0]); }
    public long getMinLatencyMillis() { return minLatency; } public long getIntervalMillis() { return periodic; } public boolean isPeriodic() { return periodic > 0; } public boolean isPersisted() { return persisted; }
    public long getMaxExecutionDelayMillis() { return maxDelay; } public long getTriggerContentMaxDelay() { return triggerMaxDelay; } public long getTriggerContentUpdateDelay() { return triggerUpdateDelay; }
    public int getNetworkType() { return network; } public boolean isRequireCharging() { return charging; } public boolean isRequireDeviceIdle() { return idle; }
    public static final long getMinPeriodMillis() { return 900000L; } public static final long getMinFlexMillis() { return 300000L; }
    public static final class Builder {
        private final JobInfo j;
        public Builder(int jobId, ComponentName service) { j = new JobInfo(jobId, service); }
        public Builder setExtras(android.os.PersistableBundle e) { j.extras = e; return this; }
        public Builder addTriggerContentUri(TriggerContentUri u) { j.triggers.add(u); return this; }
        public Builder setTriggerContentUpdateDelay(long d) { j.triggerUpdateDelay = d; return this; }
        public Builder setTriggerContentMaxDelay(long d) { j.triggerMaxDelay = d; return this; }
        public Builder setMinimumLatency(long d) { j.minLatency = d; return this; }
        public Builder setOverrideDeadline(long d) { j.maxDelay = d; return this; }
        public Builder setPeriodic(long p) { j.periodic = p; return this; } public Builder setPeriodic(long p, long flex) { j.periodic = p; return this; }
        public Builder setPersisted(boolean p) { j.persisted = p; return this; }
        public Builder setRequiredNetworkType(int t) { j.network = t; return this; } public Builder setRequiresCharging(boolean b) { j.charging = b; return this; }
        public Builder setRequiresDeviceIdle(boolean b) { j.idle = b; return this; } public Builder setRequiresBatteryNotLow(boolean b) { return this; } public Builder setRequiresStorageNotLow(boolean b) { return this; }
        public Builder setBackoffCriteria(long ms, int policy) { return this; } public Builder setImportantWhileForeground(boolean b) { return this; } public Builder setPrefetch(boolean b) { return this; }
        public Builder setExpedited(boolean b) { return this; } public Builder setPriority(int p) { return this; } public Builder setUserInitiated(boolean b) { return this; }
        public JobInfo build() { return j; }
    }
}
