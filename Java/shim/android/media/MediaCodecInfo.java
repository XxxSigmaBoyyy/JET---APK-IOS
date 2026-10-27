package android.media;
public final class MediaCodecInfo {
    private final String mName; private final boolean mEncoder; private final String[] mTypes;
    MediaCodecInfo(String name, boolean encoder, String[] types) { mName = name; mEncoder = encoder; mTypes = types; }
    public String getName() { return mName; } public String getCanonicalName() { return mName; } public boolean isEncoder() { return mEncoder; }
    public boolean isAlias() { return false; } public boolean isHardwareAccelerated() { return false; } public boolean isSoftwareOnly() { return true; } public boolean isVendor() { return false; }
    public String[] getSupportedTypes() { return mTypes.clone(); }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
