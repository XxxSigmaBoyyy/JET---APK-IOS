package android.graphics;
import java.awt.geom.AffineTransform;
/** Affine Android Matrix (perspective entries kept but not applied on host). */
public class Matrix {
    public static final int MSCALE_X = 0, MSKEW_X = 1, MTRANS_X = 2, MSKEW_Y = 3, MSCALE_Y = 4, MTRANS_Y = 5, MPERSP_0 = 6, MPERSP_1 = 7, MPERSP_2 = 8;
    public static final Matrix IDENTITY_MATRIX = new Matrix();
    public enum ScaleToFit { FILL, START, CENTER, END }
    AffineTransform a = new AffineTransform();
    public Matrix() {}
    public Matrix(Matrix m) { if (m != null) a = new AffineTransform(m.a); }
    public AffineTransform jetAwt() { return a; }
    public boolean isIdentity() { return a.isIdentity(); }
    public boolean isAffine() { return true; }
    public boolean rectStaysRect() { return (a.getType() & (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM)) == 0; }
    public void set(Matrix m) { a = m == null ? new AffineTransform() : new AffineTransform(m.a); }
    public void reset() { a.setToIdentity(); }
    public void setTranslate(float dx, float dy) { a.setToTranslation(dx, dy); }
    public void setScale(float sx, float sy) { a.setToScale(sx, sy); }
    public void setScale(float sx, float sy, float px, float py) { a.setToTranslation(px, py); a.scale(sx, sy); a.translate(-px, -py); }
    public void setRotate(float d) { a.setToRotation(Math.toRadians(d)); }
    public void setRotate(float d, float px, float py) { a.setToRotation(Math.toRadians(d), px, py); }
    public void setSinCos(float s, float c) { a.setTransform(c, s, -s, c, 0, 0); }
    public void setSinCos(float s, float c, float px, float py) { a.setTransform(c, s, -s, c, px - c * px + s * py, py - s * px - c * py); }
    public void setSkew(float kx, float ky) { a.setToShear(kx, ky); }
    public void setSkew(float kx, float ky, float px, float py) { a.setToTranslation(px, py); a.shear(kx, ky); a.translate(-px, -py); }
    public boolean setConcat(Matrix x, Matrix y) { AffineTransform t = new AffineTransform(x.a); t.concatenate(y.a); a = t; return true; }
    private static AffineTransform tr(float dx, float dy) { return AffineTransform.getTranslateInstance(dx, dy); }
    private static AffineTransform sc(float sx, float sy, float px, float py) { AffineTransform t = tr(px, py); t.scale(sx, sy); t.translate(-px, -py); return t; }
    private static AffineTransform ro(float d, float px, float py) { return AffineTransform.getRotateInstance(Math.toRadians(d), px, py); }
    private static AffineTransform sk(float kx, float ky, float px, float py) { AffineTransform t = tr(px, py); t.shear(kx, ky); t.translate(-px, -py); return t; }
    private boolean pre(AffineTransform t) { a.concatenate(t); return true; }
    private boolean post(AffineTransform t) { a.preConcatenate(t); return true; }
    public boolean preTranslate(float dx, float dy) { return pre(tr(dx, dy)); }
    public boolean preScale(float sx, float sy) { return pre(sc(sx, sy, 0, 0)); }
    public boolean preScale(float sx, float sy, float px, float py) { return pre(sc(sx, sy, px, py)); }
    public boolean preRotate(float d) { return pre(ro(d, 0, 0)); }
    public boolean preRotate(float d, float px, float py) { return pre(ro(d, px, py)); }
    public boolean preSkew(float kx, float ky) { return pre(sk(kx, ky, 0, 0)); }
    public boolean preSkew(float kx, float ky, float px, float py) { return pre(sk(kx, ky, px, py)); }
    public boolean preConcat(Matrix m) { return pre(m.a); }
    public boolean postTranslate(float dx, float dy) { return post(tr(dx, dy)); }
    public boolean postScale(float sx, float sy) { return post(sc(sx, sy, 0, 0)); }
    public boolean postScale(float sx, float sy, float px, float py) { return post(sc(sx, sy, px, py)); }
    public boolean postRotate(float d) { return post(ro(d, 0, 0)); }
    public boolean postRotate(float d, float px, float py) { return post(ro(d, px, py)); }
    public boolean postSkew(float kx, float ky) { return post(sk(kx, ky, 0, 0)); }
    public boolean postSkew(float kx, float ky, float px, float py) { return post(sk(kx, ky, px, py)); }
    public boolean postConcat(Matrix m) { return post(m.a); }
    public boolean invert(Matrix inv) { try { AffineTransform t = a.createInverse(); if (inv != null) inv.a = t; return true; } catch (java.awt.geom.NoninvertibleTransformException e) { return false; } }
    public void getValues(float[] v) { v[0] = (float) a.getScaleX(); v[1] = (float) a.getShearX(); v[2] = (float) a.getTranslateX(); v[3] = (float) a.getShearY(); v[4] = (float) a.getScaleY(); v[5] = (float) a.getTranslateY(); v[6] = 0; v[7] = 0; v[8] = 1; }
    public void setValues(float[] v) { a.setTransform(v[0], v[3], v[1], v[4], v[2], v[5]); }
    public void mapPoints(float[] dst, int di, float[] src, int si, int n) { a.transform(src, si, dst, di, n); }
    public void mapPoints(float[] dst, float[] src) { mapPoints(dst, 0, src, 0, src.length / 2); }
    public void mapPoints(float[] pts) { mapPoints(pts, 0, pts, 0, pts.length / 2); }
    public void mapVectors(float[] dst, int di, float[] src, int si, int n) { double m00 = a.getScaleX(), m01 = a.getShearX(), m10 = a.getShearY(), m11 = a.getScaleY(); float[] s = src.clone(); for (int i = 0; i < n; i++) { float x = s[si + 2 * i], y = s[si + 2 * i + 1]; dst[di + 2 * i] = (float) (m00 * x + m01 * y); dst[di + 2 * i + 1] = (float) (m10 * x + m11 * y); } }
    public void mapVectors(float[] dst, float[] src) { mapVectors(dst, 0, src, 0, src.length / 2); }
    public void mapVectors(float[] v) { mapVectors(v, 0, v, 0, v.length / 2); }
    public boolean mapRect(RectF dst, RectF src) {
        float[] p = { src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom }; mapPoints(p);
        dst.set(Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6])), Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7])), Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6])), Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7])));
        return rectStaysRect();
    }
    public boolean mapRect(RectF r) { return mapRect(r, new RectF(r)); }
    public float mapRadius(float r) { float[] v = { r, 0, 0, r }; mapVectors(v); return (float) Math.sqrt(Math.hypot(v[0], v[1]) * Math.hypot(v[2], v[3])); }
    public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
        if (src.isEmpty()) { reset(); return false; }
        float sx = dst.width() / src.width(), sy = dst.height() / src.height(); float tx = dst.left - src.left * sx, ty = dst.top - src.top * sy;
        if (stf != ScaleToFit.FILL) { float s = Math.min(sx, sy); float dx = dst.width() - src.width() * s, dy = dst.height() - src.height() * s; sx = sy = s; tx = dst.left - src.left * s; ty = dst.top - src.top * s; if (stf == ScaleToFit.CENTER) { tx += dx / 2; ty += dy / 2; } else if (stf == ScaleToFit.END) { tx += dx; ty += dy; } }
        a.setTransform(sx, 0, 0, sy, tx, ty); return true;
    }
    public boolean setPolyToPoly(float[] src, int si, float[] dst, int di, int n) { if (n == 0) { reset(); return true; } if (n == 1) { setTranslate(dst[di] - src[si], dst[di + 1] - src[si + 1]); return true; } return false; }
    @Override public boolean equals(Object o) { return o instanceof Matrix && ((Matrix) o).a.equals(a); }
    @Override public int hashCode() { return a.hashCode(); }
    @Override public String toString() { float[] v = new float[9]; getValues(v); return "Matrix" + java.util.Arrays.toString(v); }
    public String toShortString() { return toString(); }
}
