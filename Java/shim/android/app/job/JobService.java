package android.app.job;
public abstract class JobService extends android.app.Service {
    public static final String PERMISSION_BIND = "android.permission.BIND_JOB_SERVICE";
    public abstract boolean onStartJob(JobParameters params);
    public abstract boolean onStopJob(JobParameters params);
    public final void jobFinished(JobParameters params, boolean wantsReschedule) {}
    public final android.os.IBinder onBind(android.content.Intent intent) { return null; }
}
