package android.media;
/** Silent SoundPool (headless): loads report completion, play returns stream ids, nothing is audible. Audio backend is out of scope for the harness. */
public class SoundPool {
    public interface OnLoadCompleteListener { void onLoadComplete(SoundPool pool, int sampleId, int status); }
    private int nextSample = 1, nextStream = 1; private OnLoadCompleteListener listener; private final android.os.Handler h;
    @Deprecated public SoundPool(int maxStreams, int streamType, int srcQuality) { android.os.Looper l = android.os.Looper.myLooper(); h = new android.os.Handler(l != null ? l : android.os.Looper.getMainLooper()); }
    private int loaded() { final int id = nextSample++; final OnLoadCompleteListener l = listener; if (l != null) h.post(() -> l.onLoadComplete(this, id, 0)); return id; }
    public int load(String path, int priority) { return loaded(); }
    public int load(android.content.Context c, int resId, int priority) { return loaded(); }
    public int load(android.content.res.AssetFileDescriptor afd, int priority) { return loaded(); }
    public int load(java.io.FileDescriptor fd, long off, long len, int priority) { return loaded(); }
    public boolean unload(int soundId) { return true; }
    public int play(int soundId, float l, float r, int prio, int loop, float rate) { return soundId > 0 ? nextStream++ : 0; }
    public void pause(int s) {} public void resume(int s) {} public void stop(int s) {}
    public void autoPause() {} public void autoResume() {}
    public void setVolume(int s, float l, float r) {} public void setPriority(int s, int p) {} public void setLoop(int s, int loop) {} public void setRate(int s, float r) {}
    public void setOnLoadCompleteListener(OnLoadCompleteListener l) { listener = l; }
    public void release() {}
    public static class Builder {
        private int max = 1;
        public Builder() {}
        public Builder setMaxStreams(int m) { max = m; return this; }
        public Builder setAudioAttributes(AudioAttributes a) { return this; }
        public SoundPool build() { return new SoundPool(max, 3, 0); }
    }
}
