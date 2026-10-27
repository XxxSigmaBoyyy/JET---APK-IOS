package android.media;
public class AudioManager {
    public static final int STREAM_VOICE_CALL = 0, STREAM_SYSTEM = 1, STREAM_RING = 2, STREAM_MUSIC = 3, STREAM_ALARM = 4, STREAM_NOTIFICATION = 5, USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;
    public static final int RINGER_MODE_SILENT = 0, RINGER_MODE_VIBRATE = 1, RINGER_MODE_NORMAL = 2, AUDIOFOCUS_GAIN = 1, AUDIOFOCUS_GAIN_TRANSIENT = 2, AUDIOFOCUS_LOSS = -1, AUDIOFOCUS_LOSS_TRANSIENT = -2, AUDIOFOCUS_REQUEST_GRANTED = 1, AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int ADJUST_RAISE = 1, ADJUST_LOWER = -1, ADJUST_SAME = 0, FLAG_SHOW_UI = 1, MODE_NORMAL = 0;
    public static final String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY", PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE", PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";
    public interface OnAudioFocusChangeListener { void onAudioFocusChange(int focusChange); }
    private final int[] vol = {5, 7, 5, 8, 6, 5}; private int ringer = RINGER_MODE_NORMAL;
    public int getStreamVolume(int s) { return s >= 0 && s < vol.length ? vol[s] : 5; } public int getStreamMaxVolume(int s) { return 15; } public int getStreamMinVolume(int s) { return 0; }
    public void setStreamVolume(int s, int v, int f) { if (s >= 0 && s < vol.length) vol[s] = v; } public void adjustStreamVolume(int s, int d, int f) { if (s >= 0 && s < vol.length) vol[s] = Math.max(0, Math.min(15, vol[s] + d)); }
    public int getRingerMode() { return ringer; } public void setRingerMode(int m) { ringer = m; } public int getMode() { return MODE_NORMAL; }
    public int requestAudioFocus(OnAudioFocusChangeListener l, int s, int d) { return AUDIOFOCUS_REQUEST_GRANTED; } public int abandonAudioFocus(OnAudioFocusChangeListener l) { return AUDIOFOCUS_REQUEST_GRANTED; }
    public boolean isMusicActive() { return false; } public boolean isWiredHeadsetOn() { return false; } public boolean isBluetoothA2dpOn() { return false; } public boolean isSpeakerphoneOn() { return true; }
    public String getProperty(String k) { return PROPERTY_OUTPUT_SAMPLE_RATE.equals(k) ? "48000" : PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(k) ? "256" : null; }
    public static final int GET_DEVICES_INPUTS = 1, GET_DEVICES_OUTPUTS = 2, GET_DEVICES_ALL = 3;
    private static final AudioDeviceInfo SPEAKER = new AudioDeviceInfo(1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true), MIC = new AudioDeviceInfo(2, AudioDeviceInfo.TYPE_BUILTIN_MIC, false);
    public AudioDeviceInfo[] getDevices(int flags) { java.util.List<AudioDeviceInfo> l = new java.util.ArrayList<>(); if ((flags & GET_DEVICES_OUTPUTS) != 0) l.add(SPEAKER); if ((flags & GET_DEVICES_INPUTS) != 0) l.add(MIC); return l.toArray(new AudioDeviceInfo[0]); }
    public void registerAudioDeviceCallback(AudioDeviceCallback cb, android.os.Handler h) {
        if (cb == null) return; Runnable r = () -> cb.onAudioDevicesAdded(getDevices(GET_DEVICES_ALL));   // AOSP: initial devices reported on register
        if (h != null) h.post(r); else new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }
    public void unregisterAudioDeviceCallback(AudioDeviceCallback cb) {}
    public void playSoundEffect(int e) {} public void playSoundEffect(int e, float v) {}
}
