package android.view.animation;
public abstract class Animation implements Cloneable {
    public static final int INFINITE = -1, RESTART = 1, REVERSE = 2, START_ON_FIRST_FRAME = -1, ABSOLUTE = 0, RELATIVE_TO_SELF = 1, RELATIVE_TO_PARENT = 2, ZORDER_NORMAL = 0, ZORDER_TOP = 1, ZORDER_BOTTOM = -1;
    public interface AnimationListener { void onAnimationStart(Animation a); void onAnimationEnd(Animation a); void onAnimationRepeat(Animation a); }
    protected long mDuration, mStartOffset; protected Interpolator mInterpolator; protected boolean mFillAfter, mFillBefore = true, mFillEnabled; protected int mRepeatCount, mRepeatMode = RESTART;
    protected AnimationListener mListener; private boolean started, ended;
    public Animation() {} public Animation(android.content.Context c, android.util.AttributeSet a) {}
    public void setDuration(long d) { mDuration = d; } public long getDuration() { return mDuration; } public void setStartOffset(long o) { mStartOffset = o; } public long getStartOffset() { return mStartOffset; }
    public void setInterpolator(Interpolator i) { mInterpolator = i; } public void setInterpolator(android.content.Context c, int id) {} public Interpolator getInterpolator() { return mInterpolator; }
    public void setFillAfter(boolean b) { mFillAfter = b; } public boolean getFillAfter() { return mFillAfter; } public void setFillBefore(boolean b) { mFillBefore = b; } public void setFillEnabled(boolean b) { mFillEnabled = b; }
    public void setRepeatCount(int n) { mRepeatCount = n; } public int getRepeatCount() { return mRepeatCount; } public void setRepeatMode(int m) { mRepeatMode = m; } public int getRepeatMode() { return mRepeatMode; }
    public void setAnimationListener(AnimationListener l) { mListener = l; }
    public void start() { started = true; } public void startNow() { started = true; } public void cancel() { ended = true; } public void reset() { started = ended = false; }
    public boolean hasStarted() { return started; } public boolean hasEnded() { return ended; } public void setStartTime(long t) {} public long getStartTime() { return 0; }
    public void initialize(int w, int h, int pw, int ph) {} public boolean isInitialized() { return true; } public void setZAdjustment(int z) {} public void setBackgroundColor(int c) {} public void setDetachWallpaper(boolean b) {}
    public boolean willChangeBounds() { return true; } public boolean willChangeTransformationMatrix() { return true; } public long computeDurationHint() { return mStartOffset + mDuration; }
    /** Virtual-time harness: animations complete instantly when attached to a view. */
    public void jetRunToEnd() { started = true; if (mListener != null) mListener.onAnimationStart(this); ended = true; if (mListener != null) mListener.onAnimationEnd(this); }
    protected float resolveSize(int type, float v, int size, int psize) { return type == ABSOLUTE ? v : type == RELATIVE_TO_SELF ? v * size : v * psize; }
    @Override protected Animation clone() throws CloneNotSupportedException { return (Animation) super.clone(); }
}
