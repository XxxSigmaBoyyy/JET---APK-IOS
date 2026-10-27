package android.animation;
public abstract class Animator implements Cloneable {
    public interface AnimatorListener { void onAnimationStart(Animator a); void onAnimationEnd(Animator a); void onAnimationCancel(Animator a); void onAnimationRepeat(Animator a);
        default void onAnimationStart(Animator a, boolean r) { onAnimationStart(a); } default void onAnimationEnd(Animator a, boolean r) { onAnimationEnd(a); } }
    public interface AnimatorPauseListener { void onAnimationPause(Animator a); void onAnimationResume(Animator a); }
    protected final java.util.ArrayList<AnimatorListener> mListeners = new java.util.ArrayList<>(); protected long mStartDelay; protected boolean mRunning;
    public void start() { mRunning = true; for (AnimatorListener l : new java.util.ArrayList<>(mListeners)) l.onAnimationStart(this); end(); }
    public void cancel() { if (mRunning) for (AnimatorListener l : new java.util.ArrayList<>(mListeners)) l.onAnimationCancel(this); end(); }
    public void end() { if (mRunning) { mRunning = false; for (AnimatorListener l : new java.util.ArrayList<>(mListeners)) l.onAnimationEnd(this); } }
    public void pause() {} public void resume() {} public boolean isPaused() { return false; }
    public long getStartDelay() { return mStartDelay; } public void setStartDelay(long d) { mStartDelay = d; }
    public abstract Animator setDuration(long d); public abstract long getDuration(); public long getTotalDuration() { return mStartDelay + getDuration(); }
    public abstract void setInterpolator(TimeInterpolator i); public TimeInterpolator getInterpolator() { return null; }
    public boolean isRunning() { return mRunning; } public boolean isStarted() { return mRunning; }
    public void addListener(AnimatorListener l) { mListeners.add(l); } public void removeListener(AnimatorListener l) { mListeners.remove(l); }
    public java.util.ArrayList<AnimatorListener> getListeners() { return mListeners; } public void removeAllListeners() { mListeners.clear(); }
    public void addPauseListener(AnimatorPauseListener l) {} public void removePauseListener(AnimatorPauseListener l) {} public void removeAllPauseListeners() {}
    public void setTarget(Object t) {} public void setupStartValues() {} public void setupEndValues() {}
    @Override public Animator clone() { try { return (Animator) super.clone(); } catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
}
