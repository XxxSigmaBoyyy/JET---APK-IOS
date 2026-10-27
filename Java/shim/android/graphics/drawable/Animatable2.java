package android.graphics.drawable;
/** JET: android.graphics.drawable.Animatable2. */
public interface Animatable2 extends Animatable {
    void registerAnimationCallback(AnimationCallback callback);
    boolean unregisterAnimationCallback(AnimationCallback callback);
    void clearAnimationCallbacks();
    abstract class AnimationCallback { public void onAnimationStart(Drawable drawable) {} public void onAnimationEnd(Drawable drawable) {} }
}
