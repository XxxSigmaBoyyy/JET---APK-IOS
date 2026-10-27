package android.view;
/** JET: API 30 WindowInsetsController. System bars are modelled as state only (no system UI is drawn). */
public interface WindowInsetsController {
    int APPEARANCE_OPAQUE_STATUS_BARS = 1, APPEARANCE_OPAQUE_NAVIGATION_BARS = 2, APPEARANCE_LOW_PROFILE_BARS = 4,
        APPEARANCE_LIGHT_STATUS_BARS = 8, APPEARANCE_LIGHT_NAVIGATION_BARS = 16, APPEARANCE_SEMI_TRANSPARENT_STATUS_BARS = 32,
        APPEARANCE_SEMI_TRANSPARENT_NAVIGATION_BARS = 64, APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND = 128, APPEARANCE_LIGHT_CAPTION_BARS = 256;
    int BEHAVIOR_SHOW_BARS_BY_TOUCH = 0, BEHAVIOR_DEFAULT = 1, BEHAVIOR_SHOW_BARS_BY_SWIPE = 1, BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;
    void hide(int types); void show(int types);
    void setSystemBarsBehavior(int b); int getSystemBarsBehavior();
    void setSystemBarsAppearance(int a, int mask); int getSystemBarsAppearance();
    default void setSystemBarsColor(int a, int b) {}
    default void addOnControllableInsetsChangedListener(OnControllableInsetsChangedListener l) { l.onControllableInsetsChanged(this, 0); }
    default void removeOnControllableInsetsChangedListener(OnControllableInsetsChangedListener l) {}
    interface OnControllableInsetsChangedListener { void onControllableInsetsChanged(WindowInsetsController c, int typeMask); }
    final class Jet implements WindowInsetsController {
        private int behavior = BEHAVIOR_DEFAULT, appearance, hidden;
        public void hide(int t) { hidden |= t; } public void show(int t) { hidden &= ~t; }
        public int jetHiddenTypes() { return hidden; }
        public void setSystemBarsBehavior(int b) { behavior = b; } public int getSystemBarsBehavior() { return behavior; }
        public void setSystemBarsAppearance(int a, int mask) { appearance = (appearance & ~mask) | (a & mask); } public int getSystemBarsAppearance() { return appearance; }
    }
}
