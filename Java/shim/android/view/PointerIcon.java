package android.view;
public final class PointerIcon {
    public static final int TYPE_NULL = 0, TYPE_ARROW = 1000, TYPE_HAND = 1002, TYPE_TEXT = 1008, TYPE_DEFAULT = TYPE_ARROW;
    private final int type; private PointerIcon(int t) { type = t; }
    public static PointerIcon getSystemIcon(android.content.Context c, int t) { return new PointerIcon(t); }
    public static PointerIcon create(android.graphics.Bitmap b, float x, float y) { return new PointerIcon(-1); }
    public static PointerIcon load(android.content.res.Resources r, int id) { return new PointerIcon(-1); }
}
