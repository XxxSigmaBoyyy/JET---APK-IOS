package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import jet.runtime.XmlAttrs;
import java.util.*;

/** Column-aligned layout: natural column widths, then stretch/shrink like Android. */
public class TableLayout extends LinearLayout {
    public TableLayout(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public TableLayout(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }

    private final Set<Integer> stretch = new HashSet<>();
    private boolean stretchAll, shrinkAll;
    private int[] colW = new int[0];
    public TableLayout(Context c) { this(c, null); }
    public TableLayout(Context c, AttributeSet a) {
        super(c, a);
        mOrientation = VERTICAL;
        if (a instanceof XmlAttrs) {
            XmlAttrs x = (XmlAttrs) a;
            String s = x.get("stretchColumns");
            if (s != null) for (String p : s.split(",")) { p = p.trim(); if (p.equals("*")) stretchAll = true; else if (!p.isEmpty()) stretch.add(Integer.parseInt(p)); }
            String k = x.get("shrinkColumns");
            shrinkAll = k != null && k.trim().equals("*");
        }
    }
    @Override protected void onMeasure(int ws, int hs) {
        int avail = MeasureSpec.getSize(ws) - mPaddingLeft - mPaddingRight;
        int unspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int ncol = 0;
        for (View r : mChildren) if (r instanceof TableRow) ncol = Math.max(ncol, ((TableRow) r).getChildCount());
        colW = new int[ncol];
        for (View r : mChildren) if (r instanceof TableRow) {
            TableRow row = (TableRow) r;
            for (int j = 0; j < row.getChildCount(); j++) {
                View c = row.getChildAt(j); android.view.ViewGroup.LayoutParams lp = c.getLayoutParams();
                c.measure(unspec, unspec);
                colW[j] = Math.max(colW[j], c.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            }
        }
        int total = 0; for (int w : colW) total += w;
        if (total < avail) {
            List<Integer> st = new ArrayList<>();
            for (int j = 0; j < ncol; j++) if (stretchAll || stretch.contains(j)) st.add(j);
            if (!st.isEmpty()) { int extra = (avail - total) / st.size(); for (int j : st) colW[j] += extra; }
        } else if (total > avail && shrinkAll && total > 0) {
            for (int j = 0; j < ncol; j++) colW[j] = colW[j] * avail / total;
        }
        int h = mPaddingTop + mPaddingBottom;
        for (View r : mChildren) {
            android.view.ViewGroup.LayoutParams rlp = r.getLayoutParams();
            if (r instanceof TableRow) {
                TableRow row = (TableRow) r; int rh = 0;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View c = row.getChildAt(j); android.view.ViewGroup.LayoutParams lp = c.getLayoutParams();
                    c.measure(MeasureSpec.makeMeasureSpec(Math.max(0, colW[j] - lp.leftMargin - lp.rightMargin), MeasureSpec.EXACTLY), unspec);
                    rh = Math.max(rh, c.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                }
                for (int j = 0; j < row.getChildCount(); j++) {   // equalise heights
                    View c = row.getChildAt(j); android.view.ViewGroup.LayoutParams lp = c.getLayoutParams();
                    c.measure(MeasureSpec.makeMeasureSpec(Math.max(0, colW[j] - lp.leftMargin - lp.rightMargin), MeasureSpec.EXACTLY),
                              MeasureSpec.makeMeasureSpec(rh - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY));
                }
                row.forceMeasured(avail, rh);
                h += rh + rlp.topMargin + rlp.bottomMargin;
            } else {
                r.measure(MeasureSpec.makeMeasureSpec(avail, MeasureSpec.EXACTLY), unspec);
                h += r.getMeasuredHeight() + rlp.topMargin + rlp.bottomMargin;
            }
        }
        setMeasuredDimension(resolveSize(avail + mPaddingLeft + mPaddingRight, ws), resolveSize(h, hs));
    }
}
