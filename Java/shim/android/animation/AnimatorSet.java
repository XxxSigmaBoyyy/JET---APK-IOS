package android.animation;
import java.util.ArrayList;
import java.util.List;
/** AnimatorSet: together/sequential groups (Builder with/before/after supported as a dependency chain). */
public final class AnimatorSet extends Animator {
    private final ArrayList<ArrayList<Animator>> mStages = new ArrayList<>(); private long mDuration = -1; private TimeInterpolator mInterp; private int mPending, mStage;
    public AnimatorSet() {}
    public void playTogether(Animator... a) { ArrayList<Animator> s = new ArrayList<>(); for (Animator x : a) if (x != null) s.add(x); mStages.add(s); }
    public void playTogether(java.util.Collection<Animator> a) { playTogether(a.toArray(new Animator[0])); }
    public void playSequentially(Animator... a) { for (Animator x : a) if (x != null) { ArrayList<Animator> s = new ArrayList<>(); s.add(x); mStages.add(s); } }
    public void playSequentially(List<Animator> a) { playSequentially(a.toArray(new Animator[0])); }
    public ArrayList<Animator> getChildAnimations() { ArrayList<Animator> r = new ArrayList<>(); for (ArrayList<Animator> s : mStages) r.addAll(s); return r; }
    public Builder play(Animator a) { return new Builder(a); }
    public final class Builder {
        private final ArrayList<Animator> stage;
        Builder(Animator a) { stage = find(a); if (stage == null) { ArrayList<Animator> s = new ArrayList<>(); s.add(a); mStages.add(s); } }
        private ArrayList<Animator> find(Animator a) { for (ArrayList<Animator> s : mStages) if (s.contains(a)) return s; return null; }
        private ArrayList<Animator> cur() { for (ArrayList<Animator> s : mStages) if (!s.isEmpty() && s == (stage != null ? stage : mStages.get(mStages.size() - 1))) return s; return mStages.get(mStages.size() - 1); }
        public Builder with(Animator a) { cur().add(a); return this; }
        public Builder before(Animator a) { int i = mStages.indexOf(cur()); ArrayList<Animator> s = new ArrayList<>(); s.add(a); mStages.add(i + 1, s); return this; }
        public Builder after(Animator a) { int i = mStages.indexOf(cur()); ArrayList<Animator> s = new ArrayList<>(); s.add(a); mStages.add(Math.max(0, i), s); return this; }
        public Builder after(long delay) { ValueAnimator v = ValueAnimator.ofFloat(0f, 1f); v.setDuration(delay); return after(v); }
    }
    @Override public AnimatorSet setDuration(long d) { mDuration = d; return this; }
    @Override public long getDuration() { return mDuration; }
    @Override public void setInterpolator(TimeInterpolator i) { mInterp = i; }
    @Override public TimeInterpolator getInterpolator() { return mInterp; }
    @Override public void setTarget(Object t) { for (Animator a : getChildAnimations()) a.setTarget(t); }
    @Override public void start() { mRunning = true; mStage = -1; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationStart(this); next(); }
    private void next() {
        if (!mRunning) return;
        mStage++;
        if (mStage >= mStages.size()) { mRunning = false; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationEnd(this); return; }
        ArrayList<Animator> s = mStages.get(mStage); mPending = s.size(); if (mPending == 0) { next(); return; }
        for (Animator a : s) {
            if (mDuration >= 0) a.setDuration(mDuration); if (mInterp != null) a.setInterpolator(mInterp);
            final int st = mStage;
            a.addListener(new AnimatorListenerAdapter() { @Override public void onAnimationEnd(Animator x) { x.removeListener(this); if (st == mStage && --mPending == 0) next(); } });
            a.start();
        }
    }
    @Override public void cancel() { if (!mRunning) return; for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationCancel(this); mRunning = false; for (Animator a : getChildAnimations()) a.cancel(); for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationEnd(this); }
    @Override public void end() { if (!mRunning) return; mRunning = false; for (Animator a : getChildAnimations()) a.end(); for (AnimatorListener l : new ArrayList<>(mListeners)) l.onAnimationEnd(this); }
    @Override public boolean isRunning() { return mRunning; }
    @Override public AnimatorSet clone() { return (AnimatorSet) super.clone(); }
}
