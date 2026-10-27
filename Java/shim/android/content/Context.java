package android.content;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import java.util.Collections;
import android.content.res.AssetManager;
import android.content.res.Resources;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
public class Context {
    public static final int MODE_PRIVATE = 0;
    protected Resources mResources;
    protected AssetManager mAssets;
    protected File mDataDir;
    private final Map<String, SharedPreferences> prefs = new HashMap<>();
    public Context() {}
    public Context(Resources r) { mResources = r; }
    public Resources getResources() { return mResources; }
    public AssetManager getAssets() {
        if (mAssets == null) { Resources r = getResources(); mAssets = r != null ? r.getAssets() : new AssetManager(null); }
        return mAssets;
    }
    public final android.graphics.drawable.Drawable getDrawable(int id) { return getResources() == null ? null : getResources().getDrawable(id); }
    public final int getColor(int id) { return getResources() == null ? 0 : getResources().getColor(id); }
    public final String getString(int id) { return getResources().getString(id); }
    public final CharSequence getText(int id) { return getResources().getText(id); }
    public String getPackageName() { return getResources().getPackageName(); }

    public TypedArray obtainStyledAttributes(int[] attrs) {
        return getTheme() == null ? new TypedArray(getResources(), attrs) : getTheme().obtainStyledAttributes(attrs);
    }
    public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs) {
        return getTheme() == null ? new TypedArray(getResources(), attrs) : getTheme().obtainStyledAttributes(set, attrs, 0, 0);
    }
    public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
        if (android.content.res.TypedArray.DBG) { Context c = this; StringBuilder sb = new StringBuilder(); for (int g = 0; c != null && g < 6; g++) { sb.append(c.getClass().getName()).append('@').append(System.identityHashCode(c.getTheme())).append(" > "); c = c instanceof ContextWrapper ? ((ContextWrapper) c).getBaseContext() : null; } System.err.println("[TA] ctx " + sb); }
        return getTheme() == null ? new TypedArray(getResources(), attrs) : getTheme().obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
    }
    public TypedArray obtainStyledAttributes(int resid, int[] attrs) {
        return getTheme() == null ? new TypedArray(getResources(), attrs) : getTheme().obtainStyledAttributes(resid, attrs);
    }


    private android.content.res.Resources.Theme mTheme;
    public android.content.res.Resources.Theme getTheme() {
        if (mTheme == null && getResources() != null) mTheme = getResources().newTheme();
        return mTheme;
    }
    public void setTheme(int resid) { if (getTheme() != null) getTheme().applyStyle(resid, true); }
    public File getExternalFilesDir(String type) {
        File base = new File(getFilesDir().getParentFile(), "external");
        File dir = type == null ? base : new File(base, type);
        dir.mkdirs();
        return dir;
    }
    public File[] getExternalFilesDirs(String type) { return new File[]{ getExternalFilesDir(type) }; }

    public Context createConfigurationContext(Configuration overrideConfiguration) {
        // Prototype: return this; resources already hold a Configuration
        if (overrideConfiguration != null && mResources != null) {
            // AOSP merges the override onto the base configuration: undefined (0/null) override fields keep the base value.
            try { Configuration merged = new Configuration(mResources.getConfiguration()); merged.updateFrom(overrideConfiguration);
                  mResources.updateConfiguration(merged, mResources.getDisplayMetrics()); } catch (Throwable ignore) {}
        }
        return this;
    }
    public Context createContext(android.content.ContextParams params) { return this; }
    public Context createDeviceProtectedStorageContext() { return this; }


    private PackageManager mPm;
    public PackageManager getPackageManager() {
        if (mPm == null) mPm = new PackageManager() {
            public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
                PackageInfo pi = new PackageInfo();
                pi.packageName = packageName; pi.versionCode = 1; pi.versionName = "1.0";
                pi.applicationInfo = getApplicationInfo(packageName, 0);
                return pi;
            }
            public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
                // own package: the full ApplicationInfo (sourceDir, dataDir, nativeLibraryDir...); others aren't installed
                if (packageName != null && packageName.equals(Context.this.getPackageName())) return new ApplicationInfo(Context.this.getApplicationInfo());
                throw new NameNotFoundException(packageName);
            }
            public java.util.List<android.content.pm.ResolveInfo> queryIntentActivities(Intent intent, int flags) {
                return Collections.emptyList();
            }
            public android.content.pm.ResolveInfo resolveActivity(Intent intent, int flags) { return null; }
            public android.content.res.XmlResourceParser getXml(String pkg, int resid, ApplicationInfo ai) { if (pkg != null && !pkg.equals(Context.this.getPackageName())) return null; try { return Context.this.getResources().getXml(resid); } catch (RuntimeException e) { return null; } }
            public Intent getLaunchIntentForPackage(String packageName) { return null; }
            public String getInstallerPackageName(String packageName) { return null; }
            public boolean hasSystemFeature(String name) { return android.content.pm.PackageManager.jetHasSystemFeature(name); }
            public boolean hasSystemFeature(String name, int version) { return hasSystemFeature(name); }
        };
        return mPm;
    }

    public File getFilesDir() { File f = new File(dataDir(), "files"); f.mkdirs(); return f; }
    public File getCacheDir() { File f = new File(dataDir(), "cache"); f.mkdirs(); return f; }
    public File getDir(String name, int mode) { File f = new File(dataDir(), name); f.mkdirs(); return f; }
    public File getDatabasePath(String name) { File f = new File(dataDir(), "databases"); f.mkdirs(); return new File(f, name); }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        synchronized (prefs) {
            SharedPreferences p = prefs.get(name);
            if (p == null) {
                p = new SharedPreferencesImpl(new File(dataDir(), "shared_prefs/" + name + ".xml"));
                prefs.put(name, p);
            }
            return p;
        }
    }
    public Object getSystemService(String name) { return name == null ? null : jet.runtime.Services.get(this, name); }
    @SuppressWarnings("unchecked") public final <T> T getSystemService(Class<T> k) { String n = getSystemServiceName(k); return n == null ? null : (T) getSystemService(n); }
    public String getSystemServiceName(Class<?> k) { return jet.runtime.Services.nameFor(k); }
    public static final String SENSOR_SERVICE = "sensor", VIBRATOR_SERVICE = "vibrator", AUDIO_SERVICE = "audio", NOTIFICATION_SERVICE = "notification", ALARM_SERVICE = "alarm",
        POWER_SERVICE = "power", CONNECTIVITY_SERVICE = "connectivity", INPUT_METHOD_SERVICE = "input_method", CLIPBOARD_SERVICE = "clipboard", UI_MODE_SERVICE = "uimode",
        KEYGUARD_SERVICE = "keyguard", LOCATION_SERVICE = "location", WIFI_SERVICE = "wifi", STORAGE_SERVICE = "storage", DISPLAY_SERVICE = "display", ACCESSIBILITY_SERVICE = "accessibility",
        JOB_SCHEDULER_SERVICE = "jobscheduler", DOWNLOAD_SERVICE = "download", SHORTCUT_SERVICE = "shortcut", TELEPHONY_SERVICE = "phone", BATTERY_SERVICE = "batterymanager",
        USER_SERVICE = "user", CAPTIONING_SERVICE = "captioning", MEDIA_SESSION_SERVICE = "media_session", USB_SERVICE = "usb", CAMERA_SERVICE = "camera", VIBRATOR_MANAGER_SERVICE = "vibrator_manager";
    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final String WINDOW_SERVICE = "window";
    public static final String ACTIVITY_SERVICE = "activity";
    public Context getApplicationContext() { android.app.Application a = android.app.Application.jetCurrent; return a != null ? a : this; }
    private static Intent sJetStarted;
    public void startActivity(Intent intent) { System.out.println("[Context] startActivity " + intent.getComponent()); if (intent != null && intent.getComponent() != null) sJetStarted = intent; }
    public void startActivity(Intent intent, android.os.Bundle options) { startActivity(intent); }
    public static Intent jetTakeStartedActivity() { Intent i = sJetStarted; sJetStarted = null; return i; }
    public void sendBroadcast(Intent intent) {}
    public ContentResolver getContentResolver() { return new ContentResolver(); }
    public ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : getClass().getClassLoader();
    }
    public int checkSelfPermission(String p) { return 0; /* PERMISSION_GRANTED */ }
    public final File dataDir() {
        if (mDataDir == null) {
            mDataDir = new File(System.getProperty("jet.data.dir", System.getProperty("java.io.tmpdir") + "/jet-data"), getPackageName());
            mDataDir.mkdirs();
        }
        return mDataDir;
    }

    // ---- Context API batch (pass 3) ----
    public static final int MODE_APPEND = 0x8000, MODE_MULTI_PROCESS = 4, MODE_ENABLE_WRITE_AHEAD_LOGGING = 8, MODE_NO_LOCALIZED_COLLATORS = 0x10;
    public static final int BIND_AUTO_CREATE = 1, BIND_DEBUG_UNBIND = 2, BIND_NOT_FOREGROUND = 4, BIND_IMPORTANT = 0x40, BIND_ABOVE_CLIENT = 8, CONTEXT_INCLUDE_CODE = 1, CONTEXT_IGNORE_SECURITY = 2, CONTEXT_RESTRICTED = 4;
    public static final int RECEIVER_EXPORTED = 2, RECEIVER_NOT_EXPORTED = 4, RECEIVER_VISIBLE_TO_INSTANT_APPS = 1;
    public int checkPermission(String p, int pid, int uid) { return 0; } public int checkCallingPermission(String p) { return 0; } public int checkCallingOrSelfPermission(String p) { return 0; }
    public int checkUriPermission(android.net.Uri u, int pid, int uid, int mode) { return 0; } public void enforceCallingOrSelfPermission(String p, String m) {} public void enforcePermission(String p, int pid, int uid, String m) {}
    public void grantUriPermission(String pkg, android.net.Uri u, int mode) {} public void revokeUriPermission(android.net.Uri u, int mode) {}
    public final String getString(int id, Object... args) { return String.format(getString(id), args); }
    public final android.content.res.ColorStateList getColorStateList(int id) { android.content.res.Resources r = getResources(); return r == null ? null : r.getColorStateList(id, getTheme()); }
    public android.os.Looper getMainLooper() { return android.os.Looper.getMainLooper(); }
    public java.util.concurrent.Executor getMainExecutor() { final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper()); return h::post; }
    public String getOpPackageName() { return getPackageName(); } public String getAttributionTag() { return null; } public String getPackageResourcePath() { return getPackageCodePath(); } public String getPackageCodePath() { String p = System.getProperty("jet.apkPath"); return p != null ? p : new File(dataDir(), "base.apk").getPath(); }
    public AttributionSource getAttributionSource() { return new AttributionSource.Builder(android.os.Process.myUid()).setPid(android.os.Process.myPid()).setPackageName(getPackageName()).setAttributionTag(getAttributionTag()).build(); }
    public File getNoBackupFilesDir() { File f = new File(dataDir(), "no_backup"); f.mkdirs(); return f; } public File getCodeCacheDir() { File f = new File(dataDir(), "code_cache"); f.mkdirs(); return f; }
    public File getExternalCacheDir() { File f = new File(new File(getFilesDir().getParentFile(), "external"), "cache"); f.mkdirs(); return f; } public File[] getExternalCacheDirs() { return new File[]{getExternalCacheDir()}; }
    public File getObbDir() { File f = new File(new File(getFilesDir().getParentFile(), "external"), "obb"); f.mkdirs(); return f; } public File[] getObbDirs() { return new File[]{getObbDir()}; }
    public File[] getExternalMediaDirs() { return new File[]{getExternalFilesDir("media")}; } public File getDataDir() { return dataDir(); }
    public File getFileStreamPath(String name) { return new File(getFilesDir(), name); }
    public java.io.FileInputStream openFileInput(String name) throws java.io.FileNotFoundException { return new java.io.FileInputStream(getFileStreamPath(name)); }
    public java.io.FileOutputStream openFileOutput(String name, int mode) throws java.io.FileNotFoundException { return new java.io.FileOutputStream(getFileStreamPath(name), (mode & MODE_APPEND) != 0); }
    public boolean deleteFile(String name) { return getFileStreamPath(name).delete(); } public String[] fileList() { String[] l = getFilesDir().list(); return l == null ? new String[0] : l; }
    public boolean deleteDatabase(String name) { return getDatabasePath(name).delete(); } public String[] databaseList() { String[] l = getDatabasePath("x").getParentFile().list(); return l == null ? new String[0] : l; }
    public boolean deleteSharedPreferences(String name) { return true; } public boolean moveSharedPreferencesFrom(Context src, String name) { return true; } public boolean moveDatabaseFrom(Context src, String name) { return true; }
    public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name, int mode, android.database.sqlite.SQLiteDatabase.CursorFactory f) { return android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), f); }
    public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name, int mode, android.database.sqlite.SQLiteDatabase.CursorFactory f, android.database.DatabaseErrorHandler h) { return openOrCreateDatabase(name, mode, f); }
    private final java.util.ArrayList<Object[]> mJetReceivers = new java.util.ArrayList<>();
    public Intent registerReceiver(BroadcastReceiver r, IntentFilter f) { if (r != null) mJetReceivers.add(new Object[]{r, f}); return null; }
    public Intent registerReceiver(BroadcastReceiver r, IntentFilter f, int flags) { return registerReceiver(r, f); }
    public Intent registerReceiver(BroadcastReceiver r, IntentFilter f, String perm, android.os.Handler h) { return registerReceiver(r, f); }
    public Intent registerReceiver(BroadcastReceiver r, IntentFilter f, String perm, android.os.Handler h, int flags) { return registerReceiver(r, f); }
    public void unregisterReceiver(BroadcastReceiver r) { mJetReceivers.removeIf(e -> e[0] == r); }
    public void sendBroadcast(Intent i, String perm) { sendBroadcast(i); } public void sendOrderedBroadcast(Intent i, String perm) { sendBroadcast(i); } public void sendStickyBroadcast(Intent i) { sendBroadcast(i); }
    public ComponentName startService(Intent i) { return jet.runtime.JetServices.start(this, i); }
    public ComponentName startForegroundService(Intent i) { return startService(i); }
    public boolean stopService(Intent i) { return jet.runtime.JetServices.stop(i); }
    public boolean bindService(Intent i, ServiceConnection c, int flags) { return jet.runtime.JetServices.bind(this, i, c, flags); }
    public void unbindService(ServiceConnection c) { jet.runtime.JetServices.unbind(c); }
    public void startActivities(Intent[] is) { if (is != null && is.length > 0) startActivity(is[is.length - 1]); } public void startActivities(Intent[] is, android.os.Bundle o) { startActivities(is); }
    public void startIntentSender(IntentSender s, Intent fill, int m, int v, int e) {}
    public boolean isRestricted() { return false; } public boolean isDeviceProtectedStorage() { return false; } public boolean isUiContext() { return true; }
    public Context createPackageContext(String pkg, int flags) { return this; } public Context createAttributionContext(String tag) { return this; } public Context createWindowContext(int type, android.os.Bundle o) { return this; }
    public Context createDisplayContext(android.view.Display d) { return this; }
    public android.view.Display getDisplay() { return android.view.Display.jetDefault(); }
    public void registerComponentCallbacks(ComponentCallbacks c) {} public void unregisterComponentCallbacks(ComponentCallbacks c) {}
    public int getWallpaperDesiredMinimumWidth() { return 0; } public int getWallpaperDesiredMinimumHeight() { return 0; }
    private android.content.pm.ApplicationInfo mJetAppInfo;
    public android.content.pm.ApplicationInfo getApplicationInfo() {
        if (mJetAppInfo == null) { android.content.pm.ApplicationInfo ai = new android.content.pm.ApplicationInfo();
            ai.packageName = getPackageName(); ai.processName = ai.packageName; ai.targetSdkVersion = Integer.getInteger("jet.targetSdk", 33); ai.minSdkVersion = Integer.getInteger("jet.minSdk", 21);
            ai.dataDir = dataDir().getPath(); ai.sourceDir = ai.publicSourceDir = getPackageCodePath(); ai.nativeLibraryDir = System.getProperty("jet.nativeLibraryDir", new File(dataDir(), "lib").getPath());
            ai.flags = 0x00000004 | 0x00000080 /* HAS_CODE | ALLOW_BACKUP-ish */; ai.enabled = true; jetFillMeta(ai.metaData); mJetAppInfo = ai; }
        return mJetAppInfo; }
    /** Manifest <application><meta-data> entries (set by the host from app.properties "appmeta.*"). Typed like PackageParser: boolean/int/float literals, @res ids as int, else String. */
    public static final java.util.Map<String, String> jetAppMeta = new java.util.LinkedHashMap<>();
    static void jetFillMeta(android.os.Bundle b) {
        for (java.util.Map.Entry<String, String> e : jetAppMeta.entrySet()) {
            String k = e.getKey(), v = e.getValue();
            if (v.matches("@[0-9A-Fa-f]{8}")) b.putInt(k, (int) Long.parseLong(v.substring(1), 16));
            else if (v.equals("true") || v.equals("false")) b.putBoolean(k, Boolean.parseBoolean(v));
            else if (v.matches("-?\\d{1,9}")) b.putInt(k, Integer.parseInt(v));
            else if (v.matches("-?\\d*\\.\\d+")) b.putFloat(k, Float.parseFloat(v));
            else b.putString(k, v);
        }
    }
}
