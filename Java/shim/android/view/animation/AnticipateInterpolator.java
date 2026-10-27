package android.view.animation;
public class AnticipateInterpolator extends BaseInterpolator {
    private final float k; public AnticipateInterpolator() { k = 2; } public AnticipateInterpolator(float k) { this.k = k; } public float getInterpolation(float t) { return t * t * ((k + 1) * t - k); }
}
