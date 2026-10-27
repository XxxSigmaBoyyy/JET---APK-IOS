package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;

/** JET Layout: AOSP's line model (top/descent per line, baseline = next top - descent) over the paint's backend-neutral
 *  jet.gfx.TextMetrics (Paint.measureText / getFontMetricsInt). LTR only: no BiDi reordering; spans are not applied to glyph styling. */
public abstract class Layout {
    public static class Directions { Directions() {} }
    public enum Alignment { ALIGN_NORMAL, ALIGN_OPPOSITE, ALIGN_CENTER, ALIGN_LEFT, ALIGN_RIGHT }
    public static final int BREAK_STRATEGY_SIMPLE = 0, BREAK_STRATEGY_HIGH_QUALITY = 1, BREAK_STRATEGY_BALANCED = 2, HYPHENATION_FREQUENCY_NONE = 0, HYPHENATION_FREQUENCY_NORMAL = 1, HYPHENATION_FREQUENCY_FULL = 2, JUSTIFICATION_MODE_NONE = 0, JUSTIFICATION_MODE_INTER_WORD = 1;
    public static final int DIR_LEFT_TO_RIGHT = 1, DIR_RIGHT_TO_LEFT = -1;
    public static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(), DIRS_ALL_RIGHT_TO_LEFT = new Directions();
    public static float getDesiredWidth(CharSequence s, TextPaint p) { return getDesiredWidth(s, 0, s.length(), p); }
    public static float getDesiredWidth(CharSequence s, int start, int end, TextPaint p) { float w = 0; int ls = start; for (int i = start; i <= end; i++) if (i == end || s.charAt(i) == (char) 10) { w = Math.max(w, p.measureText(s, ls, i)); ls = i + 1; } return w; }
    public static float getDesiredWidth(CharSequence s, int start, int end, TextPaint p, TextDirectionHeuristic d) { return getDesiredWidth(s, start, end, p); }
    public static float getDesiredWidthWithLimit(CharSequence s, int start, int end, TextPaint p, TextDirectionHeuristic d, float limit) { float w = getDesiredWidth(s, start, end, p); return w > limit ? limit : w; }

    protected CharSequence mText = ""; protected TextPaint mPaint; protected int mWidth; protected Alignment mAlignment = Alignment.ALIGN_NORMAL;
    protected float mSpacingMult = 1f, mSpacingAdd = 0f;
    protected Layout() {}
    protected Layout(CharSequence text, TextPaint paint, int width, Alignment align, float mult, float add) {
        mText = text == null ? "" : text; mPaint = paint; mWidth = width; mAlignment = align == null ? Alignment.ALIGN_NORMAL : align; mSpacingMult = mult; mSpacingAdd = add;
    }

    // ---- per-line data supplied by subclasses ----
    public abstract int getLineCount();
    public int getLineStart(int l) { return 0; }
    public int getLineTop(int l) { return 0; }
    public int getLineDescent(int l) { return 0; }
    public int getTopPadding() { return 0; }
    public int getBottomPadding() { return 0; }
    public int getEllipsisStart(int l) { return 0; }
    public int getEllipsisCount(int l) { return 0; }
    public int getEllipsizedWidth() { return mWidth; }
    public int getParagraphDirection(int l) { return DIR_LEFT_TO_RIGHT; }
    public Directions getLineDirections(int l) { return DIRS_ALL_LEFT_TO_RIGHT; }
    public boolean getLineContainsTab(int l) { int s = getLineStart(l), e = getLineEnd(l); for (int i = s; i < e; i++) if (mText.charAt(i) == '\t') return true; return false; }

    // ---- derived geometry ----
    public CharSequence getText() { return mText; }
    public final TextPaint getPaint() { return mPaint; }
    public int getWidth() { return mWidth; }
    public final Alignment getAlignment() { return mAlignment; }
    public final float getSpacingMultiplier() { return mSpacingMult; }
    public final float getSpacingAdd() { return mSpacingAdd; }
    public void increaseWidthTo(int w) { if (w > mWidth) mWidth = w; }
    public int getHeight() { return getLineTop(getLineCount()); }
    public int getHeight(boolean cap) { return getHeight(); }
    public int getLineEnd(int l) { return l + 1 < getLineCount() ? getLineStart(l + 1) : mText.length(); }
    public int getLineBottom(int l) { return getLineTop(l + 1); }
    public int getLineBottom(int l, boolean includeLineSpacing) { return getLineTop(l + 1); }
    public int getLineBaseline(int l) { return getLineTop(l + 1) - getLineDescent(l); }
    public int getLineAscent(int l) { return getLineTop(l) - (getLineTop(l + 1) - getLineDescent(l)); }
    public int getLineBounds(int l, android.graphics.Rect r) { if (r != null) r.set(0, getLineTop(l), mWidth, getLineTop(l + 1)); return getLineBaseline(l); }
    public int getLineVisibleEnd(int l) { int s = getLineStart(l), e = getLineEnd(l); while (e > s) { char c = mText.charAt(e - 1); if (c == '\n' || c == ' ' || c == '\t') e--; else break; } return e; }
    public float getLineWidth(int l) { return measure(getLineStart(l), getLineVisibleEnd(l)); }
    public float getLineMax(int l) { return getLineWidth(l); }
    public float getLineLeft(int l) {
        float lw = getLineWidth(l); int avail = mWidth;
        switch (mAlignment) { case ALIGN_CENTER: return (avail - lw) / 2f; case ALIGN_OPPOSITE: case ALIGN_RIGHT: return avail - lw; default: return 0; }
    }
    public float getLineRight(int l) { return getLineLeft(l) + getLineWidth(l); }
    public int getParagraphLeft(int l) { return 0; }
    public int getParagraphRight(int l) { return mWidth; }
    public Alignment getParagraphAlignment(int l) { return mAlignment; }
    public int getLineForOffset(int o) { int n = getLineCount(); int lo = 0, hi = n - 1; while (lo < hi) { int m = (lo + hi + 1) >>> 1; if (getLineStart(m) <= o) lo = m; else hi = m - 1; } return Math.max(0, lo); }
    public int getLineForVertical(int v) { int n = getLineCount(); int lo = 0, hi = n - 1; while (lo < hi) { int m = (lo + hi + 1) >>> 1; if (getLineTop(m) <= v) lo = m; else hi = m - 1; } return Math.max(0, lo); }
    public float getPrimaryHorizontal(int o) { int l = getLineForOffset(o); return getLineLeft(l) + measure(getLineStart(l), Math.max(getLineStart(l), Math.min(o, getLineEnd(l)))); }
    public float getPrimaryHorizontal(int o, boolean clamped) { return getPrimaryHorizontal(o); }
    public float getSecondaryHorizontal(int o) { return getPrimaryHorizontal(o); }
    public boolean isRtlCharAt(int o) { return false; }
    public int getOffsetForHorizontal(int l, float x) {
        int s = getLineStart(l), e = getLineVisibleEnd(l); float left = getLineLeft(l); int best = s; float bd = Float.MAX_VALUE;
        for (int i = s; i <= e; i++) { float d = Math.abs(left + measure(s, i) - x); if (d < bd) { bd = d; best = i; } }
        return best;
    }
    public int getOffsetToLeftOf(int o) { return Math.max(0, o - 1); }
    public int getOffsetToRightOf(int o) { return Math.min(mText.length(), o + 1); }
    public void fillCharacterBounds(int start, int end, float[] bounds, int off) {
        for (int i = start; i < end; i++) { int l = getLineForOffset(i); float x0 = getPrimaryHorizontal(i), x1 = getLineLeft(l) + measure(getLineStart(l), Math.min(i + 1, getLineEnd(l)));
            int k = off + (i - start) * 4; bounds[k] = x0; bounds[k + 1] = getLineTop(l); bounds[k + 2] = x1; bounds[k + 3] = getLineBottom(l); }
    }
    public void getCursorPath(int o, android.graphics.Path p, CharSequence editing) { p.reset(); int l = getLineForOffset(o); float x = getPrimaryHorizontal(o); p.moveTo(x, getLineTop(l)); p.lineTo(x, getLineBottom(l)); }
    public void getSelectionPath(int s, int e, android.graphics.Path p) {
        p.reset(); if (s == e) return; if (s > e) { int t = s; s = e; e = t; }
        for (int l = getLineForOffset(s), le = getLineForOffset(e); l <= le; l++) {
            float x0 = l == getLineForOffset(s) ? getPrimaryHorizontal(s) : getLineLeft(l), x1 = l == le ? getPrimaryHorizontal(e) : getLineRight(l);
            p.addRect(x0, getLineTop(l), x1, getLineBottom(l), android.graphics.Path.Direction.CW);
        }
    }
    public boolean isFallbackLineSpacingEnabled() { return false; }
    protected boolean isSpanned() { return mText instanceof Spanned; }
    protected float measure(int s, int e) { if (mPaint == null || e <= s) return 0; return mPaint.measureText(mText, s, e); }

    // ---- drawing ----
    public void draw(Canvas c) { draw(c, null, null, 0); }
    public void draw(Canvas c, android.graphics.Path highlight, Paint hp, int cursorOffsetVertical) {
        if (highlight != null && hp != null) { c.translate(0, cursorOffsetVertical); c.drawPath(highlight, hp); c.translate(0, -cursorOffsetVertical); }
        drawText(c);
    }
    public void draw(Canvas c, java.util.List highlights, java.util.List highlightPaints, android.graphics.Path selection, Paint selPaint, int cursorOffsetVertical) { draw(c, selection, selPaint, cursorOffsetVertical); }
    public void drawBackground(Canvas c) {}
    public void drawText(Canvas c) {
        if (mPaint == null) return;
        int n = getLineCount();
        for (int l = 0; l < n; l++) {
            int s = getLineStart(l), e = getLineVisibleEnd(l), ec = getEllipsisCount(l);
            float x = getLineLeft(l), y = getLineBaseline(l);
            if (ec > 0) {
                int es = s + getEllipsisStart(l);
                String t = mText.subSequence(s, Math.min(es, e)).toString() + "\u2026";
                c.drawText(t, x, y, mPaint);
            } else if (e > s) c.drawText(mText, s, e, x, y, mPaint);
        }
    }
}
