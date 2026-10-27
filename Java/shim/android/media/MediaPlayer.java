package android.media;
/** Silent MediaPlayer (headless harness): a correct state machine with listeners, no audio output.
 * Durations are 0 (unknown); the iOS backend maps this to AVAudioPlayer. */
public class MediaPlayer {
    public interface OnCompletionListener { void onCompletion(MediaPlayer mp); }
    public interface OnErrorListener { boolean onError(MediaPlayer mp, int what, int extra); }
    public interface OnPreparedListener { void onPrepared(MediaPlayer mp); }
    public interface OnSeekCompleteListener { void onSeekComplete(MediaPlayer mp); }
    public interface OnInfoListener { boolean onInfo(MediaPlayer mp, int what, int extra); }
    public interface OnBufferingUpdateListener { void onBufferingUpdate(MediaPlayer mp, int percent); }
    public interface OnVideoSizeChangedListener { void onVideoSizeChanged(MediaPlayer mp, int w, int h); }
    public static final int MEDIA_ERROR_UNKNOWN = 1, MEDIA_ERROR_SERVER_DIED = 100, MEDIA_INFO_UNKNOWN = 1, SEEK_PREVIOUS_SYNC = 0, SEEK_NEXT_SYNC = 1, SEEK_CLOSEST_SYNC = 2, SEEK_CLOSEST = 3;
    private enum S { IDLE, INITIALIZED, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED, END }
    private S st = S.IDLE; private boolean looping; private int pos; private OnPreparedListener prepared; private OnCompletionListener completion; private OnSeekCompleteListener seek;
    private final android.os.Handler h;
    public MediaPlayer() { android.os.Looper l = android.os.Looper.myLooper(); h = new android.os.Handler(l != null ? l : android.os.Looper.getMainLooper()); }
    public MediaPlayer(android.content.Context c) { this(); }
    public static MediaPlayer create(android.content.Context c, int resid) { MediaPlayer m = new MediaPlayer(); m.st = S.PREPARED; return m; }
    public static MediaPlayer create(android.content.Context c, android.net.Uri uri) { MediaPlayer m = new MediaPlayer(); m.st = S.PREPARED; return m; }
    private void src() { if (st != S.IDLE) throw new IllegalStateException("setDataSource in state " + st); st = S.INITIALIZED; }
    public void setDataSource(String path) { src(); }
    public void setDataSource(java.io.FileDescriptor fd) { src(); }
    public void setDataSource(java.io.FileDescriptor fd, long off, long len) { src(); }
    public void setDataSource(android.content.Context c, android.net.Uri u) { src(); }
    public void setDataSource(android.content.Context c, android.net.Uri u, java.util.Map<String, String> headers) { src(); }
    public void setDataSource(android.content.res.AssetFileDescriptor afd) { src(); }
    public void prepare() throws java.io.IOException { if (st != S.INITIALIZED && st != S.STOPPED) throw new IllegalStateException("prepare in state " + st); st = S.PREPARED; }
    public void prepareAsync() { if (st != S.INITIALIZED && st != S.STOPPED) throw new IllegalStateException("prepareAsync in state " + st); st = S.PREPARED; final OnPreparedListener l = prepared; if (l != null) h.post(() -> l.onPrepared(this)); }
    public void start() { if (st == S.IDLE || st == S.INITIALIZED || st == S.END) throw new IllegalStateException("start in state " + st); st = S.STARTED; }
    public void pause() { if (st == S.STARTED) st = S.PAUSED; }
    public void stop() { if (st != S.IDLE && st != S.END) st = S.STOPPED; }
    public void reset() { st = S.IDLE; pos = 0; }
    public void release() { st = S.END; }
    public boolean isPlaying() { return st == S.STARTED; }
    public void setLooping(boolean l) { looping = l; } public boolean isLooping() { return looping; }
    public int getDuration() { return 0; }
    public int getCurrentPosition() { return pos; }
    public void seekTo(int msec) { pos = Math.max(0, msec); final OnSeekCompleteListener l = seek; if (l != null) h.post(() -> l.onSeekComplete(this)); }
    public void seekTo(long msec, int mode) { seekTo((int) msec); }
    public void setVolume(float l, float r) {}
    public void setAudioStreamType(int t) {} public void setAudioAttributes(AudioAttributes a) {}
    public void setWakeMode(android.content.Context c, int mode) {} public void setScreenOnWhilePlaying(boolean on) {}
    public void setAudioSessionId(int id) {} public int getAudioSessionId() { return 1; }
    public int getVideoWidth() { return 0; } public int getVideoHeight() { return 0; }
    public void setNextMediaPlayer(MediaPlayer next) {}
    public void setOnCompletionListener(OnCompletionListener l) { completion = l; }
    public void setOnErrorListener(OnErrorListener l) {}
    public void setOnPreparedListener(OnPreparedListener l) { prepared = l; }
    public void setOnSeekCompleteListener(OnSeekCompleteListener l) { seek = l; }
    public void setOnInfoListener(OnInfoListener l) {} public void setOnBufferingUpdateListener(OnBufferingUpdateListener l) {} public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener l) {}
}
