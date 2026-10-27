package android.media;
/** android.media.Ringtone: silent on the host (no audio device); state is tracked. */
public class Ringtone {
    private final android.net.Uri uri; private boolean playing, looping; private float volume = 1f; private int stream = AudioManager.STREAM_RING; private AudioAttributes attrs;
    Ringtone(android.content.Context c, android.net.Uri u) { uri = u; }
    public String getTitle(android.content.Context c) { int t = RingtoneManager.getDefaultType(uri); return t == RingtoneManager.TYPE_ALARM ? "Default alarm sound" : t == RingtoneManager.TYPE_NOTIFICATION ? "Default notification sound" : "Default ringtone"; }
    public void play() { playing = true; } public void stop() { playing = false; } public boolean isPlaying() { return playing; }
    public void setLooping(boolean l) { looping = l; } public boolean isLooping() { return looping; }
    public void setVolume(float v) { volume = v; } public float getVolume() { return volume; }
    public void setStreamType(int s) { stream = s; } public int getStreamType() { return stream; }
    public void setAudioAttributes(AudioAttributes a) { attrs = a; } public AudioAttributes getAudioAttributes() { return attrs; }
    public boolean isHapticGeneratorEnabled() { return false; } public boolean setHapticGeneratorEnabled(boolean e) { return false; }
}
