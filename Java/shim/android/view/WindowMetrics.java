package android.view;
/** JET: API 30 WindowMetrics. The app window fills the host display; insets are zero (no system bars). */
public final class WindowMetrics {
    private final android.graphics.Rect bounds; private final WindowInsets insets; private final float density;
    public WindowMetrics(android.graphics.Rect bounds, WindowInsets insets) { this(bounds, insets, Display.jetDefault().jetDensity()); }
    public WindowMetrics(android.graphics.Rect bounds, WindowInsets insets, float density) { this.bounds = bounds; this.insets = insets; this.density = density; }
    public android.graphics.Rect getBounds() { return new android.graphics.Rect(bounds); }
    public WindowInsets getWindowInsets() { return insets; }
    public float getDensity() { return density; }
    static WindowMetrics jetCurrent() { Display d = Display.jetDefault(); return new WindowMetrics(new android.graphics.Rect(0, 0, d.getWidth(), d.getHeight()), WindowInsets.CONSUMED, d.jetDensity()); }
}
