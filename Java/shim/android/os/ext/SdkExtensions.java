package android.os.ext;
/** Extension versions shipped with a stock image at the emulated API level (jet.sdk); 0 for SDKs newer than the device. */
public class SdkExtensions {
    public static final int AD_SERVICES = 1000000;
    private SdkExtensions() {}
    public static int getExtensionVersion(int sdk) {
        if (sdk < 30 && sdk != AD_SERVICES) throw new IllegalArgumentException("not a valid extension: " + sdk);
        int dev = android.os.Build.VERSION.SDK_INT;
        if (sdk == AD_SERVICES) return dev >= 34 ? 7 : 0;
        if (sdk > dev) return 0;
        return dev >= 34 ? 7 : dev == 33 ? 5 : dev == 32 || dev == 31 ? 2 : 1;
    }
    public static java.util.Map<Integer, Integer> getAllExtensionVersions() { java.util.Map<Integer, Integer> m = new java.util.HashMap<>(); for (int s = 30; s <= android.os.Build.VERSION.SDK_INT; s++) m.put(s, getExtensionVersion(s)); return m; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
