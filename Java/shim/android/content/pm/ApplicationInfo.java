package android.content.pm;
public class ApplicationInfo extends PackageItemInfo {
    public static final int FLAG_SYSTEM = 1, FLAG_DEBUGGABLE = 2, FLAG_HAS_CODE = 4, FLAG_PERSISTENT = 8, FLAG_ALLOW_BACKUP = 0x8000, FLAG_LARGE_HEAP = 0x100000, FLAG_UPDATED_SYSTEM_APP = 0x80,
        FLAG_EXTERNAL_STORAGE = 0x40000, FLAG_SUPPORTS_RTL = 0x400000, FLAG_INSTALLED = 0x800000, FLAG_HARDWARE_ACCELERATED = 0x20000000, FLAG_TEST_ONLY = 0x100, FLAG_STOPPED = 0x200000;
    public static final int CATEGORY_UNDEFINED = -1, CATEGORY_GAME = 0, CATEGORY_AUDIO = 1, CATEGORY_VIDEO = 2, CATEGORY_IMAGE = 3, CATEGORY_SOCIAL = 4, CATEGORY_NEWS = 5, CATEGORY_MAPS = 6, CATEGORY_PRODUCTIVITY = 7;
    public String className, processName, taskAffinity, permission, manageSpaceActivityName, backupAgentName, sourceDir, publicSourceDir, dataDir, nativeLibraryDir, deviceProtectedDataDir, appComponentFactory;
    public String[] splitSourceDirs, splitPublicSourceDirs, splitNames, sharedLibraryFiles, resourceDirs;
    public int flags, targetSdkVersion = 28, minSdkVersion = 21, compileSdkVersion = 33, uid = 10123, theme, descriptionRes, uiOptions, requiresSmallestWidthDp, compatibleWidthLimitDp, largestWidthLimitDp, category = CATEGORY_UNDEFINED;
    public boolean enabled = true;
    public ApplicationInfo() {} public ApplicationInfo(ApplicationInfo o) { super(o); className = o.className; processName = o.processName; flags = o.flags; targetSdkVersion = o.targetSdkVersion; minSdkVersion = o.minSdkVersion; sourceDir = o.sourceDir; dataDir = o.dataDir; nativeLibraryDir = o.nativeLibraryDir; theme = o.theme; enabled = o.enabled; uid = o.uid; }
    public CharSequence loadDescription(PackageManager pm) { return null; } public boolean isProfileableByShell() { return false; } public boolean isResourceOverlay() { return false; } public int getRequestedMemoryTagMode() { return 0; }
}
