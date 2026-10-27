package android.view;
/** JET: API 30 WindowInsetsAnimation. The host never animates insets, so callbacks are registered but not run. */
public final class WindowInsetsAnimation {
    private final int typeMask; private final android.view.animation.Interpolator interp; private final long duration; private float fraction, alpha = 1f;
    public WindowInsetsAnimation(int typeMask, android.view.animation.Interpolator interpolator, long durationMillis) { this.typeMask = typeMask; interp = interpolator; duration = durationMillis; }
    public int getTypeMask() { return typeMask; }
    public float getFraction() { return fraction; } public void setFraction(float f) { fraction = f; }
    public float getInterpolatedFraction() { return interp != null ? interp.getInterpolation(fraction) : fraction; }
    public android.view.animation.Interpolator getInterpolator() { return interp; }
    public long getDurationMillis() { return duration; }
    public float getAlpha() { return alpha; } public void setAlpha(float a) { alpha = a; }
    public static final class Bounds {
        private final android.graphics.Insets lower, upper;
        public Bounds(android.graphics.Insets lowerBound, android.graphics.Insets upperBound) { lower = lowerBound; upper = upperBound; }
        public android.graphics.Insets getLowerBound() { return lower; } public android.graphics.Insets getUpperBound() { return upper; }
        public Bounds inset(android.graphics.Insets i) { return this; }
    }
    public abstract static class Callback {
        public static final int DISPATCH_MODE_STOP = 0, DISPATCH_MODE_CONTINUE_ON_SUBTREE = 1;
        private final int mode;
        public Callback(int dispatchMode) { mode = dispatchMode; }
        public final int getDispatchMode() { return mode; }
        public void onPrepare(WindowInsetsAnimation a) {}
        public Bounds onStart(WindowInsetsAnimation a, Bounds b) { return b; }
        public abstract WindowInsets onProgress(WindowInsets insets, java.util.List<WindowInsetsAnimation> running);
        public void onEnd(WindowInsetsAnimation a) {}
    }
}
