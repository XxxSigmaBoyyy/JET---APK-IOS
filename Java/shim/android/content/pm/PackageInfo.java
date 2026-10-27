package android.content.pm;
public class PackageInfo {
    public String packageName, versionName, sharedUserId;
    public String[] splitNames, requestedPermissions;
    public int versionCode, baseRevisionCode, sharedUserLabel, installLocation;
    public long firstInstallTime = 1735689600000L, lastUpdateTime = 1735689600000L;  // fixed "install" time (deterministic runs)
    public ApplicationInfo applicationInfo;
    public ActivityInfo[] activities, receivers;
    public ServiceInfo[] services;
    public ProviderInfo[] providers;
    public android.content.pm.Signature[] signatures;
    public PackageInfo() {}
    public long getLongVersionCode() { return versionCode & 0xffffffffL; }
    public void setLongVersionCode(long v) { versionCode = (int) v; }
}
