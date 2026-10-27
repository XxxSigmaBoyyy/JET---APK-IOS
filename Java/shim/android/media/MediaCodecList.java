package android.media;
/** No media codecs are available on this host (JET has no MediaCodec backend): the list is empty, findDecoder/EncoderForFormat return null. */
public final class MediaCodecList {
    public static final int REGULAR_CODECS = 0, ALL_CODECS = 1;
    private final int mKind;
    public MediaCodecList(int kind) { mKind = kind; }
    public static int getCodecCount() { return 0; }
    public static MediaCodecInfo getCodecInfoAt(int index) { throw new IllegalArgumentException(); }
    public MediaCodecInfo[] getCodecInfos() { return new MediaCodecInfo[0]; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
