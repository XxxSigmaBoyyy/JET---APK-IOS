package android.view;
import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import java.util.ArrayList;
import java.util.HashMap;
/** JET: AOSP-shaped ViewPropertyAnimator backed by ValueAnimator (virtual-time Choreographer/Handler frames). */
public class ViewPropertyAnimator {
    static final int TX = 1, TY = 2, TZ = 4, SX = 8, SY = 16, ROT = 32, RX = 64, RY = 128, X = 256, Y = 512, Z = 1024, A = 2048;
    private static final class Holder { final int prop; final float from, delta; Holder(int p, float f, float d) { prop = p; from = f; delta = d; } }
    private static final class Batch { int mask; final ArrayList<Holder> holders; Batch(int m, ArrayList<Holder> h) { mask = m; holders = h; } }
    private final View v;
    private long mDuration = -1, mStartDelay = 0; private TimeInterpolator mInterp;
    private Animator.AnimatorListener mListener; private ValueAnimator.AnimatorUpdateListener mUpdate;
    private Runnable mPendingStart, mPendingEnd;
    private final ArrayList<Holder> mPending = new ArrayList<>();
    private final HashMap<ValueAnimator, Batch> mRunning = new HashMap<>();
    private final Runnable mStarter = this::startAnimation;
    ViewPropertyAnimator(View v) { this.v = v; }
    private float get(int p) {
        switch (p) { case TX: return v.getTranslationX(); case TY: return v.getTranslationY(); case TZ: return v.getTranslationZ(); case SX: return v.getScaleX(); case SY: return v.getScaleY();
            case ROT: return v.getRotation(); case RX: return v.getRotationX(); case RY: return v.getRotationY(); case X: return v.getX(); case Y: return v.getY(); case Z: return v.getZ(); default: return v.getAlpha(); }
    }
    private void set(int p, float f) {
        switch (p) { case TX: v.setTranslationX(f); break; case TY: v.setTranslationY(f); break; case TZ: v.setTranslationZ(f); break; case SX: v.setScaleX(f); break; case SY: v.setScaleY(f); break;
            case ROT: v.setRotation(f); break; case RX: v.setRotationX(f); break; case RY: v.setRotationY(f); break; case X: v.setX(f); break; case Y: v.setY(f); break; case Z: v.setZ(f); break; default: v.setAlpha(f); }
    }
    private ViewPropertyAnimator to(int p, float val) { return by(p, get(p), val - get(p)); }
    private ViewPropertyAnimator by(int p, float from, float delta) {
        // A new request for a property cancels that property in running batches (AOSP animatePropertyBy).
        for (Batch b : mRunning.values()) if ((b.mask & p) != 0) { b.mask &= ~p; b.holders.removeIf(h -> h.prop == p); }
        mPending.removeIf(h -> h.prop == p);
        mPending.add(new Holder(p, from, delta));
        v.removeCallbacks(mStarter); v.postOnAnimation(mStarter);
        return this;
    }
    public ViewPropertyAnimator alpha(float a) { return to(A, a); } public ViewPropertyAnimator alphaBy(float a) { return by(A, get(A), a); }
    public ViewPropertyAnimator translationX(float x) { return to(TX, x); } public ViewPropertyAnimator translationXBy(float x) { return by(TX, get(TX), x); }
    public ViewPropertyAnimator translationY(float y) { return to(TY, y); } public ViewPropertyAnimator translationYBy(float y) { return by(TY, get(TY), y); }
    public ViewPropertyAnimator translationZ(float z) { return to(TZ, z); } public ViewPropertyAnimator translationZBy(float z) { return by(TZ, get(TZ), z); }
    public ViewPropertyAnimator x(float x) { return to(X, x); } public ViewPropertyAnimator xBy(float x) { return by(X, get(X), x); }
    public ViewPropertyAnimator y(float y) { return to(Y, y); } public ViewPropertyAnimator yBy(float y) { return by(Y, get(Y), y); }
    public ViewPropertyAnimator z(float z) { return to(Z, z); } public ViewPropertyAnimator zBy(float z) { return by(Z, get(Z), z); }
    public ViewPropertyAnimator scaleX(float s) { return to(SX, s); } public ViewPropertyAnimator scaleXBy(float s) { return by(SX, get(SX), s); }
    public ViewPropertyAnimator scaleY(float s) { return to(SY, s); } public ViewPropertyAnimator scaleYBy(float s) { return by(SY, get(SY), s); }
    public ViewPropertyAnimator rotation(float r) { return to(ROT, r); } public ViewPropertyAnimator rotationBy(float r) { return by(ROT, get(ROT), r); }
    public ViewPropertyAnimator rotationX(float r) { return to(RX, r); } public ViewPropertyAnimator rotationXBy(float r) { return by(RX, get(RX), r); }
    public ViewPropertyAnimator rotationY(float r) { return to(RY, r); } public ViewPropertyAnimator rotationYBy(float r) { return by(RY, get(RY), r); }
    public ViewPropertyAnimator setDuration(long d) { if (d < 0) throw new IllegalArgumentException("Animators cannot have negative duration: " + d); mDuration = d; return this; }
    public long getDuration() { return mDuration >= 0 ? mDuration : 300; }
    public ViewPropertyAnimator setStartDelay(long d) { if (d < 0) throw new IllegalArgumentException("Animators cannot have negative start delay: " + d); mStartDelay = d; return this; }
    public long getStartDelay() { return mStartDelay; }
    public ViewPropertyAnimator setInterpolator(TimeInterpolator i) { mInterp = i; return this; }
    public TimeInterpolator getInterpolator() { return mInterp; }
    public ViewPropertyAnimator setListener(Animator.AnimatorListener l) { mListener = l; return this; }
    public ViewPropertyAnimator setUpdateListener(ValueAnimator.AnimatorUpdateListener l) { mUpdate = l; return this; }
    public ViewPropertyAnimator withEndAction(Runnable r) { mPendingEnd = r; return this; }
    public ViewPropertyAnimator withStartAction(Runnable r) { mPendingStart = r; return this; }
    public ViewPropertyAnimator withLayer() { return this; }
    public void start() { v.removeCallbacks(mStarter); startAnimation(); }
    public void cancel() {
        v.removeCallbacks(mStarter); mPending.clear();
        for (ValueAnimator a : new ArrayList<>(mRunning.keySet())) a.cancel();
        mRunning.clear();
    }
    private void startAnimation() {
        if (mPending.isEmpty() && mPendingStart == null && mPendingEnd == null) return;
        final ValueAnimator anim = ValueAnimator.ofFloat(1f);
        int mask = 0; ArrayList<Holder> hs = new ArrayList<>(mPending); for (Holder h : hs) mask |= h.prop;
        mPending.clear();
        final Batch batch = new Batch(mask, hs);
        mRunning.put(anim, batch);
        final Runnable startAct = mPendingStart, endAct = mPendingEnd; mPendingStart = null; mPendingEnd = null;
        final Animator.AnimatorListener listener = mListener; final ValueAnimator.AnimatorUpdateListener upd = mUpdate;
        anim.setDuration(getDuration()); anim.setStartDelay(mStartDelay);
        if (mInterp != null) anim.setInterpolator(mInterp);
        anim.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator a) { if (startAct != null) startAct.run(); if (listener != null) listener.onAnimationStart(a); }
            public void onAnimationEnd(Animator a) { mRunning.remove(anim); if (listener != null) listener.onAnimationEnd(a); if (endAct != null) endAct.run(); }
            public void onAnimationCancel(Animator a) { mRunning.remove(anim); if (listener != null) listener.onAnimationCancel(a); }
            public void onAnimationRepeat(Animator a) { if (listener != null) listener.onAnimationRepeat(a); }
        });
        anim.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            for (Holder h : batch.holders) set(h.prop, h.from + h.delta * f);
            v.invalidate();
            if (upd != null) upd.onAnimationUpdate(a);
        });
        anim.start();
    }
}
