package android.view;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;

/** Choreographer at 60 Hz on the looper's Handler and the virtual SystemClock (vsync = 16 ms ticks). */
public final class Choreographer {
    public interface FrameCallback { void doFrame(long frameTimeNanos); }
    public static final int CALLBACK_INPUT = 0, CALLBACK_ANIMATION = 1, CALLBACK_INSETS_ANIMATION = 2, CALLBACK_TRAVERSAL = 3, CALLBACK_COMMIT = 4;
    static final long FRAME_MS = 16;
    private static final ThreadLocal<Choreographer> sTl = ThreadLocal.withInitial(() -> {
        Looper l = Looper.myLooper(); if (l == null) l = Looper.getMainLooper(); return new Choreographer(l);
    });
    private static Choreographer sMain;
    private final Handler mHandler;
    private final ArrayList<Object[]> mPending = new ArrayList<>(); // {callbackOrRunnable, token, dueMs}
    private boolean mScheduled;
    private long mLastFrameNanos;

    private Choreographer(Looper l) { mHandler = new Handler(l); }
    public static Choreographer getInstance() { return sTl.get(); }
    public static synchronized Choreographer getMainThreadInstance() { if (sMain == null) sMain = new Choreographer(Looper.getMainLooper()); return sMain; }
    public static long getFrameDelay() { return FRAME_MS; }
    public static void setFrameDelay(long d) {}
    public long getFrameTime() { return getFrameTimeNanos() / 1_000_000L; }
    public long getFrameTimeNanos() { return mLastFrameNanos != 0 ? mLastFrameNanos : SystemClock.uptimeMillis() * 1_000_000L; }

    public void postFrameCallback(FrameCallback cb) { postFrameCallbackDelayed(cb, 0); }
    public void postFrameCallbackDelayed(FrameCallback cb, long delayMillis) { if (cb == null) throw new IllegalArgumentException("callback must not be null"); add(cb, null, delayMillis); }
    public void removeFrameCallback(FrameCallback cb) { remove(cb, null); }
    public void postCallback(int type, Runnable action, Object token) { postCallbackDelayed(type, action, token, 0); }
    public void postCallbackDelayed(int type, Runnable action, Object token, long delayMillis) { add(action, token, delayMillis); }
    public void removeCallbacks(int type, Runnable action, Object token) { remove(action, token); }

    private synchronized void add(Object cb, Object token, long delay) {
        mPending.add(new Object[]{cb, token, SystemClock.uptimeMillis() + Math.max(0, delay)});
        if (!mScheduled) { mScheduled = true; mHandler.postDelayed(this::doFrame, nextVsyncDelay()); }
    }
    private synchronized void remove(Object cb, Object token) { mPending.removeIf(e -> (cb == null || e[0] == cb) && (token == null || e[1] == token)); }
    private static long nextVsyncDelay() { long now = SystemClock.uptimeMillis(); return FRAME_MS - (now % FRAME_MS); }
    private void doFrame() {
        ArrayList<Object[]> run = new ArrayList<>();
        long now = SystemClock.uptimeMillis();
        synchronized (this) {
            mScheduled = false;
            mLastFrameNanos = now * 1_000_000L;
            for (java.util.Iterator<Object[]> it = mPending.iterator(); it.hasNext(); ) { Object[] e = it.next(); if ((long) e[2] <= now) { run.add(e); it.remove(); } }
        }
        for (Object[] e : run) {
            if (e[0] instanceof FrameCallback) ((FrameCallback) e[0]).doFrame(mLastFrameNanos);
            else ((Runnable) e[0]).run();
        }
        synchronized (this) { if (!mPending.isEmpty() && !mScheduled) { mScheduled = true; mHandler.postDelayed(this::doFrame, nextVsyncDelay()); } }
    }
}
