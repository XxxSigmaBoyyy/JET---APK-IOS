package android.graphics;
import java.awt.geom.*;
/** Android Path over java.awt.geom.Path2D (host); arc/oval/round-rect/op/transform semantics follow Skia. */
public class Path {
    public enum Direction { CW, CCW }
    public enum FillType { WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD }
    public enum Op { DIFFERENCE, INTERSECT, UNION, XOR, REVERSE_DIFFERENCE }
    Path2D.Float p = new Path2D.Float();
    private FillType fill = FillType.WINDING; private float lx, ly, mx, my;
    public Path() {}
    public Path(Path src) { if (src != null) set(src); }
    Path2D awt() { return p; }
    public void set(Path src) { p = new Path2D.Float(src.p); fill = src.fill; lx = src.lx; ly = src.ly; mx = src.mx; my = src.my; }
    public void reset() { p.reset(); fill = FillType.WINDING; lx = ly = mx = my = 0; }
    public void rewind() {
        // AOSP Path.rewind keeps fillType; Path2D.reset() keeps windingRule, but re-apply
        // so Compose ImageVector evenOdd holes survive toPath→rewind→restore.
        FillType keep = fill; p.reset(); setFillType(keep); lx = ly = mx = my = 0;
    }
    public boolean isEmpty() { return p.getCurrentPoint() == null; }
    public boolean isConvex() { return true; }
    public boolean isRect(RectF r) { return false; }
    public boolean isInverseFillType() { return fill == FillType.INVERSE_WINDING || fill == FillType.INVERSE_EVEN_ODD; }
    public void toggleInverseFillType() {}
    public FillType getFillType() { return fill; }
    public void setFillType(FillType f) {
        fill = f; p.setWindingRule(f == FillType.EVEN_ODD || f == FillType.INVERSE_EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        if (f == FillType.EVEN_ODD) {
            jetEvenOddSets++;
            if (jetEvenOddSets == 1) System.err.println("[JET] Path.setFillType(EVEN_ODD) first");
        }
    }
    public static int jetEvenOddSets;
    private void ensure() { if (p.getCurrentPoint() == null) p.moveTo(lx, ly); }
    public void moveTo(float x, float y) { p.moveTo(x, y); lx = mx = x; ly = my = y; }
    public void rMoveTo(float dx, float dy) { moveTo(lx + dx, ly + dy); }
    public void lineTo(float x, float y) { ensure(); p.lineTo(x, y); lx = x; ly = y; }
    public void rLineTo(float dx, float dy) { lineTo(lx + dx, ly + dy); }
    public void quadTo(float x1, float y1, float x2, float y2) { ensure(); p.quadTo(x1, y1, x2, y2); lx = x2; ly = y2; }
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) { quadTo(lx + dx1, ly + dy1, lx + dx2, ly + dy2); }
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) { ensure(); p.curveTo(x1, y1, x2, y2, x3, y3); lx = x3; ly = y3; }
    public void rCubicTo(float a, float b, float c, float d, float e, float f) { cubicTo(lx + a, ly + b, lx + c, ly + d, lx + e, ly + f); }
    public void close() { if (p.getCurrentPoint() != null) p.closePath(); lx = mx; ly = my; }
    private void arc(float l, float t, float r, float b, float start, float sweep, boolean forceMoveTo) {
        // Android angles: clockwise from +x in y-down space; Java2D Arc2D: counter-clockwise -> negate.
        Arc2D.Float arc = new Arc2D.Float(l, t, r - l, b - t, -start, -sweep, Arc2D.OPEN);
        boolean connect = !forceMoveTo && p.getCurrentPoint() != null;
        p.append(arc, connect);
        Point2D e = arc.getEndPoint(); lx = (float) e.getX(); ly = (float) e.getY();
        if (!connect) { Point2D s = arc.getStartPoint(); mx = (float) s.getX(); my = (float) s.getY(); }
    }
    public void arcTo(RectF o, float start, float sweep, boolean force) { arc(o.left, o.top, o.right, o.bottom, start, sweep, force); }
    public void arcTo(RectF o, float start, float sweep) { arcTo(o, start, sweep, false); }
    public void arcTo(float l, float t, float r, float b, float start, float sweep, boolean force) { arc(l, t, r, b, start, sweep, force); }
    public void addArc(RectF o, float start, float sweep) { arc(o.left, o.top, o.right, o.bottom, start, sweep, true); }
    public void addArc(float l, float t, float r, float b, float start, float sweep) { arc(l, t, r, b, start, sweep, true); }
    public void addRect(float l, float t, float r, float b, Direction d) { p.append(new Rectangle2D.Float(Math.min(l, r), Math.min(t, b), Math.abs(r - l), Math.abs(b - t)), false); lx = mx = l; ly = my = t; }
    public void addRect(RectF r, Direction d) { addRect(r.left, r.top, r.right, r.bottom, d); }
    public void addOval(float l, float t, float r, float b, Direction d) { p.append(new Ellipse2D.Float(l, t, r - l, b - t), false); }
    public void addOval(RectF r, Direction d) { addOval(r.left, r.top, r.right, r.bottom, d); }
    public void addCircle(float x, float y, float rad, Direction d) { addOval(x - rad, y - rad, x + rad, y + rad, d); }
    public void addRoundRect(float l, float t, float r, float b, float rx, float ry, Direction d) { p.append(new RoundRectangle2D.Float(l, t, r - l, b - t, rx * 2, ry * 2), false); }
    public void addRoundRect(RectF r, float rx, float ry, Direction d) { addRoundRect(r.left, r.top, r.right, r.bottom, rx, ry, d); }
    public void addRoundRect(float l, float t, float r, float b, float[] radii, Direction d) {
        float tlx = radii[0], tly = radii[1], trx = radii[2], try_ = radii[3], brx = radii[4], bry = radii[5], blx = radii[6], bly = radii[7];
        Path2D.Float q = new Path2D.Float();
        q.moveTo(l + tlx, t); q.lineTo(r - trx, t); q.quadTo(r, t, r, t + try_); q.lineTo(r, b - bry); q.quadTo(r, b, r - brx, b); q.lineTo(l + blx, b); q.quadTo(l, b, l, b - bly); q.lineTo(l, t + tly); q.quadTo(l, t, l + tlx, t); q.closePath();
        p.append(q, false);
    }
    public void addRoundRect(RectF r, float[] radii, Direction d) { addRoundRect(r.left, r.top, r.right, r.bottom, radii, d); }
    public void addPath(Path src) { p.append(src.p, false); }
    public void addPath(Path src, float dx, float dy) { p.append(src.p.createTransformedShape(AffineTransform.getTranslateInstance(dx, dy)), false); }
    public void addPath(Path src, Matrix m) { p.append(src.p.createTransformedShape(m.a), false); }
    public void offset(float dx, float dy) { p.transform(AffineTransform.getTranslateInstance(dx, dy)); lx += dx; ly += dy; }
    public void offset(float dx, float dy, Path dst) { if (dst == null || dst == this) offset(dx, dy); else { dst.set(this); dst.offset(dx, dy); } }
    public void transform(Matrix m) { p.transform(m.a); }
    public void transform(Matrix m, Path dst) { if (dst == null || dst == this) transform(m); else { dst.set(this); dst.transform(m); } }
    public void setLastPoint(float x, float y) { lx = x; ly = y; }
    public void computeBounds(RectF out, boolean exact) { Rectangle2D b = p.getBounds2D(); out.set((float) b.getMinX(), (float) b.getMinY(), (float) b.getMaxX(), (float) b.getMaxY()); }
    public void incReserve(int n) {}
    public boolean op(Path other, Op op) { return op(this, other, op); }
    public boolean op(Path a, Path b, Op op) {
        Area x = new Area(a.p), y = new Area(b.p);
        switch (op) { case DIFFERENCE: x.subtract(y); break; case INTERSECT: x.intersect(y); break; case UNION: x.add(y); break; case XOR: x.exclusiveOr(y); break; case REVERSE_DIFFERENCE: y.subtract(x); x = y; break; }
        p = new Path2D.Float(x); return true;
    }
    public float[] approximate(float err) { return new float[0]; }
    public boolean isInterpolatable(Path o) { return false; }
    public boolean interpolate(Path o, float t, Path out) { return false; }
}
