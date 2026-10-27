package android.text;

import android.graphics.Paint;

/** JET BoringLayout: single-line, LTR, no tabs/newlines/spans text, measured with the paint's jet.gfx.TextMetrics. */
public class BoringLayout extends Layout {
    public static class Metrics extends Paint.FontMetricsInt {
        public int width;
        public Metrics() {}
        @Override public String toString() { return "BoringLayout.Metrics{width=" + width + " ascent=" + ascent + " descent=" + descent + "}"; }
    }
    private int mBottom, mDesc, mTopPad, mBottomPad, mEllStart, mEllCount, mEllW; private float mMax;

    public static Metrics isBoring(CharSequence text, TextPaint paint) { return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, null); }
    public static Metrics isBoring(CharSequence text, TextPaint paint, Metrics m) { return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, m); }
    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic d, Metrics m) { return isBoring(text, paint, d, false, m); }
    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic d, boolean useFallbackLineSpacing, Metrics m) {
        if (text == null || paint == null) return null;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\t' || (c >= 0x0590 && c <= 0x08FF) || (c >= 0xFB1D && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF) || Character.isSurrogate(c) && Character.getDirectionality(Character.codePointAt(text, i)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT) return null;
        }
        if (m == null) m = new Metrics();
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        m.top = fm.top; m.ascent = fm.ascent; m.descent = fm.descent; m.bottom = fm.bottom; m.leading = fm.leading;
        m.width = (int) Math.ceil(paint.measureText(text, 0, n));
        return m;
    }
    public static BoringLayout make(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad) { return new BoringLayout(s, p, w, a, mult, add, m, pad); }
    public static BoringLayout make(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW) { return new BoringLayout(s, p, w, a, mult, add, m, pad, ell, ellW); }
    public static BoringLayout make(CharSequence s, TextPaint p, int w, Alignment a, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW, boolean fallback) { return new BoringLayout(s, p, w, a, 1f, 0f, m, pad, ell, ellW, fallback); }
    public BoringLayout replaceOrMake(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad) { return new BoringLayout(s, p, w, a, mult, add, m, pad); }
    public BoringLayout replaceOrMake(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW) { return new BoringLayout(s, p, w, a, mult, add, m, pad, ell, ellW); }
    public BoringLayout replaceOrMake(CharSequence s, TextPaint p, int w, Alignment a, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW, boolean fallback) { return new BoringLayout(s, p, w, a, 1f, 0f, m, pad, ell, ellW, fallback); }

    public BoringLayout(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad) { this(s, p, w, a, mult, add, m, pad, null, w); }
    public BoringLayout(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW) { this(s, p, w, a, mult, add, m, pad, ell, ellW, false); }
    public BoringLayout(CharSequence s, TextPaint p, int w, Alignment a, float mult, float add, Metrics m, boolean pad, TextUtils.TruncateAt ell, int ellW, boolean fallback) {
        super(s, p, w, a, mult, add);
        if (m == null) m = isBoring(s, p); if (m == null) { m = new Metrics(); Paint.FontMetricsInt fm = p.getFontMetricsInt(); m.top = fm.top; m.ascent = fm.ascent; m.descent = fm.descent; m.bottom = fm.bottom; m.width = (int) Math.ceil(p.measureText(s, 0, s.length())); }
        mEllW = ellW;
        int above = pad ? m.top : m.ascent, below = pad ? m.bottom : m.descent;
        mTopPad = pad ? m.top - m.ascent : 0; mBottomPad = pad ? m.bottom - m.descent : 0;
        mBottom = below - above; mDesc = below; mMax = m.width;
        if (ell != null && ell != TextUtils.TruncateAt.MARQUEE && m.width > ellW && p != null) {
            float room = Math.max(0, ellW - p.measureText("\u2026")); int n = s.length(), keep = 0;
            while (keep < n && p.measureText(s, 0, keep + 1) <= room) keep++;
            mEllStart = keep; mEllCount = n - keep; mMax = ellW;
        }
    }
    public void ellipsized(int start, int end) { mEllStart = start; mEllCount = end - start; }
    @Override public int getLineCount() { return 1; }
    @Override public int getLineStart(int l) { return l <= 0 ? 0 : mText.length(); }
    @Override public int getLineTop(int l) { return l <= 0 ? 0 : mBottom; }
    @Override public int getLineDescent(int l) { return mDesc; }
    @Override public int getHeight() { return mBottom; }
    @Override public int getTopPadding() { return mTopPad; }
    @Override public int getBottomPadding() { return mBottomPad; }
    @Override public int getEllipsisStart(int l) { return mEllStart; }
    @Override public int getEllipsisCount(int l) { return mEllCount; }
    @Override public int getEllipsizedWidth() { return mEllW; }
    @Override public float getLineMax(int l) { return mMax; }
    @Override public float getLineWidth(int l) { return mEllCount > 0 ? mMax : super.getLineWidth(l); }
}
