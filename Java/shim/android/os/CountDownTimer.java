package android.os;
/** AOSP CountDownTimer semantics on the (virtual) SystemClock + main Handler. */
public abstract class CountDownTimer {
    private final long mMillisInFuture, mCountdownInterval;
    private long mStopTimeInFuture;
    private boolean mCancelled;
    private static final int MSG = 1;
    public CountDownTimer(long millisInFuture, long countDownInterval) { mMillisInFuture = millisInFuture; mCountdownInterval = countDownInterval; }
    public final synchronized void cancel() { mCancelled = true; mHandler.removeMessages(MSG); }
    public final synchronized CountDownTimer start() {
        mCancelled = false;
        if (mMillisInFuture <= 0) { onFinish(); return this; }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }
    public abstract void onTick(long millisUntilFinished);
    public abstract void onFinish();
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message msg) {
            synchronized (CountDownTimer.this) {
                if (mCancelled) return;
                final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();
                if (millisLeft <= 0) { onFinish(); return; }
                long lastTickStart = SystemClock.elapsedRealtime();
                onTick(millisLeft);
                long lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart;
                long delay;
                if (millisLeft < mCountdownInterval) { delay = millisLeft - lastTickDuration; if (delay < 0) delay = 0; }
                else { delay = mCountdownInterval - lastTickDuration; while (delay < 0) delay += mCountdownInterval; }
                if (!mCancelled) sendMessageDelayed(obtainMessage(MSG), delay);
            }
        }
    };
}
