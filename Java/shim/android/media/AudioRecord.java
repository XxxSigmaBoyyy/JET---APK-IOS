package android.media;
/** No capture device on this host: buffer sizes follow the AOSP formula (20 ms, >= 2 periods), but instances stay
 *  STATE_UNINITIALIZED and startRecording() throws, exactly as Android behaves when the mic cannot be opened. */
public class AudioRecord {
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_INVALID_OPERATION = -3;
    public static final int RECORDSTATE_STOPPED = 1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int ERROR = -1;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int READ_BLOCKING = 0;
    public static final int READ_NON_BLOCKING = 1;
    public static final int RECORDSTATE_RECORDING = 3;
    public static final int STATE_INITIALIZED = 1;
    public static final int SUCCESS = 0;
    private final int mRate, mChannelMask, mFormat;
    public AudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) { mRate = sampleRateInHz; mChannelMask = channelConfig; mFormat = audioFormat; System.err.println("[jet-audio] AudioRecord: no capture device on this host (STATE_UNINITIALIZED)"); }
    private static int channels(int cfg) { switch (cfg) { case 2: case 16: return 1; case 3: case 12: return 2; default: return Integer.bitCount(cfg) == 0 ? -1 : Integer.bitCount(cfg); } }
    private static int bytes(int fmt) { switch (fmt) { case 1: case 2: return 2; case 3: return 1; case 4: return 4; case 21: return 3; case 22: return 4; default: return -1; } }
    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int ch = channels(channelConfig), b = bytes(audioFormat);
        if (ch <= 0 || b <= 0 || sampleRateInHz < 4000 || sampleRateInHz > 192000) return ERROR_BAD_VALUE;
        int frames = sampleRateInHz / 50; return 2 * frames * ch * b;
    }
    public int getState() { return STATE_UNINITIALIZED; } public int getRecordingState() { return RECORDSTATE_STOPPED; }
    public int getSampleRate() { return mRate; } public int getChannelConfiguration() { return mChannelMask; } public int getAudioFormat() { return mFormat; }
    public int getChannelCount() { return Math.max(0, channels(mChannelMask)); } public int getAudioSessionId() { return 0; }
    public void startRecording() { throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord."); }
    public void stop() { throw new IllegalStateException("stop() called on an uninitialized AudioRecord."); }
    public void release() {}
    public int read(byte[] a, int o, int n) { return ERROR_INVALID_OPERATION; } public int read(short[] a, int o, int n) { return ERROR_INVALID_OPERATION; }
    public int read(java.nio.ByteBuffer b, int n) { return ERROR_INVALID_OPERATION; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.media.AudioDeviceInfo jet_PreferredDevice;
    private int jet_NotificationMarkerPosition;
    private int jet_PositionNotificationPeriod;
    public int getAudioSource() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.getAudioSource()I"); return 0; }
    public int getBufferSizeInFrames() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.getBufferSizeInFrames()I"); return 0; }
    public int getNotificationMarkerPosition() { return jet_NotificationMarkerPosition; }
    public int getPositionNotificationPeriod() { return jet_PositionNotificationPeriod; }
    public boolean isPrivacySensitive() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.isPrivacySensitive()Z"); return false; }
    public int read(byte[] p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.read([BIII)I"); return 0; }
    public int read(short[] p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.read([SIII)I"); return 0; }
    public int read(float[] p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.read([FIII)I"); return 0; }
    public int read(java.nio.ByteBuffer p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.read(Ljava/nio/ByteBuffer;II)I"); return 0; }
    public android.os.PersistableBundle getMetrics() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.getMetrics()Landroid/os/PersistableBundle;"); return null; }
    public int setNotificationMarkerPosition(int p0) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.setNotificationMarkerPosition(I)I"); return 0; }
    public android.media.AudioDeviceInfo getRoutedDevice() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.getRoutedDevice()Landroid/media/AudioDeviceInfo;"); return null; }
    public int setPositionNotificationPeriod(int p0) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.setPositionNotificationPeriod(I)I"); return 0; }
    public boolean setPreferredDevice(android.media.AudioDeviceInfo p0) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.setPreferredDevice(Landroid/media/AudioDeviceInfo;)Z"); return false; }
    public android.media.AudioDeviceInfo getPreferredDevice() { return jet_PreferredDevice; }
    public java.util.List getActiveMicrophones() { jet.runtime.ShimStubs.hit("android.media.AudioRecord.getActiveMicrophones()Ljava/util/List;"); return null; }
    public boolean setPreferredMicrophoneDirection(int p0) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.setPreferredMicrophoneDirection(I)Z"); return false; }
    public boolean setPreferredMicrophoneFieldDimension(float p0) { jet.runtime.ShimStubs.hit("android.media.AudioRecord.setPreferredMicrophoneFieldDimension(F)Z"); return false; }
    // ---- END generated SDK surface ----
}
