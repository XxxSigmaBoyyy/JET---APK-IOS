package android.animation;
import java.util.ArrayList;
/** Frame-stepped ValueAnimator: frames are posted to the main Looper every 16 ms (virtual time under Looper.jetPump on host). */
public class ValueAnimator extends Animator {
    public static final int RESTART = 1, REVERSE = 2, INFINITE = -1;
    public interface AnimatorUpdateListener { void onAnimationUpdate(ValueAnimator a); }
    /** API 33+: apps observe animator duration scale (developer options). */
    public interface DurationScaleChangeListener { void onChanged(float scale); }
    private static final java.util.concurrent.CopyOnWriteArrayList<DurationScaleChangeListener> sDurationScaleListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static boolean registerDurationScaleChangeListener(DurationScaleChangeListener l) {
        if (l == null) return false; return sDurationScaleListeners.addIfAbsent(l);
    }
    public static boolean unregisterDurationScaleChangeListener(DurationScaleChangeListener l) {
        return l != null && sDurationScaleListeners.remove(l);
    }
    private static float sDurationScale = 1f;
    public static void setDurationScale(float s) { sDurationScale = s; for (DurationScaleChangeListener l : sDurationScaleListeners) try { l.onChanged(s); } catch (Throwable ignored) {} } public static float getDurationScale() { return sDurationScale; }
    public static boolean areAnimatorsEnabled() { return sDurationScale != 0; }
    public static long getFrameDelay() { return 16; } public static void setFrameDelay(long d) {}
    PropertyValuesHolder[] mValues = new PropertyValuesHolder[0];
    private long mDuration = 300, mPlayed; private int mRepeatCount, mRepeatMode = RESTART, mIter; private TimeInterpolator mInterp = new android.view.animation.AccelerateDecelerateInterpolator();
    private final ArrayList<AnimatorUpdateListener> mUpdate = new ArrayList<>(); private float mFraction, mAnimFraction; private boolean mReversed, mStartedNotified; private int mGen;
    private android.os.Handler mHandler;
    public ValueAnimator() {}
    public static ValueAnimator ofFloat(float... v) { ValueAnimator a = new ValueAnimator(); a.setFloatValues(v); return a; }
    public static ValueAnimator ofInt(int... v) { ValueAnimator a = new ValueAnimator(); a.setIntValues(v); return a; }
    public static ValueAnimator ofArgb(int... v) { ValueAnimator a = new ValueAnimator(); a.setIntValues(v); a.setEvaluator(ArgbEvaluator.getInstance()); return a; }
    public static ValueAnimator ofObject(TypeEvaluator e, Object... v) { ValueAnimator a = new ValueAnimator(); a.setObjectValues(v); a.setEvaluator(e); return a; }
    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... v) { ValueAnimator a = new ValueAnimator(); a.setValues(v); return a; }
    public void setFloatValues(float... v) { if (mValues.length == 0) mValues = new PropertyValuesHolder[] { PropertyValuesHolder.ofFloat("", v) }; else mValues[0].setFloatValues(v); }
    public void setIntValues(int... v) { if (mValues.length == 0) mValues = new PropertyValuesHolder[] { PropertyValuesHolder.ofInt("", v) }; else mValues[0].setIntValues(v); }
    public void setObjectValues(Object... v) { if (mValues.length == 0) mValues = new PropertyValuesHolder[] { PropertyValuesHolder.ofObject("", null, v) }; else mValues[0].setObjectValues(v); }
    public void setValues(PropertyValuesHolder... v) { mValues = v == null ? new PropertyValuesHolder[0] : v; }
    public PropertyValuesHolder[] getValues() { return mValues; }
    public void setEvaluator(TypeEvaluator e) { if (mValues.length > 0) mValues[0].setEvaluator(e); }
    @Override public ValueAnimator setDuration(long d) { if (d < 0) throw new IllegalArgumentException("Animators cannot have negative duration: " + d); mDuration = d; return this; }
    @Override public long getDuration() { return mDuration; }
    @Override public long getTotalDuration() { return mRepeatCount == INFINITE ? -1 : mStartDelay + mDuration * (mRepeatCount + 1); }
    @Override public void setInterpolator(TimeInterpolator i) { mInterp = i == null ? new android.view.animation.LinearInterpolator() : i; }
    @Override public TimeInterpolator getInterpolator() { return mInterp; }
    public void setRepeatCount(int c) { mRepeatCount = c; } public int getRepeatCount() { return mRepeatCount; }
    public void setRepeatMode(int m) { mRepeatMode = m; } public int getRepeatMode() { return mRepeatMode; }
    public void addUpdateListener(AnimatorUpdateListener l) { mUpdate.add(l); } public void removeUpdateListener(AnimatorUpdateListener l) { mUpdate.remove(l); } public void removeAllUpdateListeners() { mUpdate.clear(); }
    public Object getAnimatedValue() { return mValues.length == 0 ? null : mValues[0].animated(); }
    public Object getAnimatedValue(String name) { for (PropertyValuesHolder h : mValues) if (name.equals(h.getPropertyName())) return h.animated(); return null; }
    public float getAnimatedFraction() { return mAnimFraction; }
    public long getCurrentPlayTime() { return mPlayed; }
    public void setCurrentPlayTime(long t) { mPlayed = t; setCurrentFraction(mDuration == 0 ? 1 : t / (float) mDuration); }
    public void setCurrentFraction(float f) { mFraction = f; animateValue(f); }
    void animateValue(float fraction) {
        float f = Math.max(0, Math.min(1, fraction)); if (mReversed) f = 1 - f;
        mAnimFraction = mInterp.getInterpolation(f);
        for (PropertyValuesHolder h : mValues) h.calc(mAnimFraction);
        jetApply();
        for (AnimatorUpdateListener l : new ArrayList<>(mUpdate)) l.onAnimationUpdate(this);
    }
    /** ObjectAnimator writes values to its target here. */
    void jetApply() {}
    @Override public void start() { start(false); }
    public void reverse() { start(!mReversed); }
    private void start(boolean reversed) {
        mReversed = reversed; mRunning = true; mPlayed = 0; mIter = 0; mStartedNotified = false; final int gen = ++mGen;
        android.os.Looper lp = android.os.Looper.myLooper(); if (lp == null) lp = android.os.Looper.getMainLooper();
        mHandler = new android.os.Handler(lp);
        if (mStartDelay <= 0) notifyStart();
        animateValue(0);
        long total = (long) (mDuration * sDurationScale);
        if (total <= 0 && mStartDelay <= 0) { animateValue(1); finish(); return; }
        mHandler.postDelayed(() -> frame(gen), Math.max(16, mStartDelay));
    }
    private void notifyStart() { if (mStartedNotified) return; mStartedNotified = true; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationStart(this); }
    private void frame(int gen) {
        if (!mRunning || gen != mGen) return;
        notifyStart();
        long d = Math.max(1, (long) (mDuration * sDurationScale)); mPlayed += 16;
        if (mPlayed >= d) {
            if (mRepeatCount == INFINITE || mIter < mRepeatCount) {
                mIter++; mPlayed = 0; if (mRepeatMode == REVERSE) mReversed = !mReversed;
                for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationRepeat(this);
            } else { animateValue(1); finish(); return; }
        }
        animateValue(mPlayed / (float) d);
        mHandler.postDelayed(() -> frame(gen), 16);
    }
    private void finish() { mRunning = false; mGen++; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationEnd(this); }
    @Override public void end() { if (!mRunning) return; animateValue(1); finish(); }
    @Override public void cancel() { if (!mRunning) return; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationCancel(this); finish(); }
    @Override public boolean isRunning() { return mRunning; } @Override public boolean isStarted() { return mRunning; }
    @Override public ValueAnimator clone() { ValueAnimator a = (ValueAnimator) super.clone(); a.mValues = mValues.clone(); for (int i = 0; i < mValues.length; i++) a.mValues[i] = mValues[i].clone(); return a; }
}
