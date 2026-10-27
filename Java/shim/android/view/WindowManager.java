package android.view;
public interface WindowManager extends ViewManager {
    void addView(View view, android.view.ViewGroup.LayoutParams params);
    void updateViewLayout(View view, android.view.ViewGroup.LayoutParams params);
    void removeView(View view);
    Display getDefaultDisplay();
    // API 30+ window metrics
    default WindowMetrics getCurrentWindowMetrics() { return WindowMetrics.jetCurrent(); }
    default WindowMetrics getMaximumWindowMetrics() { return WindowMetrics.jetCurrent(); }
    class LayoutParams extends android.view.ViewGroup.LayoutParams {
        public static final int FLAG_FULLSCREEN = 0x00000400;
        public int flags;
        public int type;
        public int softInputMode, gravity, x, y, windowAnimations, format, layoutInDisplayCutoutMode, systemUiVisibility; public float alpha = 1f, dimAmount, screenBrightness = -1f, buttonBrightness = -1f, horizontalMargin, verticalMargin; public android.os.IBinder token; public String packageName; public CharSequence title;
        public static final int FLAG_KEEP_SCREEN_ON = 0x80, FLAG_NOT_FOCUSABLE = 8, FLAG_NOT_TOUCHABLE = 0x10, FLAG_DIM_BEHIND = 2, FLAG_SECURE = 0x2000, FLAG_LAYOUT_NO_LIMITS = 0x200, FLAG_TRANSLUCENT_STATUS = 0x4000000, FLAG_TRANSLUCENT_NAVIGATION = 0x8000000, FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000, FLAG_SHOW_WHEN_LOCKED = 0x80000, FLAG_TURN_SCREEN_ON = 0x200000, FLAG_LAYOUT_IN_SCREEN = 0x100, FLAG_HARDWARE_ACCELERATED = 0x1000000;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x10, SOFT_INPUT_ADJUST_PAN = 0x20, SOFT_INPUT_STATE_HIDDEN = 2, SOFT_INPUT_STATE_ALWAYS_HIDDEN = 3, SOFT_INPUT_STATE_VISIBLE = 4, LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1, LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0, TYPE_APPLICATION = 2, TYPE_APPLICATION_OVERLAY = 2038, MATCH_PARENT = -1, WRAP_CONTENT = -2;
        public void setTitle(CharSequence t) { title = t; } public CharSequence getTitle() { return title; }
        public LayoutParams() { super(0, 0); }
        public LayoutParams(int w, int h) { super(w, h); }
    }

    default void addCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> l) {}
    default void addCrossWindowBlurEnabledListener(java.util.concurrent.Executor e, java.util.function.Consumer<Boolean> l) {}
    default void removeCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> l) {}
    default boolean isCrossWindowBlurEnabled() { return false; }
}
