package android.view.animation;
public class OvershootInterpolator extends BaseInterpolator {
    private final float k; public OvershootInterpolator() { k = 2; } public OvershootInterpolator(float k) { this.k = k; } public float getInterpolation(float t) { t -= 1; return t * t * ((k + 1) * t + k) + 1; }
}
