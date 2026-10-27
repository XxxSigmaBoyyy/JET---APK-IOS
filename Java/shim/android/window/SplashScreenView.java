package android.window;
/** JET: API 31 SplashScreenView (only created if a host ever shows a splash window). */
public final class SplashScreenView extends android.widget.FrameLayout {
    public SplashScreenView(android.content.Context c) { super(c); }
    public android.view.View getIconView() { return null; }
    public long getIconAnimationDuration() { return 0; }
    public java.time.Duration getIconAnimationDurationMillis() { return java.time.Duration.ZERO; }
    public java.time.Instant getIconAnimationStart() { return null; }
    public int getIconBackgroundColor() { return 0; }
    public void remove() { if (getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) getParent()).removeView(this); }
}
