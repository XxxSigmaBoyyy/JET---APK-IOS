package android.view;
/** No system bars on the host harness: all insets are zero. */
public final class WindowInsets {
    public static final WindowInsets CONSUMED = new WindowInsets();
    public WindowInsets() {}
    public WindowInsets(WindowInsets o) {}
    public int getSystemWindowInsetLeft() { return 0; } public int getSystemWindowInsetTop() { return 0; }
    public int getSystemWindowInsetRight() { return 0; } public int getSystemWindowInsetBottom() { return 0; }
    public int getStableInsetLeft() { return 0; } public int getStableInsetTop() { return 0; }
    public int getStableInsetRight() { return 0; } public int getStableInsetBottom() { return 0; }
    public boolean hasSystemWindowInsets() { return false; } public boolean hasInsets() { return false; } public boolean hasStableInsets() { return false; }
    public boolean isConsumed() { return true; } public boolean isRound() { return false; }
    public WindowInsets consumeSystemWindowInsets() { return this; } public WindowInsets consumeStableInsets() { return this; }
    public WindowInsets consumeDisplayCutout() { return this; }
    public WindowInsets replaceSystemWindowInsets(int l, int t, int r, int b) { return this; }
    public WindowInsets replaceSystemWindowInsets(android.graphics.Rect r) { return this; }
    public DisplayCutout getDisplayCutout() { return null; } // host "phone" has no cutout
    public android.graphics.Insets getInsets(int typeMask) { return android.graphics.Insets.NONE; }
    public android.graphics.Insets getInsetsIgnoringVisibility(int typeMask) { return android.graphics.Insets.NONE; }
    public android.graphics.Insets getSystemWindowInsets() { return android.graphics.Insets.NONE; }
    public boolean isVisible(int typeMask) { return false; }
    // API 29-34 getters: everything is zero on the host "phone" (no bars, no gesture areas, no cutout)
    public android.graphics.Insets getSystemGestureInsets() { return android.graphics.Insets.NONE; }
    public android.graphics.Insets getMandatorySystemGestureInsets() { return android.graphics.Insets.NONE; }
    public android.graphics.Insets getTappableElementInsets() { return android.graphics.Insets.NONE; }
    public android.graphics.Insets getStableInsets() { return android.graphics.Insets.NONE; }
    public RoundedCorner getRoundedCorner(int position) { return null; }
    public android.graphics.Rect getPrivacyIndicatorBounds() { return null; }
    public DisplayShape getDisplayShape() { return null; }
    public java.util.List<android.graphics.Rect> getBoundingRects(int typeMask) { return java.util.Collections.emptyList(); }
    public WindowInsets inset(int l, int t, int r, int b) { return this; }
    public WindowInsets inset(android.graphics.Insets i) { return this; }
    public static final class Type {
        public static int statusBars() { return 1; } public static int navigationBars() { return 2; } public static int captionBar() { return 4; }
        public static int ime() { return 8; } public static int systemGestures() { return 16; } public static int mandatorySystemGestures() { return 32; }
        public static int tappableElement() { return 64; } public static int displayCutout() { return 128; } public static int systemBars() { return 7; }
        public static int systemOverlays() { return 256; }
    }
    public static final class Builder {
        public Builder() {} public Builder(WindowInsets i) {}
        public Builder setInsets(int t, android.graphics.Insets i) { return this; }
        public Builder setSystemWindowInsets(android.graphics.Insets i) { return this; }
        public Builder setInsetsIgnoringVisibility(int t, android.graphics.Insets i) { return this; }
        public Builder setVisible(int t, boolean v) { return this; }
        public Builder setStableInsets(android.graphics.Insets i) { return this; }
        public Builder setSystemGestureInsets(android.graphics.Insets i) { return this; }
        public Builder setMandatorySystemGestureInsets(android.graphics.Insets i) { return this; }
        public Builder setTappableElementInsets(android.graphics.Insets i) { return this; }
        public Builder setDisplayCutout(DisplayCutout c) { return this; }
        public Builder setRoundedCorner(int p, RoundedCorner c) { return this; }
        public WindowInsets build() { return CONSUMED; }
    }
}
