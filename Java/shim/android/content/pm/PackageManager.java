package android.content.pm;
import android.content.ComponentName;
import android.content.Intent;
import java.util.Collections;
import java.util.List;
public abstract class PackageManager {
    public static final int GET_META_DATA = 0x00000080;
    public static final int MATCH_DEFAULT_ONLY = 0x00010000;
    public abstract PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException;
    public android.content.res.XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) { return null; }
    public boolean isInstantApp() { return false; }
    public boolean isInstantApp(String packageName) { return false; }
    public abstract ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException;
    public abstract List<ResolveInfo> queryIntentActivities(Intent intent, int flags);
    public abstract ResolveInfo resolveActivity(Intent intent, int flags);
    public abstract Intent getLaunchIntentForPackage(String packageName);
    public abstract String getInstallerPackageName(String packageName);
    public abstract boolean hasSystemFeature(String name);
    public boolean hasSystemFeature(String name, int version) { return hasSystemFeature(name); }
    public ServiceInfo getServiceInfo(ComponentName className, int flags) throws NameNotFoundException {
        ServiceInfo si = new ServiceInfo();
        si.packageName = className.getPackageName();
        si.name = className.getClassName();
        java.util.Map<String, String> md = jetServiceMeta.get(si.name);
        if (md != null && (flags & GET_META_DATA) != 0) {   // AOSP: metaData only with GET_META_DATA
            si.metaData = new android.os.Bundle();
            for (java.util.Map.Entry<String, String> e : md.entrySet()) {
                String v = e.getValue();
                if (v.matches("@[0-9A-Fa-f]{8}")) si.metaData.putInt(e.getKey(), (int) Long.parseLong(v.substring(1), 16)); else si.metaData.putString(e.getKey(), v);
            }
        }
        return si;
    }
    /** Manifest &lt;service&gt; meta-data by class name (filled by the host from app.properties "service.*.meta.*"). */
    public static final java.util.Map<String, java.util.Map<String, String>> jetServiceMeta = new java.util.LinkedHashMap<>();
    /** Manifest providers (filled by the host from the bundle), keyed by class name. */
    public static final java.util.Map<String, ProviderInfo> jetProviders = new java.util.LinkedHashMap<>();
    public static final int GET_PROVIDERS = 0x00000008, MATCH_DISABLED_COMPONENTS = 0x00000200;
    public static final int COMPONENT_ENABLED_STATE_DEFAULT = 0, COMPONENT_ENABLED_STATE_ENABLED = 1, COMPONENT_ENABLED_STATE_DISABLED = 2,
        COMPONENT_ENABLED_STATE_DISABLED_USER = 3, COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4, DONT_KILL_APP = 1, SYNCHRONOUS = 4;
    private final java.util.Map<String, Integer> mEnabled = new java.util.HashMap<>();
    public int getComponentEnabledSetting(ComponentName c) { Integer v = mEnabled.get(c.flattenToString()); return v == null ? COMPONENT_ENABLED_STATE_DEFAULT : v; }
    public void setComponentEnabledSetting(ComponentName c, int state, int flags) { mEnabled.put(c.flattenToString(), state); }
    public int getApplicationEnabledSetting(String pkg) { return COMPONENT_ENABLED_STATE_DEFAULT; }
    public void setApplicationEnabledSetting(String pkg, int state, int flags) {}
    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
        ProviderInfo pi = jetProviders.get(component.getClassName());
        if (pi == null) throw new NameNotFoundException(component.getClassName());
        return pi;
    }
    public ProviderInfo resolveContentProvider(String authority, int flags) {
        for (ProviderInfo pi : jetProviders.values()) if (pi.authority != null && java.util.Arrays.asList(pi.authority.split(";")).contains(authority)) return pi;
        return null;
    }
    public ActivityInfo getActivityInfo(ComponentName className, int flags) throws NameNotFoundException {
        ActivityInfo ai = new ActivityInfo();
        ai.packageName = className.getPackageName();
        ai.name = className.getClassName();
        return ai;
    }
    // API 30/33 additions
    public InstallSourceInfo getInstallSourceInfo(String pkg) throws NameNotFoundException { getApplicationInfo(pkg, 0); return new InstallSourceInfo(getInstallerPackageName(pkg)); }
    public PackageInfo getPackageInfo(String pkg, PackageInfoFlags f) throws NameNotFoundException { return getPackageInfo(pkg, (int) f.getValue()); }
    public ApplicationInfo getApplicationInfo(String pkg, ApplicationInfoFlags f) throws NameNotFoundException { return getApplicationInfo(pkg, (int) f.getValue()); }
    /** Headless host: no other installed packages export services/receivers/providers for implicit intents. */
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) { return new java.util.ArrayList<>(); }
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) { return new java.util.ArrayList<>(); }
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) { return new java.util.ArrayList<>(); }
    public List<ResolveInfo> queryIntentActivities(Intent i, ResolveInfoFlags f) { return queryIntentActivities(i, (int) f.getValue()); }
    public ResolveInfo resolveActivity(Intent i, ResolveInfoFlags f) { return resolveActivity(i, (int) f.getValue()); }
    public static class Flags { final long v; Flags(long v) { this.v = v; } public long getValue() { return v; } }
    public static final class PackageInfoFlags extends Flags { PackageInfoFlags(long v) { super(v); } public static PackageInfoFlags of(long v) { return new PackageInfoFlags(v); } }
    public static final class ApplicationInfoFlags extends Flags { ApplicationInfoFlags(long v) { super(v); } public static ApplicationInfoFlags of(long v) { return new ApplicationInfoFlags(v); } }
    public static final class ResolveInfoFlags extends Flags { ResolveInfoFlags(long v) { super(v); } public static ResolveInfoFlags of(long v) { return new ResolveInfoFlags(v); } }
    public static final class ComponentInfoFlags extends Flags { ComponentInfoFlags(long v) { super(v); } public static ComponentInfoFlags of(long v) { return new ComponentInfoFlags(v); } }

    // Generic phone profile features (WhatsApp HAM device class uses android.hardware.telephony).
    public static final String FEATURE_TELEPHONY = "android.hardware.telephony";
    public static final String FEATURE_TELEPHONY_GSM = "android.hardware.telephony.gsm";
    public static final String FEATURE_TELEPHONY_CDMA = "android.hardware.telephony.cdma";
    public static final String FEATURE_TOUCHSCREEN = "android.hardware.touchscreen";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH = "android.hardware.touchscreen.multitouch";
    public static final String FEATURE_WIFI = "android.hardware.wifi";
    public static final String FEATURE_LOCATION = "android.hardware.location";
    public static final String FEATURE_LOCATION_GPS = "android.hardware.location.gps";
    public static final String FEATURE_CAMERA = "android.hardware.camera";
    public static final String FEATURE_CAMERA_ANY = "android.hardware.camera.any";
    public static final String FEATURE_MICROPHONE = "android.hardware.microphone";
    public static final String FEATURE_SCREEN_PORTRAIT = "android.hardware.screen.portrait";
    public static final String FEATURE_SCREEN_LANDSCAPE = "android.hardware.screen.landscape";
    public static final String FEATURE_FAKETOUCH = "android.hardware.faketouch";
    public static final String FEATURE_AUDIO_OUTPUT = "android.hardware.audio.output";
    public static final String FEATURE_SIP = "android.software.sip";
    public static final String FEATURE_SIP_VOIP = "android.software.sip.voip";
    private static final java.util.Set<String> JET_PHONE_FEATURES = new java.util.HashSet<>(java.util.Arrays.asList(
        FEATURE_TELEPHONY, FEATURE_TELEPHONY_GSM, FEATURE_TOUCHSCREEN, FEATURE_TOUCHSCREEN_MULTITOUCH,
        FEATURE_WIFI, FEATURE_LOCATION, FEATURE_LOCATION_GPS, FEATURE_CAMERA, FEATURE_CAMERA_ANY,
        FEATURE_MICROPHONE, FEATURE_SCREEN_PORTRAIT, FEATURE_SCREEN_LANDSCAPE, FEATURE_FAKETOUCH,
        FEATURE_AUDIO_OUTPUT, FEATURE_SIP, FEATURE_SIP_VOIP,
        "android.software.input_methods", "android.software.home_screen", "android.software.webview",
        "android.hardware.bluetooth", "android.hardware.bluetooth_le", "android.hardware.usb.host",
        "android.hardware.sensor.accelerometer", "android.hardware.sensor.compass", "android.hardware.sensor.gyroscope"
    ));
    /** True for a consistent generic phone feature set (not a tablet/desktop/companion-only profile). */
    public static boolean jetHasSystemFeature(String name) {
        if (name == null) return false;
        if (JET_PHONE_FEATURES.contains(name)) return true;
        // versioned telephony.* / touchscreen.* prefixes common in manifests
        if (name.startsWith("android.hardware.telephony")) return true;
        if (name.startsWith("android.hardware.touchscreen")) return true;
        if (name.startsWith("android.hardware.camera")) return true;
        return false;
    }

    public static class NameNotFoundException extends android.util.AndroidException {
        public NameNotFoundException() {}
        public NameNotFoundException(String name) { super(name); }
    }
}
