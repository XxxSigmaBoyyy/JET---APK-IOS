package android.view.animation;
public class LayoutAnimationController { private Animation a; public LayoutAnimationController(Animation a) { this.a = a; } public LayoutAnimationController(Animation a, float delay) { this.a = a; } public Animation getAnimation() { return a; } public void setAnimation(Animation a) { this.a = a; } public void setDelay(float d) {} public void start() {} }
