package android.app.job;
public class JobParameters {
    private final JobInfo info;
    public JobParameters(JobInfo i) { info = i; }
    public int getJobId() { return info.getId(); } public android.os.PersistableBundle getExtras() { return info.getExtras(); }
    public android.net.Uri[] getTriggeredContentUris() { return null; } public String[] getTriggeredContentAuthorities() { return null; }
    public boolean isOverrideDeadlineExpired() { return false; } public int getStopReason() { return 0; }
}
