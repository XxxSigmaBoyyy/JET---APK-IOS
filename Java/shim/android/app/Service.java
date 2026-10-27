package android.app;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
public abstract class Service extends android.content.ContextWrapper {
    public static final int START_STICKY = 1, START_NOT_STICKY = 2, START_REDELIVER_INTENT = 3;
    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int STOP_FOREGROUND_REMOVE = 1, STOP_FOREGROUND_DETACH = 2;
    public Service() { super(null); }
    public void onCreate() {}
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    public void onDestroy() {}
    public abstract IBinder onBind(Intent intent);
    public boolean onUnbind(Intent intent) { return false; }
    public void onRebind(Intent intent) {}
    public void stopSelf() {}
    public void stopSelf(int startId) {}
    public boolean stopSelfResult(int startId) { return true; }
    public final void startForeground(int id, Notification notification) {}
    public final void startForeground(int id, Notification notification, int foregroundServiceType) {}
    public final void stopForeground(boolean removeNotification) {}
    public final void stopForeground(int flags) {}
    public final Application getApplication() { return Application.jetCurrent; }
}
