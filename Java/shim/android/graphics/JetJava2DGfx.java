package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
/** Java2D reference backend (Linux-only). Logic moved verbatim from the pre-pass-11 Canvas. */
final class JetJava2DGfx implements jet.gfx.Gfx {
    private Graphics2D g; private final int w, h;
    private static final class St { AffineTransform tx; Shape clip; Graphics2D parent; BufferedImage layer; int alpha = 255; }
    private final ArrayDeque<St> stack = new ArrayDeque<>();
    private final jet.gfx.PixelBuffer target;
    JetJava2DGfx(jet.gfx.PixelBuffer pb, boolean bitmap) { target = pb; g = view(pb).createGraphics(); if (bitmap) hints(g); w = pb.w; h = pb.h; }
    /** Linux-only reference backend: AWT BufferedImage *view* over the PixelBuffer's own int[] (zero copy; never escapes this class). */
    static BufferedImage view(jet.gfx.PixelBuffer pb) {
        Object v = pb.backendView;
        if (v instanceof BufferedImage) return (BufferedImage) v;
        java.awt.image.DataBufferInt db = new java.awt.image.DataBufferInt(pb.px, pb.w * pb.h);
        java.awt.image.WritableRaster r = java.awt.image.Raster.createPackedRaster(db, pb.w, pb.h, pb.w, new int[]{0xFF0000, 0xFF00, 0xFF, 0xFF000000}, null);
        BufferedImage bi = new BufferedImage(java.awt.image.ColorModel.getRGBdefault(), r, false, null);
        pb.backendView = bi; return bi;
    }
    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
    public int width() { return w; } public int height() { return h; }
    private St snap() { St s = new St(); s.tx = g.getTransform(); s.clip = g.getClip(); return s; }
    public void save() { stack.push(snap()); }
    public int depth() { return stack.size(); }
    public void saveLayer(int alpha) {
        St s = snap(); s.alpha = alpha; s.parent = g;
        s.layer = new BufferedImage(Math.max(1, Math.min(w, 8192)), Math.max(1, Math.min(h, 8192)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D lg = s.layer.createGraphics(); hints(lg); lg.setTransform(g.getTransform()); lg.setClip(g.getClip());
        stack.push(s); g = lg;
    }
    public void restore() {
        if (stack.isEmpty()) return;
        St s = stack.pop();
        if (s.layer != null) {
            g.dispose(); g = s.parent;
            AffineTransform keep = g.getTransform(); Shape kc = g.getClip(); Composite oc = g.getComposite();
            g.setTransform(new AffineTransform()); g.setClip(null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, s.alpha / 255f))));
            g.drawImage(s.layer, 0, 0, null);
            g.setComposite(oc); g.setTransform(keep); g.setClip(kc);
        }
        g.setTransform(s.tx); g.setClip(s.clip);
    }
    public AffineTransform getTransform() { return g.getTransform(); }
    public void setTransform(AffineTransform t) { g.setTransform(t); }
    public void concat(AffineTransform t) { g.transform(t); }
    public void clip(Shape s) { g.clip(s); }
    public void clipOut(Shape s) { Shape c = g.getClip(); Area a = new Area(c != null ? c : new Rectangle2D.Float(-1e5f, -1e5f, 2e5f, 2e5f)); a.subtract(new Area(s)); g.setClip(a); }
    public Rectangle getClipBounds() { try { return g.getClipBounds(); } catch (NullPointerException e) { return new Rectangle(); } /* non-invertible transform: clip collapses */ }

    private Composite saved;
    private boolean apply(Paint p) {
        saved = g.getComposite();
        if (p == null) { g.setColor(java.awt.Color.BLACK); return true; }
        int c = JetGfx.effectiveColor(p);
        Shader sh = p.getShader(); java.awt.Paint ap = sh == null ? null : sh.jetAwt();
        if (ap != null) { g.setPaint(ap); if (p.getAlpha() < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.getAlpha() / 255f)); }
        else g.setColor(new java.awt.Color(c, true));
        android.graphics.BlendMode abm = p.getBlendMode();
        Xfermode x = p.getXfermode();
        if (abm != null) {
            if (abm == android.graphics.BlendMode.CLEAR) g.setComposite(AlphaComposite.Clear);
            else if (abm == android.graphics.BlendMode.SRC) g.setComposite(AlphaComposite.Src);
            else if (abm == android.graphics.BlendMode.DST_OUT) g.setComposite(AlphaComposite.DstOut);
            else if (abm == android.graphics.BlendMode.DST_IN) g.setComposite(AlphaComposite.DstIn);
            else if (abm == android.graphics.BlendMode.SRC_IN) g.setComposite(AlphaComposite.SrcIn);
            else if (abm == android.graphics.BlendMode.SRC_ATOP) g.setComposite(AlphaComposite.SrcAtop);
            else if (abm == android.graphics.BlendMode.DST) return false;
        } else if (x instanceof PorterDuffXfermode) {
            PorterDuff.Mode m = ((PorterDuffXfermode) x).mode;
            if (m == PorterDuff.Mode.CLEAR) g.setComposite(AlphaComposite.Clear);
            else if (m == PorterDuff.Mode.SRC) g.setComposite(AlphaComposite.Src);
            else if (m == PorterDuff.Mode.DST_OUT) g.setComposite(AlphaComposite.DstOut);
            else if (m == PorterDuff.Mode.DST_IN) g.setComposite(AlphaComposite.DstIn);
            else if (m == PorterDuff.Mode.SRC_IN) g.setComposite(AlphaComposite.SrcIn);
            else if (m == PorterDuff.Mode.SRC_ATOP) g.setComposite(AlphaComposite.SrcAtop);
            else if (m == PorterDuff.Mode.DST) return false;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, p.isAntiAlias() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        return true;
    }
    private void unapply() { if (saved != null) g.setComposite(saved); saved = null; }
    private static BasicStroke stroke(Paint p) {
        int cap = p.getStrokeCap() == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND : p.getStrokeCap() == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
        int join = p.getStrokeJoin() == Paint.Join.ROUND ? BasicStroke.JOIN_ROUND : p.getStrokeJoin() == Paint.Join.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
        float w = Math.max(p.getStrokeWidth(), 0.0001f);
        float[] d = dash(p); if (d != null) return new BasicStroke(w, cap, join, Math.max(1, p.getStrokeMiter()), d, ((DashPathEffect) p.getPathEffect()).phase);
        return new BasicStroke(w, cap, join, Math.max(1, p.getStrokeMiter()));
    }
    static float[] dash(Paint p) {
        if (!(p.getPathEffect() instanceof DashPathEffect)) return null; DashPathEffect d = (DashPathEffect) p.getPathEffect();
        boolean ok = d.intervals != null && d.intervals.length >= 2; for (int i = 0; ok && i < d.intervals.length; i++) ok = d.intervals[i] >= 0; return ok ? d.intervals : null;
    }
    public void drawShape(Shape s, Paint p, boolean fill, boolean strokeIt) {
        if (!apply(p)) { unapply(); return; }
        try {
            if (fill) g.fill(s);
            if (strokeIt) { Stroke os = g.getStroke(); g.setStroke(stroke(p)); g.draw(s); g.setStroke(os); }
        } finally { unapply(); }
    }
    public void drawPaint(Paint p) {
        Shape c = g.getClip(); Shape s;
        if (c != null) { try { s = g.getTransform().createInverse().createTransformedShape(c); } catch (Exception e) { s = c; } } else s = new Rectangle2D.Float(-1e4f, -1e4f, 2e4f, 2e4f);
        drawShape(s, p, true, false);
    }
    static Font font(int style, int size) { return new Font(Font.SANS_SERIF, style, size); }
    public void drawText(String text, float x, float y, int style, int size, Paint p) {
        if (!apply(p)) { unapply(); return; }
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(font(style, size));
            Paint.Style st = p == null ? Paint.Style.FILL : p.getStyle();
            if (st == Paint.Style.FILL) g.drawString(text, x, y);
            else { Shape s = g.getFont().createGlyphVector(g.getFontRenderContext(), text).getOutline(x, y); if (st == Paint.Style.FILL_AND_STROKE) g.fill(s); Stroke os = g.getStroke(); g.setStroke(stroke(p)); g.draw(s); g.setStroke(os); }
            if (p != null && (p.isUnderlineText() || p.isStrikeThruText())) {
                float w = g.getFontMetrics().stringWidth(text);
                if (p.isUnderlineText()) g.fill(new Rectangle2D.Float(x, y + 1, w, Math.max(1, p.getTextSize() / 18)));
                if (p.isStrikeThruText()) g.fill(new Rectangle2D.Float(x, y - p.getTextSize() / 3, w, Math.max(1, p.getTextSize() / 18)));
            }
        } finally { unapply(); }
    }
    public void drawImage(jet.gfx.PixelBuffer src, AffineTransform at, Paint p) {
        Composite old = g.getComposite();
        if (p != null && p.getAlpha() < 255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.getAlpha() / 255f));
        Object oi = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, p == null || p.isFilterBitmap() ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (p != null && p.getColorFilter() != null) src = JetGfx.tint(src, p.getColorFilter());
        BufferedImage im = view(src);
        if (src.opaque) { BufferedImage o = new BufferedImage(src.w, src.h, BufferedImage.TYPE_INT_RGB); o.getRaster().setDataElements(0, 0, src.w, src.h, src.px); im = o; }
        g.drawImage(im, at, null);
        if (oi != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oi);
        g.setComposite(old);
    }
    public void drawColor(int color, int mode) {
        Composite old = g.getComposite();
        g.setComposite(mode == 2 ? AlphaComposite.Clear : mode == 1 ? AlphaComposite.Src : AlphaComposite.SrcOver);
        g.setColor(new java.awt.Color(color, true));
        AffineTransform t = g.getTransform(); Shape c = g.getClip();
        if (c != null) g.fill(c); else { g.setTransform(new AffineTransform()); g.fillRect(0, 0, Math.max(w, 10000), Math.max(h, 10000)); g.setTransform(t); }
        g.setComposite(old);
    }
    public void flush() { target.gen++; }
    public void dispose() { g.dispose(); }

    private static final java.awt.font.FontRenderContext FRC = new java.awt.font.FontRenderContext(null, true, true);
    private static Graphics2D MEASURE;
    static {
        // Apps (e.g. Signal) can ship a broken META-INF ServiceLoader InetAddressResolverProvider;
        // AWT FcFontConfiguration calls InetAddress.getLocalHost during first font metrics and aborts Compose layout.
        try { if (System.getProperty("java.awt.headless") == null) System.setProperty("java.awt.headless", "true"); } catch (Throwable ignored) {}
        try {
            MEASURE = (Graphics2D) new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics();
            MEASURE.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } catch (Throwable t) { MEASURE = null; }
    }
    static jet.gfx.TextMetrics metrics(int style, int size) {
        final Font f = font(style, size);
        final int sz = Math.max(1, size);
        FontMetrics fm = null;
        try { if (MEASURE != null) fm = MEASURE.getFontMetrics(f); } catch (Throwable ignored) {}
        final FontMetrics fmF = fm;
        return new jet.gfx.TextMetrics() {
            public int getAscent() { return fmF != null ? fmF.getAscent() : Math.round(sz * 0.8f); }
            public int getDescent() { return fmF != null ? fmF.getDescent() : Math.round(sz * 0.2f); }
            public int getLeading() { return fmF != null ? fmF.getLeading() : Math.max(1, sz / 10); }
            public int stringWidth(String s) {
                if (s == null) return 0;
                try { return Math.round((float) f.getStringBounds(s, FRC).getWidth()); } catch (Throwable t) {
                    return fmF != null ? fmF.stringWidth(s) : Math.max(0, s.length() * sz * 3 / 5);
                }
            }
            public float advance(String s) {
                if (s == null) return 0;
                try { return (float) f.getStringBounds(s, FRC).getWidth(); } catch (Throwable t) { return stringWidth(s); }
            }
            public boolean canDisplay(String s) { try { return f.canDisplayUpTo(s) == -1; } catch (Throwable t) { return true; } }
            public int getMaxAscent() { return fmF != null ? fmF.getMaxAscent() : getAscent(); }
            public int getMaxDescent() { return fmF != null ? fmF.getMaxDescent() : getDescent(); }
        };
    }
}
