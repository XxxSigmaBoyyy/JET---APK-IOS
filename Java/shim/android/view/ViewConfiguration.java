package android.view;
public class ViewConfiguration {
    private final float d;
    private ViewConfiguration(float density) { d = density; }
    public static ViewConfiguration get(android.content.Context c) { return new ViewConfiguration(c == null ? 2f : c.getResources().getDisplayMetrics().density); }
    @Deprecated public ViewConfiguration() { d = 1f; }
    private int dp(int v) { return Math.round(v * d); }
    public static int getLongPressTimeout() { return 400; } public static int getTapTimeout() { return 100; } public static int getDoubleTapTimeout() { return 300; }
    public static int getJumpTapTimeout() { return 500; } public static int getPressedStateDuration() { return 64; } public static int getKeyRepeatTimeout() { return 400; }
    public static int getKeyRepeatDelay() { return 50; } public static int getScrollBarFadeDuration() { return 250; } public static int getScrollDefaultDelay() { return 300; }
    public static long getZoomControlsTimeout() { return 3000; } public static long getGlobalActionKeyTimeout() { return 500; } public static int getWindowTouchSlop() { return 16; }
    public static int getMultiPressTimeout() { return 300; } public static float getScrollFriction() { return 0.015f; } public static int getEdgeSlop() { return 12; }
    public static int getTouchSlop() { return 8; } public static int getMinimumFlingVelocity() { return 50; } public static int getMaximumFlingVelocity() { return 8000; }
    public static int getScrollBarSize() { return 4; } public static int getFadingEdgeLength() { return 12; } public static long getDefaultActionModeHideDuration() { return 2000; }
    public int getScaledTouchSlop() { return dp(8); } public int getScaledPagingTouchSlop() { return dp(16); } public int getScaledDoubleTapSlop() { return dp(100); }
    public int getScaledWindowTouchSlop() { return dp(16); } public int getScaledMinimumFlingVelocity() { return dp(50); } public int getScaledMaximumFlingVelocity() { return dp(8000); }
    public int getScaledScrollBarSize() { return dp(4); } public int getScaledFadingEdgeLength() { return dp(12); } public int getScaledEdgeSlop() { return dp(12); }
    public int getScaledOverscrollDistance() { return 0; } public int getScaledOverflingDistance() { return dp(6); } public int getScaledHoverSlop() { return dp(4); }
    public int getScaledMinimumScalingSpan() { return dp(170); } public float getScaledHorizontalScrollFactor() { return dp(64); } public float getScaledVerticalScrollFactor() { return dp(64); }
    public int getScaledDoubleTapTouchSlop() { return dp(8); } public int getScaledMaximumDrawingCacheSize() { return 4 * 1080 * 1920; }
    public boolean hasPermanentMenuKey() { return false; } public boolean shouldShowMenuShortcutsWhenKeyboardPresent() { return false; }
    public static int getHoverTooltipHideTimeout() { return 15000; } public static int getHoverTooltipShowTimeout() { return 500; }
    public int getScaledMinScrollbarTouchTarget() { return dp(48); } public int getScaledAmbiguousGestureMultiplier() { return 2; }
}
