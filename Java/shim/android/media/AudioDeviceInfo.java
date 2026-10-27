package android.media;
/** Host audio endpoints: one built-in speaker (output) and one built-in mic (input). */
public final class AudioDeviceInfo {
    public static final int TYPE_UNKNOWN = 0, TYPE_BUILTIN_EARPIECE = 1, TYPE_BUILTIN_SPEAKER = 2, TYPE_WIRED_HEADSET = 3, TYPE_WIRED_HEADPHONES = 4,
        TYPE_BLUETOOTH_SCO = 7, TYPE_BLUETOOTH_A2DP = 8, TYPE_HDMI = 9, TYPE_USB_DEVICE = 11, TYPE_USB_HEADSET = 22, TYPE_BUILTIN_MIC = 15, TYPE_TELEPHONY = 18;
    private final int mId, mType; private final boolean mSink;
    AudioDeviceInfo(int id, int type, boolean sink) { mId = id; mType = type; mSink = sink; }
    public int getId() { return mId; } public int getType() { return mType; }
    public boolean isSink() { return mSink; } public boolean isSource() { return !mSink; }
    public CharSequence getProductName() { return "JET host"; } public String getAddress() { return ""; }
    public int[] getSampleRates() { return new int[] { 48000 }; } public int[] getChannelCounts() { return new int[] { mSink ? 2 : 1 }; }
    public int[] getChannelMasks() { return new int[0]; } public int[] getChannelIndexMasks() { return new int[0]; }
    public int[] getEncodings() { return new int[] { 2 /* ENCODING_PCM_16BIT */ }; }
}
