package android.view;
public abstract class ViewOutlineProvider {
    public static final ViewOutlineProvider BACKGROUND = new ViewOutlineProvider() { public void getOutline(View v, android.graphics.Outline o) { o.setRect(0, 0, v.getWidth(), v.getHeight()); } };
    public static final ViewOutlineProvider BOUNDS = BACKGROUND, PADDED_BOUNDS = BACKGROUND;
    public abstract void getOutline(View view, android.graphics.Outline outline);
}
