package android.graphics;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.*;
/** Android Canvas: a pure facade over ONE raster backend (jet.gfx.Gfx: Skia by default, Java2D reference, or a display-list recorder). */
public class Canvas {
    public static final int ALL_SAVE_FLAG = 0x1f, CLIP_SAVE_FLAG = 0x02, MATRIX_SAVE_FLAG = 0x01, HAS_ALPHA_LAYER_SAVE_FLAG = 0x04, FULL_COLOR_LAYER_SAVE_FLAG = 0x08, CLIP_TO_LAYER_SAVE_FLAG = 0x10;
    public enum EdgeType { BW, AA }
    public enum VertexMode { TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN }
    jet.gfx.Gfx g;
    private Bitmap bitmap; private int density = 160;
    public Canvas() {}
    public Canvas(Bitmap bitmap) { setBitmap(bitmap); }
    public Canvas(jet.gfx.Gfx g) { this.g = g; }
    public void setBitmap(Bitmap bitmap) {
        if (g != null) { g.flush(); g.dispose(); }
        this.bitmap = bitmap;
        g = bitmap == null ? null : JetGfx.forBitmap(bitmap.pb);
    }
    public jet.gfx.Gfx jetGfx() { return g; }
    private void done() { if (bitmap != null && g != null) g.flush(); }
    private boolean ok() { return g != null; }
    public boolean isHardwareAccelerated() { return false; }
    public boolean isOpaque() { return false; }
    public int getWidth() { return g == null ? 0 : g.width(); }
    public int getHeight() { return g == null ? 0 : g.height(); }
    public int getDensity() { return density; } public void setDensity(int d) { density = d; }
    public int getMaximumBitmapWidth() { return 16384; } public int getMaximumBitmapHeight() { return 16384; }
    public void setDrawFilter(DrawFilter f) {} public DrawFilter getDrawFilter() { return null; }

    // ---- state stack
    public int save() { if (!ok()) return 0; g.save(); return g.depth(); }
    public int save(int flags) { return save(); }
    public int saveLayer(RectF b, Paint p) { return saveLayerAlpha(b, p == null ? 255 : p.getAlpha()); }
    public int saveLayer(RectF b, Paint p, int flags) { return saveLayer(b, p); }
    public int saveLayer(float l, float t, float r, float b, Paint p) { return saveLayer(new RectF(l, t, r, b), p); }
    public int saveLayer(float l, float t, float r, float b, Paint p, int flags) { return saveLayer(l, t, r, b, p); }
    public int saveLayerAlpha(RectF b, int alpha) { g.saveLayer(alpha); return g.depth(); }
    public int saveLayerAlpha(RectF b, int alpha, int flags) { return saveLayerAlpha(b, alpha); }
    public int saveLayerAlpha(float l, float t, float r, float b, int alpha) { return saveLayerAlpha(new RectF(l, t, r, b), alpha); }
    public int saveLayerAlpha(float l, float t, float r, float b, int alpha, int flags) { return saveLayerAlpha(l, t, r, b, alpha); }
    public void restore() { if (!ok() || g.depth() == 0) return; g.restore(); done(); }
    public int getSaveCount() { return ok() ? g.depth() + 1 : 1; }
    public void restoreToCount(int n) { while (g.depth() >= Math.max(1, n)) restore(); }

    // ---- matrix
    public void translate(float dx, float dy) { if (!ok()) return; g.concat(AffineTransform.getTranslateInstance(dx, dy)); }
    public void scale(float sx, float sy) { if (!ok()) return; g.concat(AffineTransform.getScaleInstance(sx, sy)); }
    public final void scale(float sx, float sy, float px, float py) { translate(px, py); scale(sx, sy); translate(-px, -py); }
    public void rotate(float degrees) { g.concat(AffineTransform.getRotateInstance(Math.toRadians(degrees))); }
    public final void rotate(float degrees, float px, float py) { g.concat(AffineTransform.getRotateInstance(Math.toRadians(degrees), px, py)); }
    public void skew(float sx, float sy) { g.concat(AffineTransform.getShearInstance(sx, sy)); }
    public void concat(Matrix m) { if (m != null) g.concat(m.a); }
    public void setMatrix(Matrix m) { g.setTransform(m == null ? new AffineTransform() : new AffineTransform(m.a)); }
    public void getMatrix(Matrix out) { out.a = new AffineTransform(g.getTransform()); }
    public final Matrix getMatrix() { Matrix m = new Matrix(); getMatrix(m); return m; }

    // ---- clip
    public boolean clipRect(float l, float t, float r, float b) { if (!ok()) return false; g.clip(new Rectangle2D.Float(l, t, r - l, b - t)); return !isClipEmpty(); }
    public boolean clipRect(Rect r) { return clipRect(r.left, r.top, r.right, r.bottom); }
    public boolean clipRect(RectF r) { return clipRect(r.left, r.top, r.right, r.bottom); }
    public boolean clipRect(int l, int t, int r, int b) { return clipRect((float) l, t, r, b); }
    public boolean clipRect(float l, float t, float r, float b, Region.Op op) { return op == Region.Op.DIFFERENCE ? clipOutRect(l, t, r, b) : clipRect(l, t, r, b); }
    public boolean clipRect(Rect r, Region.Op op) { return clipRect(r.left, r.top, r.right, r.bottom, op); }
    public boolean clipRect(RectF r, Region.Op op) { return clipRect(r.left, r.top, r.right, r.bottom, op); }
    public boolean clipPath(Path p) { g.clip(p.p); return !isClipEmpty(); }
    public boolean clipPath(Path p, Region.Op op) { return op == Region.Op.DIFFERENCE ? clipOutPath(p) : clipPath(p); }
    public boolean clipOutRect(float l, float t, float r, float b) { return clipOut(new Rectangle2D.Float(l, t, r - l, b - t)); }
    public boolean clipOutRect(Rect r) { return clipOutRect(r.left, r.top, r.right, r.bottom); }
    public boolean clipOutRect(RectF r) { return clipOutRect(r.left, r.top, r.right, r.bottom); }
    public boolean clipOutRect(int l, int t, int r, int b) { return clipOutRect((float) l, t, r, b); }
    public boolean clipOutPath(Path p) { return clipOut(p.p); }
    private boolean clipOut(Shape s) { g.clipOut(s); return !isClipEmpty(); }
    private boolean isClipEmpty() { Rectangle c = g.getClipBounds(); return c != null && c.isEmpty(); }
    public boolean getClipBounds(Rect out) { Rectangle b = g.getClipBounds(); if (b == null) { out.set(0, 0, getWidth(), getHeight()); return true; } out.set(b.x, b.y, b.x + b.width, b.y + b.height); return !b.isEmpty(); }
    public final Rect getClipBounds() { Rect r = new Rect(); getClipBounds(r); return r; }
    public boolean quickReject(RectF r, EdgeType t) { Rectangle b = g.getClipBounds(); return b != null && !b.intersects(r.left, r.top, r.width(), r.height()); }
    public boolean quickReject(RectF r) { return quickReject(r, EdgeType.AA); }
    public boolean quickReject(float l, float t, float r, float b, EdgeType e) { return quickReject(new RectF(l, t, r, b), e); }
    public boolean quickReject(float l, float t, float r, float b) { return quickReject(new RectF(l, t, r, b), EdgeType.AA); }
    public boolean quickReject(Path p, EdgeType e) { RectF r = new RectF(); p.computeBounds(r, false); return quickReject(r, e); }
    public boolean quickReject(Path p) { return quickReject(p, EdgeType.AA); }

    // ---- jet entry points for shim drawables/widgets (portable: geometry + Paint only)
    /** True when this is an app-defined Canvas subclass (a wrapper such as a no-clip/recording canvas that
     *  delegates the public draw API to another canvas): framework widgets must then draw through that public,
     *  overridable API, as on Android, instead of the internal helpers that target this object's own surface. */
    private Boolean jetForeign;
    private boolean jetForeign() {
        Boolean f = jetForeign;
        if (f == null) { ClassLoader mine = Canvas.class.getClassLoader(); Class<?> c = getClass(); f = Boolean.FALSE;
            for (; c != null && c != Canvas.class; c = c.getSuperclass()) if (c.getClassLoader() != mine) { f = Boolean.TRUE; break; }
            jetForeign = f; }
        return f;
    }
    private static Path jetPathOf(Shape s) { Path q = new Path(); q.p.append(s, false); return q; }
    public void jetFill(Shape s, Paint p) {
        if (jetForeign()) { Paint q = new Paint(p); q.setStyle(Paint.Style.FILL); drawPath(jetPathOf(s), q); return; }
        if (!ok()) return; g.drawShape(s, p, true, false); done(); }
    public void jetStroke(Shape s, Paint p) {
        if (jetForeign()) { Paint q = new Paint(p); q.setStyle(Paint.Style.STROKE); drawPath(jetPathOf(s), q); return; }
        if (!ok()) return; g.drawShape(s, p, false, true); done(); }
    public void jetDrawString(String s, float x, float y, int style, int size, Paint p) {
        if (jetForeign()) {
            Paint q = p == null ? new Paint() : new Paint(p); q.setTextSize(size); q.setTextAlign(Paint.Align.LEFT);
            if (JetGfx.style(q) != style) q.setTypeface(Typeface.create(q.getTypeface(), style & 3));
            drawText(s, x, y, q); return;
        }
        if (!ok()) return; g.drawText(s, x, y, style, size, p); done(); }
    public void jetDrawImage(jet.gfx.PixelBuffer im, AffineTransform at, Paint p) { if (!ok()) return; g.drawImage(im, at, p); done(); }
    public void jetClip(Shape s) { if (!ok()) return; g.clip(s); }
    private void shape(Shape s, Paint p) {
        if (!ok()) return;
        Paint.Style st = p == null ? Paint.Style.FILL : p.getStyle();
        g.drawShape(s, p, st == Paint.Style.FILL || st == Paint.Style.FILL_AND_STROKE, st == Paint.Style.STROKE || st == Paint.Style.FILL_AND_STROKE); done();
    }

    // ---- draw
    public void drawColor(int color) { drawColor(color, PorterDuff.Mode.SRC_OVER); }
    public void drawColor(long color) { drawColor((int) (color >>> 32)); }
    public void drawColor(int color, PorterDuff.Mode mode) { if (!ok()) return; g.drawColor(color, mode == PorterDuff.Mode.CLEAR ? 2 : mode == PorterDuff.Mode.SRC ? 1 : 0); done(); }
    public void drawColor(int color, BlendMode mode) { drawColor(color, mode == BlendMode.CLEAR ? PorterDuff.Mode.CLEAR : mode == BlendMode.SRC ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER); }
    public void drawRGB(int r, int gg, int b) { drawColor(Color.rgb(r, gg, b)); }
    public void drawARGB(int a, int r, int gg, int b) { drawColor(Color.argb(a, r, gg, b)); }
    public void drawPaint(Paint p) { Paint q = new Paint(p); q.setStyle(Paint.Style.FILL); g.drawPaint(q); done(); }
    public void drawRect(float l, float t, float r, float b, Paint p) { shape(new Rectangle2D.Float(l, t, r - l, b - t), p); }
    public void drawRect(Rect r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }
    public void drawRect(RectF r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }
    public void drawRoundRect(float l, float t, float r, float b, float rx, float ry, Paint p) { shape(new RoundRectangle2D.Float(l, t, r - l, b - t, rx * 2, ry * 2), p); }
    public void drawRoundRect(RectF r, float rx, float ry, Paint p) { drawRoundRect(r.left, r.top, r.right, r.bottom, rx, ry, p); }
    public void drawDoubleRoundRect(RectF o, float orx, float ory, RectF i, float irx, float iry, Paint p) { Area a = new Area(new RoundRectangle2D.Float(o.left, o.top, o.width(), o.height(), orx * 2, ory * 2)); a.subtract(new Area(new RoundRectangle2D.Float(i.left, i.top, i.width(), i.height(), irx * 2, iry * 2))); shape(a, p); }
    public void drawOval(float l, float t, float r, float b, Paint p) { shape(new Ellipse2D.Float(l, t, r - l, b - t), p); }
    public void drawOval(RectF r, Paint p) { drawOval(r.left, r.top, r.right, r.bottom, p); }
    public void drawCircle(float cx, float cy, float radius, Paint p) { shape(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2), p); }
    public void drawArc(float l, float t, float r, float b, float start, float sweep, boolean useCenter, Paint p) { shape(new Arc2D.Float(l, t, r - l, b - t, -start, -sweep, useCenter ? Arc2D.PIE : Arc2D.OPEN), p); }
    public void drawArc(RectF o, float start, float sweep, boolean useCenter, Paint p) { drawArc(o.left, o.top, o.right, o.bottom, start, sweep, useCenter, p); }
    public void drawLine(float x0, float y0, float x1, float y1, Paint p) { Paint q = new Paint(p); q.setStyle(Paint.Style.STROKE); shape(new Line2D.Float(x0, y0, x1, y1), q); }
    public void drawLines(float[] pts, int off, int count, Paint p) { for (int i = off; i + 3 < off + count; i += 4) drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], p); }
    public void drawLines(float[] pts, Paint p) { drawLines(pts, 0, pts.length, p); }
    public void drawPoint(float x, float y, Paint p) { float w = Math.max(1, p.getStrokeWidth()); Paint q = new Paint(p); q.setStyle(Paint.Style.FILL); if (p.getStrokeCap() == Paint.Cap.ROUND) shape(new Ellipse2D.Float(x - w / 2, y - w / 2, w, w), q); else shape(new Rectangle2D.Float(x - w / 2, y - w / 2, w, w), q); }
    public void drawPoints(float[] pts, int off, int count, Paint p) { for (int i = off; i + 1 < off + count; i += 2) drawPoint(pts[i], pts[i + 1], p); }
    public void drawPoints(float[] pts, Paint p) { drawPoints(pts, 0, pts.length, p); }
    public void drawPath(Path path, Paint p) {
        if (path == null) return;
        // Keep AWT winding in sync with Android fillType (Compose ImageVector evenOdd holes).
        path.p.setWindingRule(path.getFillType() == Path.FillType.EVEN_ODD || path.getFillType() == Path.FillType.INVERSE_EVEN_ODD
            ? java.awt.geom.Path2D.WIND_EVEN_ODD : java.awt.geom.Path2D.WIND_NON_ZERO);
        shape(path.p, p);
    }
    public void drawText(String text, float x, float y, Paint p) {
        if (!ok()) return;
        if (text == null) return;
        int st = p == null ? 0 : JetGfx.style(p), sz = p == null ? 12 : JetGfx.size(p);
        if (p != null && p.getTextAlign() != Paint.Align.LEFT) { float w = JetGfx.metrics(st, sz).stringWidth(text); x -= p.getTextAlign() == Paint.Align.CENTER ? w / 2f : w; }
        g.drawText(text, x, y, st, sz, p); done();
    }
    public void drawText(String t, int s, int e, float x, float y, Paint p) { drawText(t.substring(s, e), x, y, p); }
    public void drawText(CharSequence t, int s, int e, float x, float y, Paint p) { drawText(t.subSequence(s, e).toString(), x, y, p); }
    public void drawText(char[] t, int i, int n, float x, float y, Paint p) { drawText(new String(t, i, n), x, y, p); }
    public void drawTextRun(CharSequence t, int s, int e, int cs, int ce, float x, float y, boolean rtl, Paint p) { drawText(t, s, e, x, y, p); }
    public void drawTextRun(char[] t, int i, int n, int ci, int cn, float x, float y, boolean rtl, Paint p) { drawText(t, i, n, x, y, p); }
    public void drawTextOnPath(String t, Path path, float h, float v, Paint p) { RectF b = new RectF(); path.computeBounds(b, false); drawText(t, b.left + h, b.top + v, p); }
    private void img(jet.gfx.PixelBuffer im, AffineTransform at, Paint p) { g.drawImage(im, at, p); done(); }
    public void drawBitmap(Bitmap bmp, float left, float top, Paint p) { if (bmp == null) return; img(bmp.pb, AffineTransform.getTranslateInstance(left, top), p); }
    public void drawBitmap(Bitmap bmp, Rect src, Rect dst, Paint p) { if (bmp == null || dst == null) return; drawBitmap(bmp, src, new RectF(dst), p); }
    public void drawBitmap(Bitmap bmp, Rect src, RectF dst, Paint p) {
        if (bmp == null || dst == null) return;
        jet.gfx.PixelBuffer im = bmp.pb; int sw = im.w, sh = im.h;
        if (src != null) { int x = Math.max(0, src.left), y = Math.max(0, src.top), w = Math.min(sw - x, src.width()), h = Math.min(sh - y, src.height()); if (w <= 0 || h <= 0) return; im = bmp.pb.sub(x, y, w, h); sw = w; sh = h; }
        AffineTransform at = AffineTransform.getTranslateInstance(dst.left, dst.top); at.scale(dst.width() / sw, dst.height() / sh); img(im, at, p);
    }
    public void drawBitmap(Bitmap bmp, Matrix m, Paint p) { if (bmp == null) return; img(bmp.pb, m == null ? new AffineTransform() : new AffineTransform(m.a), p); }
    public void drawBitmap(int[] colors, int off, int stride, float x, float y, int w, int h, boolean alpha, Paint p) { jet.gfx.PixelBuffer b = new jet.gfx.PixelBuffer(w, h); b.write(colors, off, stride, 0, 0, w, h); b.opaque = !alpha; img(b, AffineTransform.getTranslateInstance(x, y), p); }
    public void drawBitmapMesh(Bitmap b, int mw, int mh, float[] v, int vo, int[] c, int co, Paint p) {}
    public void drawVertices(VertexMode m, int n, float[] v, int vo, float[] t, int to, int[] c, int co, short[] i, int io, int ic, Paint p) {}
    public void drawPicture(Picture pic) {} public void drawPicture(Picture pic, RectF dst) {} public void drawPicture(Picture pic, Rect dst) {}
    public void drawRenderNode(android.graphics.RenderNode n) { if (n != null) n.jetReplay(this); }
    public void enableZ() {} public void disableZ() {}
}
