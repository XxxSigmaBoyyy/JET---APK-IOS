package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import static android.view.View.MeasureSpec.*;

/** RelativeLayout with Android's rule model (LTR): parent/sibling alignment, above/below/leftOf/rightOf, centering. */
public class RelativeLayout extends ViewGroup {
    public static final int TRUE = -1, LEFT_OF = 0, RIGHT_OF = 1, ABOVE = 2, BELOW = 3, ALIGN_BASELINE = 4, ALIGN_LEFT = 5, ALIGN_TOP = 6, ALIGN_RIGHT = 7, ALIGN_BOTTOM = 8,
        ALIGN_PARENT_LEFT = 9, ALIGN_PARENT_TOP = 10, ALIGN_PARENT_RIGHT = 11, ALIGN_PARENT_BOTTOM = 12, CENTER_IN_PARENT = 13, CENTER_HORIZONTAL = 14, CENTER_VERTICAL = 15,
        START_OF = 16, END_OF = 17, ALIGN_START = 18, ALIGN_END = 19, ALIGN_PARENT_START = 20, ALIGN_PARENT_END = 21;
    private static final String[] RULE_ATTR = {"layout_toLeftOf", "layout_toRightOf", "layout_above", "layout_below", "layout_alignBaseline", "layout_alignLeft", "layout_alignTop",
        "layout_alignRight", "layout_alignBottom", "layout_alignParentLeft", "layout_alignParentTop", "layout_alignParentRight", "layout_alignParentBottom", "layout_centerInParent",
        "layout_centerHorizontal", "layout_centerVertical", "layout_toStartOf", "layout_toEndOf", "layout_alignStart", "layout_alignEnd", "layout_alignParentStart", "layout_alignParentEnd"};
    private int mGravity = 0x33; private int mIgnoreGravity;
    public RelativeLayout(Context c) { this(c, null); }
    public RelativeLayout(Context c, AttributeSet a) { super(c, a); jet.runtime.XmlAttrs x = jet.runtime.XmlAttrs.of(a); if (x != null && x.get("gravity") != null) try { mGravity = jet.runtime.ResTable.intVal(getResources().table().resolve(x.get("gravity"))); } catch (RuntimeException ignored) {} }
    public RelativeLayout(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public RelativeLayout(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }
    public void setGravity(int g) { mGravity = g; requestLayout(); } public int getGravity() { return mGravity; }
    public void setHorizontalGravity(int g) { mGravity = (mGravity & ~7) | (g & 7); } public void setVerticalGravity(int g) { mGravity = (mGravity & ~0x70) | (g & 0x70); } public void setIgnoreGravity(int id) { mIgnoreGravity = id; }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private final int[] rules = new int[22];
        public boolean alignWithParent; int mL, mT, mR, mB;
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(ViewGroup.LayoutParams o) { super(o); if (o instanceof LayoutParams) System.arraycopy(((LayoutParams) o).rules, 0, rules, 0, 22); }
        public LayoutParams(ViewGroup.MarginLayoutParams o) { this((ViewGroup.LayoutParams) o); }
        public LayoutParams(LayoutParams o) { this((ViewGroup.LayoutParams) o); alignWithParent = o.alignWithParent; }
        public LayoutParams(Context c, AttributeSet a) {
            super(c, a);
            jet.runtime.XmlAttrs x = jet.runtime.XmlAttrs.of(a);
            if (x != null && c != null) {
                jet.runtime.ResTable t = c.getResources().table();
                for (int i = 0; i < RULE_ATTR.length; i++) {
                    String v = x.get(RULE_ATTR[i]); if (v == null) continue;
                    int id = jet.runtime.ResTable.refId(v);
                    boolean isId = id != 0 && (t.fullName(id).contains(":id/") || t.name(id) == null);
                    String r = isId ? v : t.resolve(v);
                    if ("true".equals(r) || "-1".equals(r)) rules[i] = TRUE; else if (isId) rules[i] = id;
                }
                String aw = x.get("layout_alignWithParentIfMissing"); alignWithParent = "true".equals(aw);
            }
        }
        public void addRule(int verb) { addRule(verb, TRUE); }
        public void addRule(int verb, int subject) { if (verb >= 0 && verb < rules.length) rules[verb] = subject; }
        public void removeRule(int verb) { addRule(verb, 0); }
        public int getRule(int verb) { return rules[verb]; }
        public int[] getRules() { return rules; } public int[] getRules(int dir) { return rules; }
        int r(int i) { return rules[i]; }
        /** LTR resolution of start/end rules onto left/right. */
        void resolve() {
            if (rules[ALIGN_PARENT_START] != 0) rules[ALIGN_PARENT_LEFT] = rules[ALIGN_PARENT_START];
            if (rules[ALIGN_PARENT_END] != 0) rules[ALIGN_PARENT_RIGHT] = rules[ALIGN_PARENT_END];
            if (rules[START_OF] != 0) rules[LEFT_OF] = rules[START_OF]; if (rules[END_OF] != 0) rules[RIGHT_OF] = rules[END_OF];
            if (rules[ALIGN_START] != 0) rules[ALIGN_LEFT] = rules[ALIGN_START]; if (rules[ALIGN_END] != 0) rules[ALIGN_RIGHT] = rules[ALIGN_END];
        }
    }
    @Override public LayoutParams generateLayoutParams(AttributeSet a) { return new LayoutParams(getContext(), a); }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-2, -2); }
    @Override protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) { return new LayoutParams(p); }
    @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof LayoutParams; }

    private static final int UNSET = Integer.MIN_VALUE;
    private View sib(int id) { if (id == 0 || id == TRUE) return null; for (int i = 0; i < getChildCount(); i++) { View c = getChildAt(i); if (c.getId() == id && c.getVisibility() != GONE) return c; } return null; }
    private LayoutParams lp(View v) { ViewGroup.LayoutParams p = v.getLayoutParams(); if (!(p instanceof LayoutParams)) { p = new LayoutParams(p); v.setLayoutParams(p); } return (LayoutParams) p; }

    private int[] spec(int start, int end, int childSize, int startMargin, int endMargin, int padStart, int padEnd, int mySize) {
        int cStart = start, cEnd = end;
        if (cStart == UNSET) cStart = padStart + startMargin;
        if (cEnd == UNSET) cEnd = mySize - padEnd - endMargin;
        int max = Math.max(0, cEnd - cStart);
        if (start != UNSET && end != UNSET) return new int[]{childSize >= 0 ? makeMeasureSpec(childSize, EXACTLY) : makeMeasureSpec(max, childSize == -1 ? EXACTLY : AT_MOST)};
        if (childSize >= 0) return new int[]{makeMeasureSpec(childSize, EXACTLY)};
        if (childSize == -1) return new int[]{makeMeasureSpec(max, EXACTLY)};
        return new int[]{makeMeasureSpec(max, mySize < 0 ? UNSPECIFIED : AT_MOST)};
    }

    @Override protected void onMeasure(int ws, int hs) {
        int W = getSize(ws), H = getSize(hs); boolean exW = getMode(ws) == EXACTLY, exH = getMode(hs) == EXACTLY;
        // AOSP: UNSPECIFIED -> myWidth/myHeight = -1, so parent-end/center rules are not applied until the wrap size is known.
        final boolean unW = getMode(ws) == UNSPECIFIED, unH = getMode(hs) == UNSPECIFIED;
        if (unW) W = 1 << 20; if (unH) H = 1 << 20;
        int n = getChildCount(); boolean[] doneH = new boolean[n], doneV = new boolean[n];
        for (int i = 0; i < n; i++) lp(getChildAt(i)).resolve();
        int maxR = 0, maxB = 0;
        // Horizontal pass, dependency-ordered by repeated sweeps.
        for (int pass = 0, left = n; pass <= n && left > 0; pass++) for (int i = 0; i < n; i++) {
            View c = getChildAt(i); if (doneH[i]) continue; if (c.getVisibility() == GONE) { doneH[i] = doneV[i] = true; left--; continue; }
            LayoutParams p = lp(c);
            if (pass < n && (!ready(p.r(LEFT_OF), doneH) || !ready(p.r(RIGHT_OF), doneH) || !ready(p.r(ALIGN_LEFT), doneH) || !ready(p.r(ALIGN_RIGHT), doneH))) continue;
            int L = UNSET, R = UNSET; View a;
            if ((a = sib(p.r(LEFT_OF))) != null) R = lp(a).mL - lp(a).leftMargin - p.rightMargin; else if (p.alignWithParent && p.r(LEFT_OF) != 0) R = W - mPaddingRight - p.rightMargin;
            if ((a = sib(p.r(RIGHT_OF))) != null) L = lp(a).mR + lp(a).rightMargin + p.leftMargin; else if (p.alignWithParent && p.r(RIGHT_OF) != 0) L = mPaddingLeft + p.leftMargin;
            if ((a = sib(p.r(ALIGN_LEFT))) != null) L = lp(a).mL + p.leftMargin; if ((a = sib(p.r(ALIGN_RIGHT))) != null) R = lp(a).mR - p.rightMargin;
            if (p.r(ALIGN_PARENT_LEFT) != 0) L = mPaddingLeft + p.leftMargin; if (p.r(ALIGN_PARENT_RIGHT) != 0 && !unW) R = W - mPaddingRight - p.rightMargin;
            int wspec = spec(L, R, p.width, p.leftMargin, p.rightMargin, mPaddingLeft, mPaddingRight, W)[0];
            int hspec = spec(UNSET, UNSET, p.height, p.topMargin, p.bottomMargin, mPaddingTop, mPaddingBottom, H)[0];
            c.measure(wspec, hspec);
            int w = c.getMeasuredWidth();
            if (L == UNSET && R == UNSET) { if ((p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_HORIZONTAL) != 0) && !unW) { L = (W - w) / 2; } else if ((mGravity & 7) == 1 && exW) L = (W - w) / 2; else if ((mGravity & 7) == 5 && exW) L = W - mPaddingRight - p.rightMargin - w; else L = mPaddingLeft + p.leftMargin; R = L + w; }
            else if (L == UNSET) L = R - w; else if (R == UNSET) R = L + w; else if (p.width == -2) R = L + w;
            p.mL = L; p.mR = R; doneH[i] = true; left--; maxR = Math.max(maxR, R + p.rightMargin);
        }
        for (int pass = 0, left = n; pass <= n && left > 0; pass++) for (int i = 0; i < n; i++) {
            View c = getChildAt(i); if (doneV[i]) continue; if (c.getVisibility() == GONE) { doneV[i] = true; left--; continue; }
            LayoutParams p = lp(c);
            if (pass < n && (!ready(p.r(ABOVE), doneV) || !ready(p.r(BELOW), doneV) || !ready(p.r(ALIGN_TOP), doneV) || !ready(p.r(ALIGN_BOTTOM), doneV) || !ready(p.r(ALIGN_BASELINE), doneV))) continue;
            int T = UNSET, B = UNSET; View a;
            if ((a = sib(p.r(ABOVE))) != null) B = lp(a).mT - lp(a).topMargin - p.bottomMargin; else if (p.alignWithParent && p.r(ABOVE) != 0) B = H - mPaddingBottom - p.bottomMargin;
            if ((a = sib(p.r(BELOW))) != null) T = lp(a).mB + lp(a).bottomMargin + p.topMargin; else if (p.alignWithParent && p.r(BELOW) != 0) T = mPaddingTop + p.topMargin;
            if ((a = sib(p.r(ALIGN_TOP))) != null) T = lp(a).mT + p.topMargin; if ((a = sib(p.r(ALIGN_BOTTOM))) != null) B = lp(a).mB - p.bottomMargin;
            if ((a = sib(p.r(ALIGN_BASELINE))) != null) { T = lp(a).mT; }
            if (p.r(ALIGN_PARENT_TOP) != 0) T = mPaddingTop + p.topMargin; if (p.r(ALIGN_PARENT_BOTTOM) != 0 && !unH) B = H - mPaddingBottom - p.bottomMargin;
            int wspec = makeMeasureSpec(p.mR - p.mL, EXACTLY);
            int hspec = spec(T, B, p.height, p.topMargin, p.bottomMargin, mPaddingTop, mPaddingBottom, H)[0];
            if (p.height != -2 || T != UNSET && B != UNSET) c.measure(wspec, hspec); else if (c.getMeasuredHeight() > Math.max(0, getSize(hspec)) && getMode(hspec) == AT_MOST) c.measure(wspec, hspec); else c.measure(wspec, hspec);
            int h = c.getMeasuredHeight();
            if (T == UNSET && B == UNSET) { if ((p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_VERTICAL) != 0) && !unH) T = (H - h) / 2; else if ((mGravity & 0x70) == 0x10 && exH) T = (H - h) / 2; else if ((mGravity & 0x70) == 0x50 && exH) T = H - mPaddingBottom - p.bottomMargin - h; else T = mPaddingTop + p.topMargin; B = T + h; }
            else if (T == UNSET) T = B - h; else if (B == UNSET) B = T + h; else if (p.height == -2) B = T + h;
            p.mT = T; p.mB = B; doneV[i] = true; left--; maxB = Math.max(maxB, B + p.bottomMargin);
        }
        int fw = exW ? W : Math.min(getMode(ws) == AT_MOST ? W : Integer.MAX_VALUE, Math.max(maxR + mPaddingRight, getSuggestedMinimumWidth()));
        int fh = exH ? H : Math.min(getMode(hs) == AT_MOST ? H : Integer.MAX_VALUE, Math.max(maxB + mPaddingBottom, getSuggestedMinimumHeight()));
        // wrap_content parent: bottom/right-anchored children were placed against the provisional size; shift them to the final edge.
        if (unH) for (int i = 0; i < n; i++) { View c = getChildAt(i); if (c.getVisibility() == GONE) continue; LayoutParams p = lp(c); int h = p.mB - p.mT;
            if (p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_VERTICAL) != 0) { p.mT = (fh - h) / 2; p.mB = p.mT + h; }
            else if (p.r(ALIGN_PARENT_BOTTOM) != 0 && p.r(ALIGN_PARENT_TOP) == 0) { p.mT = fh - mPaddingBottom - h; p.mB = p.mT + h; } }
        if (unW) for (int i = 0; i < n; i++) { View c = getChildAt(i); if (c.getVisibility() == GONE) continue; LayoutParams p = lp(c); int w = p.mR - p.mL;
            if (p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_HORIZONTAL) != 0) { p.mL = (fw - w) / 2; p.mR = p.mL + w; }
            else if (p.r(ALIGN_PARENT_RIGHT) != 0 && p.r(ALIGN_PARENT_LEFT) == 0) { p.mL = fw - mPaddingRight - w; p.mR = p.mL + w; } }
        if (!exH && !unH && fh != H) for (int i = 0; i < n; i++) { View c = getChildAt(i); LayoutParams p = lp(c); if (p.r(ALIGN_PARENT_BOTTOM) != 0 && p.r(ALIGN_PARENT_TOP) == 0) { int d = H - fh; p.mT -= d; p.mB -= d; } else if (p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_VERTICAL) != 0) { int d = (H - fh) / 2; p.mT -= d; p.mB -= d; } }
        if (!exW && !unW && fw != W) for (int i = 0; i < n; i++) { View c = getChildAt(i); LayoutParams p = lp(c); if (p.r(ALIGN_PARENT_RIGHT) != 0 && p.r(ALIGN_PARENT_LEFT) == 0) { int d = W - fw; p.mL -= d; p.mR -= d; } else if (p.r(CENTER_IN_PARENT) != 0 || p.r(CENTER_HORIZONTAL) != 0) { int d = (W - fw) / 2; p.mL -= d; p.mR -= d; } }
        setMeasuredDimension(fw, fh);
    }
    private boolean ready(int id, boolean[] done) { if (id == 0 || id == TRUE) return true; for (int i = 0; i < getChildCount(); i++) if (getChildAt(i).getId() == id) return done[i] || getChildAt(i).getVisibility() == GONE; return true; }
    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) { View c = getChildAt(i); if (c.getVisibility() == GONE) continue; LayoutParams p = lp(c); c.layout(p.mL, p.mT, p.mL + c.getMeasuredWidth(), p.mT + c.getMeasuredHeight()); }
    }
    public int getBaseline() { return -1; }
}
