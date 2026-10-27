package android.graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Font;
/** Skia backend via Skija (raster on Linux; same Skia API drives the Metal backend on iOS). */
final class JetSkiaGfx implements jet.gfx.Gfx {
    private static boolean _clrOnce;
    private static Rect xywh(float x, float y, float w, float h) {
        if (w < 0) { x += w; w = -w; } if (h < 0) { y += h; h = -h; }
        if (w < 0.0001f) w = 0; if (h < 0.0001f) h = 0;
        return Rect.makeXYWH(x, y, w, h);
    }
    static { jet.gfx.SkiaInit.ensure(); }
    private final jet.gfx.PixelBuffer target; private final boolean bitmap; private final int w, h;
    private final Surface surf; private final io.github.humbleui.skija.Canvas sk;
    private AffineTransform tx = new AffineTransform(); private Rectangle2D clipB;
    private final java.util.ArrayDeque<Object[]> st = new java.util.ArrayDeque<>();
    private static final ImageInfo UNPREMUL_INFO(int w, int h) { return new ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL); }
    static void init() { Surface s = Surface.makeRaster(new ImageInfo(1, 1, ColorType.BGRA_8888, ColorAlphaType.PREMUL)); s.close(); }
    JetSkiaGfx(jet.gfx.PixelBuffer img, boolean bitmap) {
        target = img; this.bitmap = bitmap; w = img.w; h = img.h;
        surf = Surface.makeRaster(new ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.PREMUL)); sk = surf.getCanvas();
        Image up = upload(img); sk.drawImage(up, 0, 0); up.close();
    }
    // ---- pixel interchange: PixelBuffer <-> Skia raster (BGRA_8888 little-endian == ARGB ints)
    static Image upload(jet.gfx.PixelBuffer im) {
        return Image.makeRasterFromBytes(new ImageInfo(im.w, im.h, ColorType.BGRA_8888, im.opaque ? ColorAlphaType.OPAQUE : ColorAlphaType.UNPREMUL), im.bgraBytes(), im.w * 4L);
    }
    public void flush() {
        io.github.humbleui.skija.Bitmap b = new io.github.humbleui.skija.Bitmap(); b.allocPixels(UNPREMUL_INFO(w, h)); surf.readPixels(b, 0, 0);
        byte[] by = b.readPixels(); b.close();
        target.setFromBgraBytes(by);
    }
    public void dispose() { if (!bitmap) flush(); surf.close(); }
    public int width() { return w; } public int height() { return h; }
    // ---- state
    public void save() { st.push(new Object[] { new AffineTransform(tx), clipB }); sk.save(); }
    public void saveLayer(int a) { st.push(new Object[] { new AffineTransform(tx), clipB }); try (io.github.humbleui.skija.Paint lp = new io.github.humbleui.skija.Paint()) { lp.setAlpha(Math.max(0, Math.min(255, a))); sk.saveLayer(null, lp); } }
    public void restore() { if (st.isEmpty()) return; Object[] s = st.pop(); tx = (AffineTransform) s[0]; clipB = (Rectangle2D) s[1]; sk.restore(); }
    public int depth() { return st.size(); }
    static Matrix33 m33(AffineTransform a) { return new Matrix33((float) a.getScaleX(), (float) a.getShearX(), (float) a.getTranslateX(), (float) a.getShearY(), (float) a.getScaleY(), (float) a.getTranslateY(), 0, 0, 1); }
    public AffineTransform getTransform() { return new AffineTransform(tx); }
    public void setTransform(AffineTransform t) { tx = new AffineTransform(t); sk.setMatrix(m33(tx)); }
    public void concat(AffineTransform t) { tx.concatenate(t); sk.setMatrix(m33(tx)); }
    public void clip(Shape s) {
        Rectangle2D b = tx.createTransformedShape(s).getBounds2D(); clipB = clipB == null ? b : clipB.createIntersection(b);
        if (s instanceof Rectangle2D) { Rectangle2D r = (Rectangle2D) s; float rx=(float)r.getX(), ry=(float)r.getY(), rw=(float)r.getWidth(), rh=(float)r.getHeight(); if (rw < 0) { rx += rw; rw = -rw; } if (rh < 0) { ry += rh; rh = -rh; } if (rw > 0 && rh > 0) sk.clipRect(Rect.makeXYWH(rx, ry, rw, rh), ClipMode.INTERSECT, false); else sk.clipRect(Rect.makeXYWH(0, 0, 0, 0), ClipMode.INTERSECT, false); }
        else try (io.github.humbleui.skija.Path p = path(s)) { sk.clipPath(p, ClipMode.INTERSECT, false); }
    }
    public void clipOut(Shape s) { try (io.github.humbleui.skija.Path p = path(s)) { sk.clipPath(p, ClipMode.DIFFERENCE, false); } }
    public Rectangle getClipBounds() {
        if (clipB == null) return null;
        try { return tx.createInverse().createTransformedShape(clipB).getBounds(); } catch (NoninvertibleTransformException e) { return new Rectangle(); }
    }
    // ---- geometry / paint conversion
    static io.github.humbleui.skija.Path path(Shape s) {
        PathBuilder pb = new PathBuilder(); float[] c = new float[6];
        PathIterator it = s.getPathIterator(null);
        pb.setFillMode(it.getWindingRule() == PathIterator.WIND_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
        for (; !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO: pb.moveTo(c[0], c[1]); break;
                case PathIterator.SEG_LINETO: pb.lineTo(c[0], c[1]); break;
                case PathIterator.SEG_QUADTO: pb.quadTo(c[0], c[1], c[2], c[3]); break;
                case PathIterator.SEG_CUBICTO: pb.cubicTo(c[0], c[1], c[2], c[3], c[4], c[5]); break;
                case PathIterator.SEG_CLOSE: pb.closePath(); break;
            }
        }
        io.github.humbleui.skija.Path p = pb.detach(); pb.close(); return p;
    }
    static FilterTileMode tile(Shader.TileMode t) { return t == Shader.TileMode.REPEAT ? FilterTileMode.REPEAT : t == Shader.TileMode.MIRROR ? FilterTileMode.MIRROR : FilterTileMode.CLAMP; }
    /** null = draw nothing (PorterDuff DST). */
    static io.github.humbleui.skija.Paint paint(android.graphics.Paint p, boolean stroke) {
        io.github.humbleui.skija.Paint sp = new io.github.humbleui.skija.Paint();
        if (p == null) { sp.setColor(0xFF000000); sp.setAntiAlias(false); return sp; }
        int c = JetGfx.effectiveColor(p);
        android.graphics.Shader sh = p.getShader();
        io.github.humbleui.skija.Shader ss = null;
        if (sh instanceof LinearGradient) { LinearGradient g = (LinearGradient) sh; if (g.colors != null && g.colors.length >= 2 && !(g.x0 == g.x1 && g.y0 == g.y1)) ss = io.github.humbleui.skija.Shader.makeLinearGradient(g.x0, g.y0, g.x1, g.y1, g.colors, Grad.fractions(g.pos, g.colors.length), new GradientStyle(tile(g.tile), false, null)); }
        else if (sh instanceof RadialGradient) { RadialGradient g = (RadialGradient) sh; if (g.colors != null && g.colors.length >= 2 && g.r > 0) ss = io.github.humbleui.skija.Shader.makeRadialGradient(g.cx, g.cy, g.r, g.colors, Grad.fractions(g.pos, g.colors.length)); }
        if (ss != null) { sp.setShader(ss); sp.setColor((p.getAlpha() << 24) | 0xFFFFFF); } else sp.setColor(c);
        // Skip fully-transparent fills (Compose Color.Transparent / textButtonColors container).
        if (ss == null && ((c >>> 24) & 0xFF) == 0 && !stroke) { sp.close(); return null; }
        MaskFilter mf = p.getMaskFilter();
        if (mf instanceof BlurMaskFilter) {
            BlurMaskFilter b = (BlurMaskFilter) mf;
            FilterBlurMode mode = b.style == BlurMaskFilter.Blur.SOLID ? FilterBlurMode.SOLID
                : b.style == BlurMaskFilter.Blur.OUTER ? FilterBlurMode.OUTER
                : b.style == BlurMaskFilter.Blur.INNER ? FilterBlurMode.INNER : FilterBlurMode.NORMAL;
            // Android radius is blur sigma-ish; Skia makeBlur takes sigma directly.
            sp.setMaskFilter(io.github.humbleui.skija.MaskFilter.makeBlur(mode, Math.max(0.01f, b.radius * 0.5f)));
        }
        // Prefer Paint.getBlendMode() (API 29 / Compose VectorPainter Clear) over legacy Xfermode.
        android.graphics.BlendMode abm = p.getBlendMode();
        Xfermode x = p.getXfermode();
        if (abm != null) {
            if (abm == android.graphics.BlendMode.DST) { sp.close(); return null; }
            BlendMode bm = abm == android.graphics.BlendMode.CLEAR ? BlendMode.CLEAR
                : abm == android.graphics.BlendMode.SRC ? BlendMode.SRC
                : abm == android.graphics.BlendMode.DST_OUT ? BlendMode.DST_OUT
                : abm == android.graphics.BlendMode.DST_IN ? BlendMode.DST_IN
                : abm == android.graphics.BlendMode.SRC_IN ? BlendMode.SRC_IN
                : abm == android.graphics.BlendMode.SRC_ATOP ? BlendMode.SRC_ATOP : null;
            if (bm != null) {
                if (bm == BlendMode.CLEAR && !_clrOnce) { _clrOnce = true; System.err.println("[JET] Paint.getBlendMode CLEAR applied (Compose VectorPainter)"); }
                sp.setBlendMode(bm);
            }
        } else if (x instanceof PorterDuffXfermode) {
            PorterDuff.Mode m = ((PorterDuffXfermode) x).mode;
            if (m == PorterDuff.Mode.DST) { sp.close(); return null; }
            BlendMode bm = m == PorterDuff.Mode.CLEAR ? BlendMode.CLEAR : m == PorterDuff.Mode.SRC ? BlendMode.SRC : m == PorterDuff.Mode.DST_OUT ? BlendMode.DST_OUT : m == PorterDuff.Mode.DST_IN ? BlendMode.DST_IN : m == PorterDuff.Mode.SRC_IN ? BlendMode.SRC_IN : m == PorterDuff.Mode.SRC_ATOP ? BlendMode.SRC_ATOP : null;
            if (bm != null) sp.setBlendMode(bm);
        }
        sp.setAntiAlias(p.isAntiAlias());
        if (stroke) {
            sp.setMode(PaintMode.STROKE); sp.setStrokeWidth(p.getStrokeWidth());
            sp.setStrokeCap(p.getStrokeCap() == android.graphics.Paint.Cap.ROUND ? PaintStrokeCap.ROUND : p.getStrokeCap() == android.graphics.Paint.Cap.SQUARE ? PaintStrokeCap.SQUARE : PaintStrokeCap.BUTT);
            sp.setStrokeJoin(p.getStrokeJoin() == android.graphics.Paint.Join.ROUND ? PaintStrokeJoin.ROUND : p.getStrokeJoin() == android.graphics.Paint.Join.BEVEL ? PaintStrokeJoin.BEVEL : PaintStrokeJoin.MITER);
            sp.setStrokeMiter(Math.max(1, p.getStrokeMiter()));
            float[] d = JetJava2DGfx.dash(p); if (d != null) sp.setPathEffect(PathEffect.makeDash(d.length % 2 == 0 ? d : java.util.Arrays.copyOf(d, d.length - 1), ((DashPathEffect) p.getPathEffect()).phase));
        }
        return sp;
    }
    // ---- draw
    public void drawShape(Shape s, android.graphics.Paint p, boolean fill, boolean stroke) {
        io.github.humbleui.skija.Path path = null;
        try {
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 0 ? !fill : !stroke) continue;
                io.github.humbleui.skija.Paint sp = paint(p, pass == 1); if (sp == null) return;
                if (s instanceof Rectangle2D) { Rectangle2D r = (Rectangle2D) s; sk.drawRect(xywh((float) r.getX(), (float) r.getY(), (float) r.getWidth(), (float) r.getHeight()), sp); }
                else { if (path == null) path = path(s); sk.drawPath(path, sp); }
                sp.close();
            }
        } finally { if (path != null) path.close(); }
    }
    public void drawPaint(android.graphics.Paint p) { io.github.humbleui.skija.Paint sp = paint(p, false); if (sp != null) { sk.drawPaint(sp); sp.close(); } }
    private static final Typeface[] TF = new Typeface[4];
    static synchronized Typeface tf(int style) {
        style &= 3; if (TF[style] == null) {
            FontStyle fs = style == 3 ? FontStyle.BOLD_ITALIC : style == 1 ? FontStyle.BOLD : style == 2 ? FontStyle.ITALIC : FontStyle.NORMAL;
            // Prefer Noto Sans (Android's Roboto stand-in), then common sans families. A fontconfig setup without the
            // family can hand back an empty typeface (no glyphs, zero metrics: TextViews measured 0 px high), so only
            // accept a face that actually maps 'A' and has a non-zero ascent.
            Typeface t = null;
            for (String fam : new String[] {"Noto Sans", "DejaVu Sans", "Liberation Sans", "sans-serif", null}) {
                Typeface c = FontMgr.getDefault().matchFamilyStyle(fam, fs);
                if (c != null && usable(c)) { t = c; break; }
                if (t == null) t = c;
            }
            TF[style] = t;
        }
        return TF[style];
    }
    private static boolean usable(Typeface t) {
        try (Font f = new Font(t, 16)) { return f.getMetrics().getAscent() < 0 && f.getUTF32Glyph('A') != 0; } catch (Throwable e) { return false; }
    }
    static Font font(int style, int size) { Font f = new Font(tf(style), size); f.setEdging(FontEdging.ANTI_ALIAS); return f; }
    private static final java.util.Map<String, TextLine> LINES = new java.util.LinkedHashMap<String, TextLine>(256, 0.75f, true) {
        protected boolean removeEldestEntry(java.util.Map.Entry<String, TextLine> e) { if (size() > 2000) { e.getValue().close(); return true; } return false; } };
    /** One long-lived shaper: TextLine.make() creates+frees a shaper per call, which crashed natively in deleteShaper. */
    private static io.github.humbleui.skija.shaper.Shaper SHAPER;
    /** HarfBuzz/SkShaper crashes natively on unpaired surrogates / C0 controls: map them to U+FFFD / space. */
    static String clean(String s) {
        StringBuilder b = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i); char r = c;
            if (Character.isHighSurrogate(c)) { if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) { if (b != null) { b.append(c).append(s.charAt(i + 1)); } i++; continue; } r = '\uFFFD'; }
            else if (Character.isLowSurrogate(c)) r = '\uFFFD';
            else if (c < 0x20 || (c >= 0x7F && c < 0xA0)) r = ' ';
            if (r != c && b == null) { b = new StringBuilder(s.length()); b.append(s, 0, i); }
            if (b != null) b.append(r);
        }
        return b == null ? s : b.toString();
    }
    static synchronized TextLine line(String s, int style, int size) {
        s = clean(s);
        String k = style + "|" + size + "|" + s; TextLine l = LINES.get(k);
        if (l == null) { Font f = font(style, size); if (SHAPER == null) SHAPER = io.github.humbleui.skija.shaper.Shaper.make(); l = SHAPER.shapeLine(s, f); f.close(); LINES.put(k, l); }
        return l;
    }
    public void drawText(String s, float x, float y, int style, int size, android.graphics.Paint p) {
        if (s.isEmpty()) return;
        android.graphics.Paint.Style ps = p == null ? android.graphics.Paint.Style.FILL : p.getStyle();
        synchronized (JetSkiaGfx.class) { drawTextLocked(s, x, y, style, size, p, ps); }
    }
    private void drawTextLocked(String s, float x, float y, int style, int size, android.graphics.Paint p, android.graphics.Paint.Style ps) {
        TextLine l = line(s, style, size);
        for (int pass = 0; pass < 2; pass++) {
            boolean stroke = pass == 1; if (stroke ? ps == android.graphics.Paint.Style.FILL : ps == android.graphics.Paint.Style.STROKE) continue;
            io.github.humbleui.skija.Paint sp = paint(p, stroke); if (sp == null) return; sp.setAntiAlias(true);
            sk.drawTextLine(l, x, y, sp); sp.close();
        }
        if (p != null && (p.isUnderlineText() || p.isStrikeThruText())) {
            io.github.humbleui.skija.Paint sp = paint(p, false); if (sp == null) return; float wd = Math.round(l.getWidth()), th = Math.max(1, p.getTextSize() / 18);
            if (p.isUnderlineText()) sk.drawRect(Rect.makeXYWH(x, y + 1, wd, th), sp);
            if (p.isStrikeThruText()) sk.drawRect(Rect.makeXYWH(x, y - p.getTextSize() / 3, wd, th), sp);
            sp.close();
        }
    }
    public void drawImage(jet.gfx.PixelBuffer im, AffineTransform at, android.graphics.Paint p) {
        if (p != null && p.getColorFilter() != null) im = JetGfx.tint(im, p.getColorFilter());
        Image si = upload(im);
        try (io.github.humbleui.skija.Paint sp = new io.github.humbleui.skija.Paint()) {
            if (p != null && p.getAlpha() < 255) sp.setAlpha(p.getAlpha());
            AffineTransform keep = new AffineTransform(tx); concat(at);
            sk.drawImageRect(si, Rect.makeWH(im.w, im.h), Rect.makeWH(im.w, im.h), p == null || p.isFilterBitmap() ? SamplingMode.LINEAR : SamplingMode.DEFAULT, sp, true);
            tx = keep; sk.setMatrix(m33(tx));
        } finally { si.close(); }
    }
    public void drawColor(int color, int mode) {
        try (io.github.humbleui.skija.Paint sp = new io.github.humbleui.skija.Paint()) { sp.setColor(color); sp.setBlendMode(mode == 2 ? BlendMode.CLEAR : mode == 1 ? BlendMode.SRC : BlendMode.SRC_OVER); sk.drawPaint(sp); }
    }
    // ---- metrics (AWT FontDesignMetrics rounding so integer layout matches the Java2D reference)
    static jet.gfx.TextMetrics metrics(int style, int size) {
        Font f = font(style, size); io.github.humbleui.skija.FontMetrics m = f.getMetrics(); f.close();
        final int mxa = (int) (0.95f - m.getTop()), mxd = (int) (0.95f + m.getBottom()); final int asc = (int) (0.95f - m.getAscent()), desc = (int) (0.95f + m.getDescent()), lead = (int) (0.95f + m.getDescent() + m.getLeading()) - desc;
        return new jet.gfx.TextMetrics() {
            public int getAscent() { return asc; } public int getDescent() { return desc; } public int getLeading() { return lead; }
            public int stringWidth(String s) { return Math.round(advance(s)); }
            public float advance(String s) { if (s.isEmpty()) return 0; synchronized (JetSkiaGfx.class) { return line(s, style, size).getWidth(); } }
            public int getMaxAscent() { return mxa; } public int getMaxDescent() { return mxd; }
            public boolean canDisplay(String s) { Font ff = font(style, size); try { for (short g : ff.getStringGlyphs(s)) if (g == 0) return false; return true; } finally { ff.close(); } }
        };
    }
}
