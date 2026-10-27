package android.os;
public class Build {
    public static final String FINGERPRINT = "google/jet_phone/jet_phone:14/UQ1A.240105.002/11000000:user/release-keys";
    public static final String MODEL = "Pixel 6";
    public static final String MANUFACTURER = "Google";
    public static final String BRAND = "google";
    public static final String DEVICE = "oriole";
    public static final String PRODUCT = "oriole";
    public static final String HARDWARE = "oriole";
    public static final String BOARD = "oriole";
    public static final String ID = "JET";
    public static final String DISPLAY = "JET";
    public static final String TAGS = "release-keys";
    public static final String TYPE = "user";
    public static final String USER = "jet";
    public static final String HOST = "jet";
    public static final long TIME = 0L;
    public static final String UNKNOWN = "unknown";
    /** API 26+: SERIAL is UNKNOWN for apps without READ_PRIVILEGED_PHONE_STATE; getSerial() throws SecurityException. */
    @Deprecated public static final String SERIAL = UNKNOWN;
    public static final String BOOTLOADER = UNKNOWN;
    @Deprecated public static final String RADIO = UNKNOWN;
    public static final String[] SUPPORTED_64_BIT_ABIS = { "aarch64".equals(System.getProperty("os.arch")) ? "arm64-v8a" : "x86_64" };   // the CPU the natives run on (iOS host: arm64-v8a)
    public static final String[] SUPPORTED_32_BIT_ABIS = {};
    public static final String[] SUPPORTED_ABIS = SUPPORTED_64_BIT_ABIS;
    @Deprecated public static final String CPU_ABI = SUPPORTED_ABIS[0];
    @Deprecated public static final String CPU_ABI2 = "";
    public static final String SOC_MANUFACTURER = UNKNOWN, SOC_MODEL = UNKNOWN, SKU = UNKNOWN, ODM_SKU = UNKNOWN;
    public static String getSerial() { throw new SecurityException("getSerial: The user " + 10000 + " does not meet the requirements to access device identifiers."); }
    public static String getRadioVersion() { return null; }
    public static class VERSION {
        // JET: runtime value (not a compile-time constant), default API 34 / Android 14; per-app override via -Djet.sdk
        public static final int SDK_INT = Integer.getInteger("jet.sdk", 34);
        public static final String SDK = String.valueOf(SDK_INT);
        public static final String RELEASE = jetRelease(SDK_INT);
        public static final String RELEASE_OR_CODENAME = RELEASE;
        public static final String RELEASE_OR_PREVIEW_DISPLAY = RELEASE;
        public static final int MEDIA_PERFORMANCE_CLASS = 0;
        static String jetRelease(int api) { switch (api) { case 28: return "9"; case 29: return "10"; case 30: return "11"; case 31: return "12"; case 32: return "12L"; case 33: return "13"; case 34: return "14"; case 35: return "15"; default: return api < 28 ? "8.1" : "14"; } }
        public static final String CODENAME = "REL";
        public static final String INCREMENTAL = "0";
        public static final String SECURITY_PATCH = "2024-10-05";
        public static final int PREVIEW_SDK_INT = 0;
        public static final String BASE_OS = "";
    }
    public static class VERSION_CODES {
        public static final int BASE = 1;
        public static final int LOLLIPOP = 21;
        public static final int M = 23;
        public static final int N = 24;
        public static final int O = 26;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31; public static final int S_V2 = 32;
        public static final int TIRAMISU = 33; public static final int UPSIDE_DOWN_CAKE = 34; public static final int VANILLA_ICE_CREAM = 35; public static final int CUR_DEVELOPMENT = 10000;
    }
}
