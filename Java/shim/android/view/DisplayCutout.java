package android.view;
import android.graphics.Rect;
import java.util.Collections;
import java.util.List;
public final class DisplayCutout {
    private final Rect safe; private final List<Rect> bounds;
    public DisplayCutout(android.graphics.Insets safeInsets, Rect l, Rect t, Rect r, Rect b) { safe = new Rect(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom); bounds = Collections.emptyList(); }
    public DisplayCutout(Rect safeInsets, List<Rect> boundingRects) { safe = new Rect(safeInsets); bounds = boundingRects == null ? Collections.<Rect>emptyList() : boundingRects; }
    public int getSafeInsetTop() { return safe.top; } public int getSafeInsetBottom() { return safe.bottom; }
    public int getSafeInsetLeft() { return safe.left; } public int getSafeInsetRight() { return safe.right; }
    public List<Rect> getBoundingRects() { return bounds; }
    public Rect getBoundingRectTop() { return new Rect(); } public Rect getBoundingRectBottom() { return new Rect(); }
    public Rect getBoundingRectLeft() { return new Rect(); } public Rect getBoundingRectRight() { return new Rect(); }
    public android.graphics.Insets getWaterfallInsets() { return android.graphics.Insets.NONE; }
}
