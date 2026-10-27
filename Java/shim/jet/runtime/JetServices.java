package jet.runtime;
import android.app.Service;
import android.content.*;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Host service manager: start/bind instantiates the Service class, calls
 * onCreate/onStartCommand/onBind, and delivers {@link ServiceConnection#onServiceConnected}
 * on the main looper. Generic — required for Signal PassphraseCreateActivity → KeyCachingService.
 */
public final class JetServices {
    private JetServices() {}
    private static final Map<String, Service> RUNNING = new ConcurrentHashMap<>();
    private static final Map<ServiceConnection, String> BINDS = new ConcurrentHashMap<>();

    public static ComponentName start(Context ctx, Intent intent) {
        if (intent == null) return null;
        ComponentName cn = intent.getComponent();
        if (cn == null) { System.out.println("[Context] startService (no component) " + intent); return null; }
        Service s = ensure(ctx, cn);
        if (s != null) {
            try { s.onStartCommand(intent, 0, 1); }
            catch (Throwable t) { System.err.println("[jet-service] onStartCommand " + cn + ": " + t); }
        }
        return cn;
    }

    public static boolean bind(Context ctx, Intent intent, ServiceConnection conn, int flags) {
        if (intent == null || conn == null) return false;
        ComponentName cn = intent.getComponent();
        if (cn == null) { System.out.println("[Context] bindService (no component) " + intent); return false; }
        Service s = ensure(ctx, cn);
        if (s == null) return false;
        IBinder binder;
        try { binder = s.onBind(intent); }
        catch (Throwable t) { System.err.println("[jet-service] onBind " + cn + ": " + t); return false; }
        BINDS.put(conn, cn.getClassName());
        ComponentName finalCn = cn;
        IBinder finalBinder = binder;
        Handler h = mainHandler();
        h.post(() -> {
            try { conn.onServiceConnected(finalCn, finalBinder); }
            catch (Throwable t) { System.err.println("[jet-service] onServiceConnected " + finalCn + ": " + t); t.printStackTrace(System.err); }
        });
        return true;
    }

    public static void unbind(ServiceConnection conn) {
        if (conn == null) return;
        BINDS.remove(conn);
    }

    public static boolean stop(Intent intent) {
        if (intent == null || intent.getComponent() == null) return false;
        Service s = RUNNING.remove(intent.getComponent().getClassName());
        if (s == null) return false;
        try { s.onDestroy(); } catch (Throwable ignored) {}
        return true;
    }

    private static Service ensure(Context ctx, ComponentName cn) {
        String name = cn.getClassName();
        Service existing = RUNNING.get(name);
        if (existing != null) return existing;
        synchronized (RUNNING) {
            existing = RUNNING.get(name);
            if (existing != null) return existing;
            try {
                ClassLoader cl = ctx.getClassLoader() != null ? ctx.getClassLoader() : JetServices.class.getClassLoader();
                Class<?> cls = Class.forName(name, true, cl);
                Service s = (Service) cls.getDeclaredConstructor().newInstance();
                s.attachBaseContext(ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx);
                try { s.onCreate(); } catch (Throwable t) { System.err.println("[jet-service] onCreate " + name + ": " + t); t.printStackTrace(System.err); }
                RUNNING.put(name, s);
                System.out.println("[jet-service] started " + name);
                return s;
            } catch (Throwable t) {
                System.err.println("[jet-service] instantiate " + name + ": " + t);
                t.printStackTrace(System.err);
                return null;
            }
        }
    }

    private static Handler mainHandler() {
        android.os.Looper main = android.os.Looper.getMainLooper();
        return main != null ? new android.os.Handler(main) : new android.os.Handler();
    }
}
