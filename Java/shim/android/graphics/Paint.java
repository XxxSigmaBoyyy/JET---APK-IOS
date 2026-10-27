package android.graphics;
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 0x01, FILTER_BITMAP_FLAG = 0x02, DITHER_FLAG = 0x04, UNDERLINE_TEXT_FLAG = 0x08, STRIKE_THRU_TEXT_FLAG = 0x10,
        FAKE_BOLD_TEXT_FLAG = 0x20, LINEAR_TEXT_FLAG = 0x40, SUBPIXEL_TEXT_FLAG = 0x80, EMBEDDED_BITMAP_TEXT_FLAG = 0x400;
    public static final int HINTING_OFF = 0, HINTING_ON = 1, START_HYPHEN_EDIT_NO_EDIT = 0, END_HYPHEN_EDIT_NO_EDIT = 0, CURSOR_AFTER = 0, CURSOR_AT_OR_AFTER = 1, CURSOR_BEFORE = 2, CURSOR_AT_OR_BEFORE = 3, CURSOR_AT = 4;
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }
    public static class FontMetrics { public float top, ascent, descent, bottom, leading; }
    public static class FontMetricsInt { public int top, ascent, descent, bottom, leading; }
    private Xfermode xfermode; private ColorFilter colorFilter; private Shader shader; private PathEffect pathEffect; private MaskFilter maskFilter; private BlendMode blendMode;
    private Cap cap = Cap.BUTT; private Join join = Join.MITER; private float miter = 4, skewX, scaleX = 1, letterSpacing, shadowR, shadowDx, shadowDy; private int shadowColor, hinting = 1;
    private String fontFeatures, fontVariation; private java.util.Locale textLocale = java.util.Locale.getDefault();
    public Xfermode setXfermode(Xfermode x) { xfermode = x; return x; } public Xfermode getXfermode() { return xfermode; }
    public void setBlendMode(BlendMode b) { blendMode = b; } public BlendMode getBlendMode() { return blendMode; }
    public ColorFilter setColorFilter(ColorFilter f) { colorFilter = f; return f; } public ColorFilter getColorFilter() { return colorFilter; }
    public Shader setShader(Shader sh) { shader = sh; return sh; } public Shader getShader() { return shader; }
    public PathEffect setPathEffect(PathEffect e) { pathEffect = e; return e; } public PathEffect getPathEffect() { return pathEffect; }
    public MaskFilter setMaskFilter(MaskFilter m) { maskFilter = m; return m; } public MaskFilter getMaskFilter() { return maskFilter; }
    public void setStrokeCap(Cap c) { cap = c; } public Cap getStrokeCap() { return cap; }
    public void setStrokeJoin(Join j) { join = j; } public Join getStrokeJoin() { return join; }
    public void setStrokeMiter(float m) { miter = m; } public float getStrokeMiter() { return miter; }
    public void setShadowLayer(float r, float dx, float dy, int c) { shadowR = r; shadowDx = dx; shadowDy = dy; shadowColor = c; }
    public void clearShadowLayer() { shadowR = 0; } public float getShadowLayerRadius() { return shadowR; } public float getShadowLayerDx() { return shadowDx; } public float getShadowLayerDy() { return shadowDy; } public int getShadowLayerColor() { return shadowColor; }
    public void setARGB(int a, int r, int g, int b) { setColor(Color.argb(a, r, g, b)); }
    public void setDither(boolean d) { setFlag(DITHER_FLAG, d); } public boolean isDither() { return (flags & DITHER_FLAG) != 0; }
    public void setFilterBitmap(boolean f) { setFlag(FILTER_BITMAP_FLAG, f); } public boolean isFilterBitmap() { return (flags & FILTER_BITMAP_FLAG) != 0; }
    public void setUnderlineText(boolean u) { setFlag(UNDERLINE_TEXT_FLAG, u); } public boolean isUnderlineText() { return (flags & UNDERLINE_TEXT_FLAG) != 0; }
    public void setStrikeThruText(boolean u) { setFlag(STRIKE_THRU_TEXT_FLAG, u); } public boolean isStrikeThruText() { return (flags & STRIKE_THRU_TEXT_FLAG) != 0; }
    public void setSubpixelText(boolean u) { setFlag(SUBPIXEL_TEXT_FLAG, u); } public boolean isSubpixelText() { return (flags & SUBPIXEL_TEXT_FLAG) != 0; }
    public void setLinearText(boolean u) { setFlag(LINEAR_TEXT_FLAG, u); } public boolean isLinearText() { return (flags & LINEAR_TEXT_FLAG) != 0; }
    public boolean isFakeBoldText() { return fakeBold; }
    private void setFlag(int f, boolean on) { if (on) flags |= f; else flags &= ~f; }
    public void setHinting(int h) { hinting = h; } public int getHinting() { return hinting; }
    public void setTextSkewX(float k) { skewX = k; } public float getTextSkewX() { return skewX; }
    public void setTextScaleX(float k) { scaleX = k; } public float getTextScaleX() { return scaleX; }
    public void setLetterSpacing(float l) { letterSpacing = l; } public float getLetterSpacing() { return letterSpacing; }
    public void setFontFeatureSettings(String f) { fontFeatures = f; } public String getFontFeatureSettings() { return fontFeatures; }
    public boolean setFontVariationSettings(String f) { fontVariation = f; return true; } public String getFontVariationSettings() { return fontVariation; }
    public void setTextLocale(java.util.Locale l) { textLocale = l; } public java.util.Locale getTextLocale() { return textLocale; }
    public void setElegantTextHeight(boolean b) {} public boolean isElegantTextHeight() { return false; }
    public void setStartHyphenEdit(int e) {} public void setEndHyphenEdit(int e) {}
    public void set(Paint p) { if (p == null) return; flags=p.flags; color=p.color; textSize=p.textSize; strokeWidth=p.strokeWidth; style=p.style; align=p.align; typeface=p.typeface; fakeBold=p.fakeBold; xfermode=p.xfermode; colorFilter=p.colorFilter; shader=p.shader; pathEffect=p.pathEffect; maskFilter=p.maskFilter; cap=p.cap; join=p.join; miter=p.miter; skewX=p.skewX; scaleX=p.scaleX; letterSpacing=p.letterSpacing; shadowR=p.shadowR; shadowDx=p.shadowDx; shadowDy=p.shadowDy; shadowColor=p.shadowColor; blendMode=p.blendMode; }
    public void reset() { set(new Paint()); }
    public float getFontSpacing() { return getFontMetrics(null); }
    public float getFontMetrics(FontMetrics m) {
        jet.gfx.TextMetrics fm = jetMetrics();
        if (m != null) { m.ascent = -fm.getAscent(); m.descent = fm.getDescent(); m.top = -fm.getMaxAscent() * 1.1f; m.bottom = fm.getMaxDescent() * 1.1f; m.leading = fm.getLeading(); }
        return fm.getAscent() + fm.getDescent() + fm.getLeading();
    }
    public FontMetrics getFontMetrics() { FontMetrics m = new FontMetrics(); getFontMetrics(m); return m; }
    public int getFontMetricsInt(FontMetricsInt m) { FontMetrics f = getFontMetrics(); if (m != null) { m.ascent = (int) Math.floor(f.ascent); m.descent = (int) Math.ceil(f.descent); m.top = (int) Math.floor(f.top); m.bottom = (int) Math.ceil(f.bottom); m.leading = Math.round(f.leading); } return Math.round(f.descent - f.ascent + f.leading); }
    public FontMetricsInt getFontMetricsInt() { FontMetricsInt m = new FontMetricsInt(); getFontMetricsInt(m); return m; }
    public float measureText(char[] t, int i, int n) { return measureText(new String(t, i, n)); }
    public float measureText(String t, int s, int e) { return measureText(t.substring(s, e)); }
    public float measureText(CharSequence t, int s, int e) { return measureText(t.subSequence(s, e).toString()); }
    public void getTextBounds(String t, int s, int e, Rect b) { String x = t.substring(s, e); jet.gfx.TextMetrics fm = jetMetrics(); b.set(0, -fm.getAscent(), Math.round(measureText(x)), fm.getDescent()); }
    public void getTextBounds(CharSequence t, int s, int e, Rect b) { getTextBounds(t.toString(), s, e, b); }
    public void getTextBounds(char[] t, int i, int n, Rect b) { getTextBounds(new String(t, i, n), 0, n, b); }
    public int getTextWidths(String t, float[] w) { for (int i = 0; i < t.length(); i++) w[i] = measureText(String.valueOf(t.charAt(i))); return t.length(); }
    public int getTextWidths(String t, int s, int e, float[] w) { return getTextWidths(t.substring(s, e), w); }
    public int getTextWidths(CharSequence t, int s, int e, float[] w) { return getTextWidths(t.toString().substring(s, e), w); }
    public int getTextWidths(char[] t, int i, int n, float[] w) { return getTextWidths(new String(t, i, n), w); }
    public int breakText(String t, boolean fwd, float max, float[] measured) { int n = 0; float w = 0; while (n < t.length()) { float cw = measureText(String.valueOf(t.charAt(fwd ? n : t.length() - 1 - n))); if (w + cw > max) break; w += cw; n++; } if (measured != null && measured.length > 0) measured[0] = w; return n; }
    public int breakText(CharSequence t, int s, int e, boolean fwd, float max, float[] m) { return breakText(t.subSequence(s, e).toString(), fwd, max, m); }
    public int breakText(char[] t, int i, int n, float max, float[] m) { return breakText(new String(t, i, n), true, max, m); }
    public boolean hasGlyph(String s) { return jetMetrics().canDisplay(s); }
    /** Backend text metrics for this paint's typeface/size (jet.gfx). */
    public jet.gfx.TextMetrics jetMetrics() { return JetGfx.metrics(JetGfx.style(this), JetGfx.size(this)); }
    public void getTextPath(String t, int s, int e, float x, float y, Path p) {}
    public boolean getFillPath(Path src, Path dst) { if (dst != null && src != null) dst.set(src); return true; }
    public float getRunAdvance(CharSequence t, int s, int e, int cs, int ce, boolean rtl, int off) { return measureText(t, s, off); }
    public static final int STYLE_FILL = 0, STYLE_STROKE = 1, STYLE_FILL_AND_STROKE = 2;
    public static final int ALIGN_LEFT = 0, ALIGN_CENTER = 1, ALIGN_RIGHT = 2;
    private int flags; private int color = Color.BLACK; private float textSize = 14; private float strokeWidth = 1;
    private int style = STYLE_FILL; private int align = ALIGN_LEFT; private Typeface typeface;
    private boolean fakeBold;
    public Paint() {}
    public Paint(int flags) { this.flags = flags; }
    public Paint(Paint p) { set(p); }
    public void setFlags(int f) { flags = f; }
    public int getFlags() { return flags; }
    public void setAntiAlias(boolean b) { if (b) flags |= ANTI_ALIAS_FLAG; else flags &= ~ANTI_ALIAS_FLAG; }
    public boolean isAntiAlias() { return (flags & ANTI_ALIAS_FLAG) != 0; }
    public void setColor(int c) { color = c; }
    public int getColor() { return color; }
    public void setAlpha(int a) { color = (color & 0x00FFFFFF) | (a << 24); }
    public int getAlpha() { return Color.alpha(color); }
    public void setTextSize(float s) { textSize = s; }
    public float getTextSize() { return textSize; }
    public void setStrokeWidth(float w) { strokeWidth = w; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setStyle(Style s) { style = s == Style.STROKE ? STYLE_STROKE : s == Style.FILL_AND_STROKE ? STYLE_FILL_AND_STROKE : STYLE_FILL; }
    public Style getStyle() { return style == STYLE_STROKE ? Style.STROKE : style == STYLE_FILL_AND_STROKE ? Style.FILL_AND_STROKE : Style.FILL; }
    public void setTextAlign(Align a) { align = a == Align.CENTER ? ALIGN_CENTER : a == Align.RIGHT ? ALIGN_RIGHT : ALIGN_LEFT; }
    public Align getTextAlign() { return align == ALIGN_CENTER ? Align.CENTER : align == ALIGN_RIGHT ? Align.RIGHT : Align.LEFT; }
    public Typeface setTypeface(Typeface t) { typeface = t; return t; }
    public Typeface getTypeface() { return typeface; }
    public void setFakeBoldText(boolean b) { fakeBold = b; }
    /** Android {@code @ColorLong} / Compose sRGB-packed long (ARGB in high 32 bits when cs id ≤ 15). */
    public void setColor(long c) {
        long cs = c & 0x3fL;
        if (cs <= 15L) { setColor((int) (c >>> 32)); return; }
        // Half-float ColorLong — decode via Color.toArgb when available.
        try { setColor(android.graphics.Color.toArgb(c)); }
        catch (Throwable t) { setColor((int) (c >>> 32)); }
    }
    public float measureText(String s) { float w = jetMetrics().advance(s); return w > 0 || s.isEmpty() ? w : s.length() * textSize * 0.5f; }
    public float ascent() { return -textSize * 0.8f; }
    public float descent() { return textSize * 0.2f; }
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.os.LocaleList jet_TextLocales;
    private float jet_WordSpacing;
    public float getUnderlinePosition() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getUnderlinePosition()F"); return 0; }
    public float getUnderlineThickness() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getUnderlineThickness()F"); return 0; }
    public float getStrikeThruPosition() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getStrikeThruPosition()F"); return 0; }
    public float getStrikeThruThickness() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getStrikeThruThickness()F"); return 0; }
    public long getColorLong() { return ((long) color) << 32; } // sRGB ColorLong / Compose packed
    public void setShadowLayer(float p0, float p1, float p2, long p3) { jet.runtime.ShimStubs.hit("android.graphics.Paint.setShadowLayer(FFFJ)V"); }
    public long getShadowLayerColorLong() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getShadowLayerColorLong()J"); return 0; }
    public android.os.LocaleList getTextLocales() { return jet_TextLocales; }
    public void setTextLocales(android.os.LocaleList p0) { jet_TextLocales = p0; }
    public float getWordSpacing() { return jet_WordSpacing; }
    public void setWordSpacing(float p0) { jet_WordSpacing = p0; }
    public int getStartHyphenEdit() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getStartHyphenEdit()I"); return 0; }
    public int getEndHyphenEdit() { jet.runtime.ShimStubs.hit("android.graphics.Paint.getEndHyphenEdit()I"); return 0; }
    public void getFontMetricsInt(java.lang.CharSequence p0, int p1, int p2, int p3, int p4, boolean p5, android.graphics.Paint.FontMetricsInt p6) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getFontMetricsInt(Ljava/lang/CharSequence;IIIIZLandroid/graphics/Paint$FontMetricsInt;)V"); }
    public void getFontMetricsInt(char[] p0, int p1, int p2, int p3, int p4, boolean p5, android.graphics.Paint.FontMetricsInt p6) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getFontMetricsInt([CIIIIZLandroid/graphics/Paint$FontMetricsInt;)V"); }
    public float getTextRunAdvances(char[] p0, int p1, int p2, int p3, int p4, boolean p5, float[] p6, int p7) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getTextRunAdvances([CIIIIZ[FI)F"); return 0; }
    public int getTextRunCursor(char[] p0, int p1, int p2, boolean p3, int p4, int p5) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getTextRunCursor([CIIZII)I"); return 0; }
    public int getTextRunCursor(java.lang.CharSequence p0, int p1, int p2, boolean p3, int p4, int p5) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getTextRunCursor(Ljava/lang/CharSequence;IIZII)I"); return 0; }
    public void getTextPath(char[] p0, int p1, int p2, float p3, float p4, android.graphics.Path p5) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getTextPath([CIIFFLandroid/graphics/Path;)V"); }
    public float getRunAdvance(char[] p0, int p1, int p2, int p3, int p4, boolean p5, int p6) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getRunAdvance([CIIIIZI)F"); return 0; }
    public float getRunCharacterAdvance(char[] p0, int p1, int p2, int p3, int p4, boolean p5, int p6, float[] p7, int p8) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getRunCharacterAdvance([CIIIIZI[FI)F"); return 0; }
    public float getRunCharacterAdvance(java.lang.CharSequence p0, int p1, int p2, int p3, int p4, boolean p5, int p6, float[] p7, int p8) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getRunCharacterAdvance(Ljava/lang/CharSequence;IIIIZI[FI)F"); return 0; }
    public int getOffsetForAdvance(char[] p0, int p1, int p2, int p3, int p4, boolean p5, float p6) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getOffsetForAdvance([CIIIIZF)I"); return 0; }
    public int getOffsetForAdvance(java.lang.CharSequence p0, int p1, int p2, int p3, int p4, boolean p5, float p6) { jet.runtime.ShimStubs.hit("android.graphics.Paint.getOffsetForAdvance(Ljava/lang/CharSequence;IIIIZF)I"); return 0; }
    public boolean equalsForTextMeasurement(android.graphics.Paint p0) { jet.runtime.ShimStubs.hit("android.graphics.Paint.equalsForTextMeasurement(Landroid/graphics/Paint;)Z"); return false; }
    // ---- END generated SDK surface ----
}
