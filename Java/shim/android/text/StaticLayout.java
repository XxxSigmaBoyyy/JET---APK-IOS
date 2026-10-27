package android.text;

import android.graphics.Paint;
import java.util.ArrayList;

/** JET StaticLayout: greedy word wrap (char break for overlong words), AOSP line metrics (includePad uses top/bottom on the
 *  first/last line; spacing mult/add applied to all but the last line), maxLines with END/START/MIDDLE ellipsis. LTR, no hyphenation. */
public class StaticLayout extends Layout {
    private int[] mStarts, mTops, mDescents, mEllStart, mEllCount; private int mLines, mTopPad, mBottomPad, mEllWidth;
    public StaticLayout(CharSequence source, TextPaint p, int w, Alignment a, float spacingMult, float spacingAdd, boolean includePad) {
        this(source, 0, source.length(), p, w, a, spacingMult, spacingAdd, includePad, null, w, -1);
    }
    public StaticLayout(CharSequence source, int s, int e, TextPaint p, int w, Alignment a, float spacingMult, float spacingAdd, boolean includePad) {
        this(source, s, e, p, w, a, spacingMult, spacingAdd, includePad, null, w, -1);
    }
    public StaticLayout(CharSequence source, int s, int e, TextPaint p, int w, Alignment a, float spacingMult, float spacingAdd, boolean includePad, TextUtils.TruncateAt ell, int ellW) {
        this(source, s, e, p, w, a, spacingMult, spacingAdd, includePad, ell, ellW, -1);
    }
    private StaticLayout(CharSequence source, int s, int e, TextPaint p, int w, Alignment a, float m, float ad, boolean pad, TextUtils.TruncateAt ell, int ellW, int maxLines) {
        super(s == 0 && e == source.length() ? source : source.subSequence(s, e), p, w, a, m, ad);
        mEllWidth = ellW;
        String t = mText.toString(); int n = t.length();
        ArrayList<Integer> ls = new ArrayList<>();
        int i = 0; ls.add(0);
        boolean truncated = false;
        outer:
        while (i <= n) {
            int nl = t.indexOf('\n', i); int end = nl < 0 ? n : nl;
            int j = i;
            while (j < end) {
                int k = j, lastBreak = -1;
                while (k < end && (k == j || measureRaw(p, t, j, k + 1) <= w)) { char ch = t.charAt(k); if (ch == ' ' || ch == '\t' || ch == '-') lastBreak = k; k++; }
                if (k < end) { if (lastBreak >= j) k = lastBreak + 1; while (k < end && t.charAt(k) == ' ') k++; }
                j = k;
                if (j < end) {
                    if (maxLines > 0 && ls.size() >= maxLines) { truncated = true; break outer; }
                    ls.add(j);
                }
            }
            if (nl < 0) break;
            i = nl + 1;
            if (maxLines > 0 && ls.size() >= maxLines) { truncated = true; break; }
            ls.add(i);
        }
        if (maxLines > 0 && ls.size() > maxLines) { while (ls.size() > maxLines) ls.remove(ls.size() - 1); truncated = true; }
        mLines = Math.max(1, ls.size());
        mStarts = new int[mLines + 1]; for (int q = 0; q < mLines; q++) mStarts[q] = ls.get(q); mStarts[mLines] = n;
        mEllStart = new int[mLines]; mEllCount = new int[mLines];
        // line metrics
        Paint.FontMetricsInt fm = p == null ? new Paint.FontMetricsInt() : p.getFontMetricsInt();
        mTops = new int[mLines + 1]; mDescents = new int[mLines];
        int y = 0;
        for (int l = 0; l < mLines; l++) {
            int above = fm.ascent, below = fm.descent;
            if (pad && l == 0) { above = fm.top; mTopPad = fm.top - fm.ascent; }
            if (pad && l == mLines - 1) { below = fm.bottom; mBottomPad = fm.bottom - fm.descent; }
            int extra = l == mLines - 1 ? 0 : (int) Math.round((below - above) * (m - 1f) + ad);
            mTops[l] = y; y += below - above + extra; mDescents[l] = below + extra;
        }
        mTops[mLines] = y;
        // ellipsis (on the last line when truncated, or on a single line that overflows)
        if (ell != null && ell != TextUtils.TruncateAt.MARQUEE && p != null) {
            int l = mLines - 1, ls0 = mStarts[l], le = n; while (le > ls0 && (t.charAt(le - 1) == '\n')) le--;
            float avail = ellW > 0 ? ellW : w;
            boolean need = truncated || measureRaw(p, t, ls0, le) > avail;
            if (need && (mLines == 1 || truncated || ell == TextUtils.TruncateAt.END)) {
                float ew = p.measureText("\u2026"), room = Math.max(0, avail - ew);
                int keep = 0; while (ls0 + keep < le && measureRaw(p, t, ls0, ls0 + keep + 1) <= room) keep++;
                if (ell == TextUtils.TruncateAt.START && mLines == 1) { mEllStart[l] = 0; mEllCount[l] = Math.max(0, (le - ls0) - keep); }
                else { mEllStart[l] = keep; mEllCount[l] = Math.max(0, (n - ls0) - keep); }
                if (mEllCount[l] == 0 && truncated) mEllCount[l] = 1;
            }
        }
    }
    private static float measureRaw(Paint p, String t, int s, int e) { return p == null ? 0 : p.measureText(t, s, e); }
    @Override public int getLineCount() { return mLines; }
    @Override public int getLineStart(int l) { return l >= mLines ? mStarts[mLines] : mStarts[Math.max(0, l)]; }
    @Override public int getLineEnd(int l) { return l + 1 >= mLines ? mStarts[mLines] : mStarts[l + 1]; }
    @Override public int getLineTop(int l) { return mTops[Math.max(0, Math.min(l, mLines))]; }
    @Override public int getLineDescent(int l) { return mDescents[Math.max(0, Math.min(l, mLines - 1))]; }
    @Override public int getTopPadding() { return mTopPad; }
    @Override public int getBottomPadding() { return mBottomPad; }
    @Override public int getEllipsisStart(int l) { return l < mLines ? mEllStart[l] : 0; }
    @Override public int getEllipsisCount(int l) { return l < mLines ? mEllCount[l] : 0; }
    @Override public int getEllipsizedWidth() { return mEllWidth; }

    public static final class Builder {
        private final CharSequence src; private final int s, e; private final TextPaint p; private final int w;
        private Alignment a = Alignment.ALIGN_NORMAL; private float m = 1f, ad = 0f; private boolean pad = true; private int max = -1;
        private TextUtils.TruncateAt ell; private int ellW;
        private Builder(CharSequence src, int s, int e, TextPaint p, int w) { this.src = src; this.s = s; this.e = e; this.p = p; this.w = w; this.ellW = w; }
        public static Builder obtain(CharSequence src, int s, int e, TextPaint p, int w) { return new Builder(src, s, e, p, w); }
        public Builder setText(CharSequence t) { return new Builder(t, 0, t.length(), p, w).copy(this); }
        private Builder copy(Builder o) { a = o.a; m = o.m; ad = o.ad; pad = o.pad; max = o.max; ell = o.ell; ellW = o.ellW; return this; }
        public Builder setAlignment(Alignment x) { a = x; return this; }
        public Builder setLineSpacing(float add, float mult) { ad = add; m = mult; return this; }
        public Builder setIncludePad(boolean b) { pad = b; return this; }
        public Builder setMaxLines(int n) { max = n; return this; }
        public Builder setEllipsize(TextUtils.TruncateAt t) { ell = t; return this; }
        public Builder setEllipsizedWidth(int x) { ellW = x; return this; }
        public Builder setBreakStrategy(int b) { return this; }
        public Builder setHyphenationFrequency(int h) { return this; }
        public Builder setJustificationMode(int j) { return this; }
        public Builder setUseLineSpacingFromFallbacks(boolean b) { return this; }
        public Builder setIndents(int[] l, int[] r) { return this; }
        private TextDirectionHeuristic jetDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        public Builder setTextDirection(TextDirectionHeuristic d) { jetDir = d; return this; }
        public Builder setLineBreakConfig(android.graphics.text.LineBreakConfig c) { return this; }
        public Builder setUseBoundsForWidth(boolean b) { return this; }
        public Builder setShiftDrawingOffsetForStartOverhang(boolean b) { return this; }
        public Builder setMinimumFontMetrics(Paint.FontMetrics fm) { return this; }
        public StaticLayout build() { return new StaticLayout(src, s, e, p, w, a, m, ad, pad, ell, ellW, max); }
    }
}
