package android.view.animation;
public class CycleInterpolator extends BaseInterpolator {
    private final float c; public CycleInterpolator(float c) { this.c = c; } public float getInterpolation(float t) { return (float) Math.sin(2 * c * Math.PI * t); }
}
