package android.view.animation;
public class AccelerateInterpolator extends BaseInterpolator {
    private final float f; public AccelerateInterpolator() { f = 1; } public AccelerateInterpolator(float f) { this.f = f; } public AccelerateInterpolator(android.content.Context c, android.util.AttributeSet a) { f = 1; } public float getInterpolation(float t) { return f == 1 ? t * t : (float) Math.pow(t, 2 * f); }
}
