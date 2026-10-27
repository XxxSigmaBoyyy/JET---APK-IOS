package android.transition;
import android.view.View;
import android.view.ViewGroup;
/** android.transition.Transition: JET does not animate scene transitions yet; configuration is kept, animations are no-ops (end state applies directly). */
public abstract class Transition implements Cloneable {
    public static final int MATCH_INSTANCE = 1, MATCH_NAME = 2, MATCH_ID = 3, MATCH_ITEM_ID = 4;
    public interface TransitionListener {
        void onTransitionStart(Transition t); void onTransitionEnd(Transition t); void onTransitionCancel(Transition t);
        void onTransitionPause(Transition t); void onTransitionResume(Transition t);
    }
    public static abstract class EpicenterCallback { public EpicenterCallback() {} public abstract android.graphics.Rect onGetEpicenter(Transition t); }
    private long mDuration = -1, mStartDelay = -1; private android.animation.TimeInterpolator mInterp;
    private final java.util.ArrayList<TransitionListener> mListeners = new java.util.ArrayList<>();
    private final java.util.ArrayList<Integer> mTargetIds = new java.util.ArrayList<>();
    private final java.util.ArrayList<View> mTargets = new java.util.ArrayList<>();
    private EpicenterCallback mEpicenter;
    public Transition() {}
    public Transition(android.content.Context c, android.util.AttributeSet a) {}
    public abstract void captureStartValues(TransitionValues v);
    public abstract void captureEndValues(TransitionValues v);
    public Transition setDuration(long d) { mDuration = d; return this; }
    public long getDuration() { return mDuration; }
    public Transition setStartDelay(long d) { mStartDelay = d; return this; }
    public long getStartDelay() { return mStartDelay; }
    public Transition setInterpolator(android.animation.TimeInterpolator i) { mInterp = i; return this; }
    public android.animation.TimeInterpolator getInterpolator() { return mInterp; }
    public Transition addListener(TransitionListener l) { mListeners.add(l); return this; }
    public Transition removeListener(TransitionListener l) { mListeners.remove(l); return this; }
    public Transition addTarget(int id) { mTargetIds.add(id); return this; }
    public Transition addTarget(View v) { mTargets.add(v); return this; }
    public Transition addTarget(String name) { return this; }
    public Transition addTarget(Class<?> c) { return this; }
    public Transition removeTarget(int id) { mTargetIds.remove((Integer) id); return this; }
    public Transition removeTarget(View v) { mTargets.remove(v); return this; }
    public Transition removeTarget(String name) { return this; }
    public Transition removeTarget(Class<?> c) { return this; }
    public java.util.List<Integer> getTargetIds() { return mTargetIds; }
    public java.util.List<View> getTargets() { return mTargets; }
    public Transition excludeTarget(int id, boolean e) { return this; }
    public Transition excludeTarget(View v, boolean e) { return this; }
    public Transition excludeTarget(String n, boolean e) { return this; }
    public Transition excludeTarget(Class<?> c, boolean e) { return this; }
    public Transition excludeChildren(int id, boolean e) { return this; }
    public Transition excludeChildren(View v, boolean e) { return this; }
    public Transition excludeChildren(Class<?> c, boolean e) { return this; }
    public void setEpicenterCallback(EpicenterCallback c) { mEpicenter = c; }
    public EpicenterCallback getEpicenterCallback() { return mEpicenter; }
    public android.graphics.Rect getEpicenter() { return mEpicenter == null ? null : mEpicenter.onGetEpicenter(this); }
    public android.animation.Animator createAnimator(ViewGroup root, TransitionValues s, TransitionValues e) { return null; }
    public String[] getTransitionProperties() { return null; }
    public TransitionValues getTransitionValues(View v, boolean start) { return null; }
    public boolean isTransitionRequired(TransitionValues s, TransitionValues e) { return false; }
    public void setMatchOrder(int... m) {}
    public String getName() { return getClass().getName(); }
    @Override public Transition clone() { try { return (Transition) super.clone(); } catch (CloneNotSupportedException e) { throw new RuntimeException(e); } }
    @Override public String toString() { return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": dur(" + mDuration + ")"; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public java.util.List getTargetNames() { jet.runtime.ShimStubs.hit("android.transition.Transition.getTargetNames()Ljava/util/List;"); return null; }
    public java.util.List getTargetTypes() { jet.runtime.ShimStubs.hit("android.transition.Transition.getTargetTypes()Ljava/util/List;"); return null; }
    public boolean canRemoveViews() { jet.runtime.ShimStubs.hit("android.transition.Transition.canRemoveViews()Z"); return false; }
    // ---- END generated SDK surface ----
}
