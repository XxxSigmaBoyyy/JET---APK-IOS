package android.app;
public abstract class ActionBar {
    public static final int DISPLAY_HOME_AS_UP = 4, DISPLAY_SHOW_TITLE = 8, DISPLAY_SHOW_HOME = 2, DISPLAY_USE_LOGO = 1, DISPLAY_SHOW_CUSTOM = 16;
    public static final int NAVIGATION_MODE_STANDARD = 0;
    public abstract void setTitle(CharSequence t);
    public abstract void setDisplayHomeAsUpEnabled(boolean e);
    public abstract void hide();
    public abstract void show();
    public abstract boolean isShowing();
    public static class LayoutParams extends android.view.ViewGroup.MarginLayoutParams {
        public int gravity;
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(android.view.ViewGroup.LayoutParams o) { super(o); }
    }
}
