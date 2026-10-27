package android.animation;
public class StateListAnimator implements Cloneable {
    public StateListAnimator() {} public void addState(int[] specs, Animator a) {} public void jumpToCurrentState() {}
    @Override public StateListAnimator clone() { return new StateListAnimator(); }
}
