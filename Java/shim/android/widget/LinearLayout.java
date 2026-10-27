package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;

public class LinearLayout extends ViewGroup {
    public LinearLayout(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public LinearLayout(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); jetInit(a); }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(int w, int h, float wt) { super(w, h); weight = wt; }
        public LayoutParams(ViewGroup.LayoutParams o) { super(o); }
        public LayoutParams(android.content.Context c, android.util.AttributeSet a) { super(c, a); }
    }

    public static final int HORIZONTAL = 0, VERTICAL = 1;
    protected int mOrientation = HORIZONTAL;
    public LinearLayout(Context c) { this(c, null); }
    public LinearLayout(Context c, AttributeSet a) { super(c, a); jetInit(a); }
    private void jetInit(AttributeSet a0) {
        XmlAttrs a = mJetAttrs != null ? mJetAttrs : (a0 instanceof XmlAttrs ? (XmlAttrs) a0 : null);
        if (a != null) { String v = a.get("orientation"); if (v != null) mOrientation = ResTable.intVal(v);
            String g = a.get("gravity"); if (g != null) try { mGravity = ResTable.intVal(g); } catch (Exception ignored) {} }
    }
    protected int mGravity = 0x800033; // START|TOP
    public void setGravity(int g) { if ((g & 0x800007) == 0) g |= 0x800003; if ((g & 0x70) == 0) g |= 0x30; if (mGravity != g) { mGravity = g; requestLayout(); } }
    public int getGravity() { return mGravity; }
    public void setHorizontalGravity(int g) { setGravity((mGravity & ~0x800007) | (g & 0x800007)); }
    public void setVerticalGravity(int g) { setGravity((mGravity & ~0x70) | (g & 0x70)); }
    public void setOrientation(int o) { mOrientation = o; requestLayout(); }
    public int getOrientation() { return mOrientation; }

    @Override protected void onMeasure(int ws, int hs) {
        boolean vert = mOrientation == VERTICAL;
        int mainSpec = vert ? hs : ws, crossSpec = vert ? ws : hs;
        int padMain = vert ? mPaddingTop + mPaddingBottom : mPaddingLeft + mPaddingRight;
        int padCross = vert ? mPaddingLeft + mPaddingRight : mPaddingTop + mPaddingBottom;
        float totalWeight = 0; int used = 0, maxCross = 0;
        for (View c : mChildren) {
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            int mMain = vert ? lp.topMargin + lp.bottomMargin : lp.leftMargin + lp.rightMargin;
            int mCross = vert ? lp.leftMargin + lp.rightMargin : lp.topMargin + lp.bottomMargin;
            int dimMain = vert ? lp.height : lp.width, dimCross = vert ? lp.width : lp.height;
            used += mMain;
            if (lp.weight > 0) { totalWeight += lp.weight; if (dimMain == 0 || dimMain == -1) continue; } // 0 or MATCH_PARENT+weight: share remaining
            int cs = getChildMeasureSpec(crossSpec, padCross + mCross, dimCross);
            int msp = getChildMeasureSpec(mainSpec, padMain + (totalWeight == 0 ? used : 0), dimMain);
            if (vert) c.measure(cs, msp); else c.measure(msp, cs);
            used += vert ? c.getMeasuredHeight() : c.getMeasuredWidth();
            maxCross = Math.max(maxCross, (vert ? c.getMeasuredWidth() : c.getMeasuredHeight()) + mCross);
        }
        int mainSize = MeasureSpec.getSize(mainSpec), mainMode = MeasureSpec.getMode(mainSpec);
        if (totalWeight > 0 && mainMode != MeasureSpec.UNSPECIFIED) {
            int remaining = mainSize - padMain - used;
            for (View c : mChildren) {
                ViewGroup.LayoutParams lp = c.getLayoutParams();
                if (lp.weight <= 0) continue;
                int dimMain = vert ? lp.height : lp.width;
                int base = (dimMain == 0 || dimMain == -1) ? 0 : (vert ? c.getMeasuredHeight() : c.getMeasuredWidth());
                int share = (int) (remaining * lp.weight / totalWeight);
                int sz = Math.max(0, base + share);
                int mCross = vert ? lp.leftMargin + lp.rightMargin : lp.topMargin + lp.bottomMargin;
                int dimCross = vert ? lp.width : lp.height;
                int cs = getChildMeasureSpec(crossSpec, padCross + mCross, dimCross);
                int msp = MeasureSpec.makeMeasureSpec(sz, MeasureSpec.EXACTLY);
                if (vert) c.measure(cs, msp); else c.measure(msp, cs);
                maxCross = Math.max(maxCross, (vert ? c.getMeasuredWidth() : c.getMeasuredHeight()) + mCross);
            }
            used = mainSize - padMain;
        }
        int mw = resolveSize((vert ? maxCross + padCross : used + padMain), ws);
        int mh = resolveSize((vert ? used + padMain : maxCross + padCross), hs);
        setMeasuredDimension(mw, mh);
    }

    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        boolean vert = mOrientation == VERTICAL;
        int pos = vert ? mPaddingTop : mPaddingLeft;
        int crossAvail = vert ? getWidth() - mPaddingLeft - mPaddingRight : getHeight() - mPaddingTop - mPaddingBottom;
        { // AOSP layoutVertical/Horizontal: android:gravity shifts the whole child block along the main axis.
            int total = 0;
            for (View c : mChildren) { if (c.getVisibility() == View.GONE) continue; ViewGroup.LayoutParams lp = c.getLayoutParams();
                total += vert ? lp.topMargin + c.getMeasuredHeight() + lp.bottomMargin : lp.leftMargin + c.getMeasuredWidth() + lp.rightMargin; }
            int mainAvail = vert ? getHeight() - mPaddingTop - mPaddingBottom : getWidth() - mPaddingLeft - mPaddingRight;
            int mg = vert ? mGravity & 0x70 : mGravity & 7;
            if (vert ? mg == 0x10 : mg == 1) pos += (mainAvail - total) / 2;
            else if (vert ? mg == 0x50 : mg == 5) pos += mainAvail - total;
        }
        for (View c : mChildren) {
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
            if (vert) {
                pos += lp.topMargin;
                int x = mPaddingLeft + lp.leftMargin;
                int g = (lp.gravity < 0 ? mGravity : lp.gravity) & 7;
                if (g == 1) x = mPaddingLeft + (crossAvail - w) / 2; else if (g == 5) x = getWidth() - mPaddingRight - lp.rightMargin - w;
                c.layout(x, pos, x + w, pos + h);
                pos += h + lp.bottomMargin;
            } else {
                pos += lp.leftMargin;
                int y = mPaddingTop + lp.topMargin;
                int g = (lp.gravity < 0 ? mGravity : lp.gravity) & 0x70;
                if (g == 0x10) y = mPaddingTop + (crossAvail - h) / 2; else if (g == 0x50) y = getHeight() - mPaddingBottom - lp.bottomMargin - h;
                c.layout(pos, y, pos + w, y + h);
                pos += w + lp.rightMargin;
            }
        }
    }
    @Override public LayoutParams generateLayoutParams(android.util.AttributeSet a) { return new LayoutParams(getContext(), a); }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-2, -2); }
    @Override protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) { return new LayoutParams(p); }
    @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof LayoutParams; }
}
