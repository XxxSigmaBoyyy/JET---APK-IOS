package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;
public class FrameLayout extends ViewGroup {
    public FrameLayout(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public FrameLayout(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(int w, int h, int g) { super(w, h); gravity = g; }
        public LayoutParams(ViewGroup.LayoutParams o) { super(o); }
        public LayoutParams(android.content.Context c, android.util.AttributeSet a) { super(c, a); }
    }

    public FrameLayout(Context c) { this(c, null); }
    public FrameLayout(Context c, AttributeSet a) { super(c, a); }
    @Override protected void onMeasure(int ws, int hs) {
        int maxW = 0, maxH = 0;
        for (View c : mChildren) {
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            measureChildWithMargins(c, ws, hs, lp);
            maxW = Math.max(maxW, c.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxH = Math.max(maxH, c.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
        }
        setMeasuredDimension(
                resolveSize(maxW + mPaddingLeft + mPaddingRight, ws),
                resolveSize(maxH + mPaddingTop + mPaddingBottom, hs));
    }
    protected void measureChildWithMargins(View c, int ws, int hs, ViewGroup.LayoutParams lp) {
        int w = getChildMeasureSpec(ws, mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin, lp.width);
        int h = getChildMeasureSpec(hs, mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
        c.measure(w, h);
    }
    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        for (View c : mChildren) {
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
            int x = mPaddingLeft + lp.leftMargin, y = mPaddingTop + lp.topMargin;
            int g = lp.gravity < 0 ? 0x33 : lp.gravity;
            int availW = getWidth() - mPaddingLeft - mPaddingRight - lp.leftMargin - lp.rightMargin;
            int availH = getHeight() - mPaddingTop - mPaddingBottom - lp.topMargin - lp.bottomMargin;
            if ((g & 7) == 1) x = mPaddingLeft + lp.leftMargin + (availW - w) / 2;
            else if ((g & 7) == 5) x = getWidth() - mPaddingRight - lp.rightMargin - w;
            if ((g & 0x70) == 0x10) y = mPaddingTop + lp.topMargin + (availH - h) / 2;
            else if ((g & 0x70) == 0x50) y = getHeight() - mPaddingBottom - lp.bottomMargin - h;
            c.layout(x, y, x + w, y + h);
        }
    }
    @Override public LayoutParams generateLayoutParams(android.util.AttributeSet a) { return new LayoutParams(getContext(), a); }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1, -1); }
    @Override protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) { return new LayoutParams(p); }
    @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof LayoutParams; }
}
