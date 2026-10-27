package jet.runtime;
import java.util.*;
/** Context.getSystemService registry for the headless harness. */
public final class Services {
    private static final Map<String, String> SDK = new HashMap<>();
    static {
        String[][] m = {{"notification", "android.app.NotificationManager"}, {"alarm", "android.app.AlarmManager"}, {"power", "android.os.PowerManager"},
            {"connectivity", "android.net.ConnectivityManager"}, {"input_method", "android.view.inputmethod.InputMethodManager"}, {"clipboard", "android.content.ClipboardManager"},
            {"uimode", "android.app.UiModeManager"}, {"keyguard", "android.app.KeyguardManager"}, {"location", "android.location.LocationManager"}, {"wifi", "android.net.wifi.WifiManager"},
            {"storage", "android.os.storage.StorageManager"}, {"display", "android.hardware.display.DisplayManager"}, {"accessibility", "android.view.accessibility.AccessibilityManager"},
            {"jobscheduler", "android.app.job.JobScheduler"}, {"download", "android.app.DownloadManager"}, {"shortcut", "android.content.pm.ShortcutManager"},
            {"phone", "android.telephony.TelephonyManager"}, {"batterymanager", "android.os.BatteryManager"}, {"user", "android.os.UserManager"}, {"locale", "android.app.LocaleManager"},
            {"captioning", "android.view.accessibility.CaptioningManager"}, {"media_session", "android.media.session.MediaSessionManager"}, {"autofill", "android.view.autofill.AutofillManager"},
            {"textclassification", "android.view.textclassifier.TextClassificationManager"}, {"usb", "android.hardware.usb.UsbManager"}, {"camera", "android.hardware.camera2.CameraManager"},
            {"vibrator_manager", "android.os.VibratorManager"}, {"window", null}, {"layout_inflater", null}, {"sensor", null}, {"vibrator", null}, {"audio", null}, {"activity", null}};
        for (String[] e : m) SDK.put(e[0], e[1]);
    }
    private static final Map<String, Object> CACHE = new HashMap<>();
    public static synchronized Object get(android.content.Context c, String name) {
        switch (name) {
            case "layout_inflater": return android.view.LayoutInflater.from(c);
            case "window": return CACHE.computeIfAbsent(name, k -> new JetWindowManager());
            case "sensor": return CACHE.computeIfAbsent(name, k -> new android.hardware.SensorManager());
            case "vibrator": return CACHE.computeIfAbsent(name, k -> new android.os.Vibrator());
            case "audio": return CACHE.computeIfAbsent(name, k -> new android.media.AudioManager());
            case "activity": return CACHE.computeIfAbsent(name, k -> new android.app.ActivityManager());
            case "display": return CACHE.computeIfAbsent(name, k -> new android.hardware.display.DisplayManager());
            case "jobscheduler": return CACHE.computeIfAbsent(name, k -> new android.app.job.JobScheduler.Jet());
            case "shortcut": return CACHE.computeIfAbsent(name, k -> new android.content.pm.ShortcutManager());
            case "input_method": return CACHE.computeIfAbsent(name, k -> jet.runtime.Services.imm());
        }
        if (CACHE.containsKey(name)) return CACHE.get(name);
        String cn = SDK.get(name); Object o = null;
        if (cn != null) o = instantiate(cn);
        System.err.println("[SERVICE] " + name + " -> " + (o == null ? "null" : o.getClass().getName() + " (SDK stub)"));
        CACHE.put(name, o); return o;
    }
    static Object imm() { try { java.lang.reflect.Constructor<?> c = android.view.inputmethod.InputMethodManager.class.getDeclaredConstructor(); c.setAccessible(true); return c.newInstance(); } catch (Exception e) { throw new RuntimeException(e); } }
    public static String nameFor(Class<?> k) {
        switch (k.getName()) {
            case "android.view.LayoutInflater": return "layout_inflater"; case "android.view.WindowManager": return "window"; case "android.hardware.SensorManager": return "sensor";
            case "android.os.Vibrator": return "vibrator"; case "android.media.AudioManager": return "audio"; case "android.app.ActivityManager": return "activity"; case "android.hardware.display.DisplayManager": return "display"; case "android.app.job.JobScheduler": return "jobscheduler"; case "android.content.pm.ShortcutManager": return "shortcut";
        }
        for (Map.Entry<String, String> e : SDK.entrySet()) if (k.getName().equals(e.getValue())) return e.getKey();
        return null;
    }
    private static Object instantiate(String cn) {
        try {
            Class<?> k = Class.forName(cn, true, Thread.currentThread().getContextClassLoader());
            try { java.lang.reflect.Constructor<?> ct = k.getDeclaredConstructor(); ct.setAccessible(true); return ct.newInstance(); }
            catch (NoSuchMethodException e) {
                java.lang.reflect.Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe"); f.setAccessible(true);
                Object u = f.get(null); return u.getClass().getMethod("allocateInstance", Class.class).invoke(u, k);
            }
        } catch (Throwable t) { System.err.println("[SERVICE] cannot instantiate " + cn + ": " + t); return null; }
    }
    static final class JetWindowManager implements android.view.WindowManager {
        public void addView(android.view.View v, android.view.ViewGroup.LayoutParams p) {} public void updateViewLayout(android.view.View v, android.view.ViewGroup.LayoutParams p) {} public void removeView(android.view.View v) {}
        public android.view.Display getDefaultDisplay() { return android.view.Display.jetDefault(); }
    }
}
