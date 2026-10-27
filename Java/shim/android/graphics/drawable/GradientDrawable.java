package android.graphics.drawable;

import android.content.res.ColorStateList;
import java.awt.*;
import java.awt.geom.*;

/** GradientDrawable (XML <shape>): rect/oval/line/ring, solid or linear/radial/sweep gradient, corners, stroke (Java2D on host). */
public class GradientDrawable extends Drawable {
    public static final int RECTANGLE = 0, OVAL = 1, LINE = 2, RING = 3;
    public static final int LINEAR_GRADIENT = 0, RADIAL_GRADIENT = 1, SWEEP_GRADIENT = 2;
    public enum Orientation { TOP_BOTTOM, TR_BL, RIGHT_LEFT, BR_TL, BOTTOM_TOP, BL_TR, LEFT_RIGHT, TL_BR }
    private int mShape = RECTANGLE, mGradient = LINEAR_GRADIENT;
    private Orientation mOrientation = Orientation.TOP_BOTTOM;
    private ColorStateList mSolid; private int[] mColors;
    private float mRadius; private float[] mRadii;
    private int mStrokeWidth; private ColorStateList mStroke; private float mDashW, mDashGap;
    private int mW = -1, mH = -1, mAlpha = 255; private float mGradRadius = 0.5f, mCx = 0.5f, mCy = 0.5f;
    private boolean mUseLevel; private int mInnerRadius = -1, mThickness = -1; private float mInnerRatio = 3f, mThickRatio = 9f;

    public GradientDrawable() {}
    public GradientDrawable(Orientation o, int[] colors) { mOrientation = o; setColors(colors); }
    public void setShape(int s) { mShape = s; invalidateSelf(); } public int getShape() { return mShape; }
    public void setGradientType(int t) { mGradient = t; invalidateSelf(); } public int getGradientType() { return mGradient; }
    public void setOrientation(Orientation o) { mOrientation = o; invalidateSelf(); } public Orientation getOrientation() { return mOrientation; }
    public void setColors(int[] c) { mColors = c == null ? null : c.clone(); if (c != null) mSolid = null; invalidateSelf(); }
    public void setColors(int[] c, float[] offsets) { setColors(c); }
    public int[] getColors() { return mColors == null ? null : mColors.clone(); }
    public void setColor(int argb) { mSolid = ColorStateList.valueOf(argb); mColors = null; invalidateSelf(); }
    public void setColor(ColorStateList c) { mSolid = c; mColors = null; invalidateSelf(); }
    public ColorStateList getColor() { return mSolid; }
    public void setCornerRadius(float r) { mRadius = Math.max(0, r); mRadii = null; invalidateSelf(); }
    public float getCornerRadius() { return mRadius; }
    public void setCornerRadii(float[] r) { mRadii = r == null ? null : r.clone(); invalidateSelf(); }
    public float[] getCornerRadii() { return mRadii == null ? null : mRadii.clone(); }
    public void setStroke(int w, int c) { setStroke(w, ColorStateList.valueOf(c), 0, 0); }
    public void setStroke(int w, ColorStateList c) { setStroke(w, c, 0, 0); }
    public void setStroke(int w, int c, float dw, float dg) { setStroke(w, ColorStateList.valueOf(c), dw, dg); }
    public void setStroke(int w, ColorStateList c, float dw, float dg) { mStrokeWidth = w; mStroke = c; mDashW = dw; mDashGap = dg; invalidateSelf(); }
    public void setSize(int w, int h) { mW = w; mH = h; invalidateSelf(); }
    public void setGradientRadius(float r) { mGradRadius = r; } public float getGradientRadius() { return mGradRadius; }
    public void setGradientCenter(float x, float y) { mCx = x; mCy = y; } public float getGradientCenterX() { return mCx; } public float getGradientCenterY() { return mCy; }
    public void setUseLevel(boolean u) { mUseLevel = u; } public boolean getUseLevel() { return mUseLevel; }
    public void setInnerRadius(int r) { mInnerRadius = r; } public int getInnerRadius() { return mInnerRadius; }
    public void setThickness(int t) { mThickness = t; } public int getThickness() { return mThickness; }
    public void setInnerRadiusRatio(float r) { mInnerRatio = r; } public void setThicknessRatio(float r) { mThickRatio = r; }
    public void setDither(boolean d) {}
    @Override public int getIntrinsicWidth() { return mW; }
    @Override public int getIntrinsicHeight() { return mH; }
    @Override public void setAlpha(int a) { super.setAlpha(a); mAlpha = a; invalidateSelf(); }
    @Override public int getAlpha() { return mAlpha; }
    @Override public int getOpacity() { return -3; }
    @Override public boolean isStateful() { return (mSolid != null && mSolid.isStateful()) || (mStroke != null && mStroke.isStateful()); }
    @Override protected boolean onStateChange(int[] s) { return isStateful(); }
    @Override public Drawable mutate() { return this; }
    @Override public ConstantState getConstantState() {
        final GradientDrawable src = this;
        return new ConstantState() { public Drawable newDrawable() { GradientDrawable g = new GradientDrawable(); g.copyFrom(src); return g; } };
    }
    void copyFrom(GradientDrawable o) {
        mShape = o.mShape; mGradient = o.mGradient; mOrientation = o.mOrientation; mSolid = o.mSolid; mColors = o.mColors == null ? null : o.mColors.clone();
        mRadius = o.mRadius; mRadii = o.mRadii == null ? null : o.mRadii.clone(); mStrokeWidth = o.mStrokeWidth; mStroke = o.mStroke; mDashW = o.mDashW; mDashGap = o.mDashGap;
        mW = o.mW; mH = o.mH; mAlpha = o.mAlpha; mGradRadius = o.mGradRadius; mCx = o.mCx; mCy = o.mCy;
    }
    private int stateColor(ColorStateList c, int hostState) {
        int[] st = getState();
        if ((hostState & 1) != 0) st = new int[]{16842919, 16842910};
        int v = c.getColorForState(st, c.getDefaultColor());
        if (mHasTint) v = (mTint & 0xFFFFFF) | ((((mTint >>> 24) * (v >>> 24)) / 255) << 24);
        return (v & 0xFFFFFF) | ((((v >>> 24) * mAlpha) / 255) << 24);
    }
    private Shape outline(double l, double t, double w, double h) {
        switch (mShape) {
            case OVAL: return new Ellipse2D.Double(l, t, w, h);
            case LINE: return new Line2D.Double(l, t + h / 2, l + w, t + h / 2);
            case RING: {
                double cx = l + w / 2, cy = t + h / 2, minD = Math.min(w, h);
                double inner = mInnerRadius >= 0 ? mInnerRadius : minD / mInnerRatio, th = mThickness >= 0 ? mThickness : minD / mThickRatio;
                Area a = new Area(new Ellipse2D.Double(cx - inner - th, cy - inner - th, 2 * (inner + th), 2 * (inner + th)));
                a.subtract(new Area(new Ellipse2D.Double(cx - inner, cy - inner, 2 * inner, 2 * inner))); return a;
            }
            default:
                if (mRadii != null && mRadii.length >= 8) {
                    Path2D p = new Path2D.Double();
                    double tl = Math.min(mRadii[0], Math.min(w, h) / 2), tr = Math.min(mRadii[2], Math.min(w, h) / 2), br = Math.min(mRadii[4], Math.min(w, h) / 2), bl = Math.min(mRadii[6], Math.min(w, h) / 2);
                    p.moveTo(l + tl, t); p.lineTo(l + w - tr, t); p.quadTo(l + w, t, l + w, t + tr); p.lineTo(l + w, t + h - br); p.quadTo(l + w, t + h, l + w - br, t + h);
                    p.lineTo(l + bl, t + h); p.quadTo(l, t + h, l, t + h - bl); p.lineTo(l, t + tl); p.quadTo(l, t, l + tl, t); p.closePath(); return p;
                }
                if (mRadius > 0) { double r = Math.min(mRadius, Math.min(w, h) / 2); return new RoundRectangle2D.Double(l, t, w, h, 2 * r, 2 * r); }
                return new Rectangle2D.Double(l, t, w, h);
        }
    }
    private android.graphics.Paint fill(int l, int t, int w, int h, int state) {
        android.graphics.Paint out = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        if (mColors != null && mColors.length >= 2) {
            int[] cs = new int[mColors.length]; float[] fr = new float[mColors.length];
            for (int i = 0; i < cs.length; i++) { int v = mColors[i]; cs[i] = (v & 0xFFFFFF) | ((((v >>> 24) * mAlpha) / 255) << 24); fr[i] = i / (float) (cs.length - 1); }
            if (mGradient == RADIAL_GRADIENT) {
                float r = Math.max(1f, mGradRadius <= 1f ? mGradRadius * Math.min(w, h) : mGradRadius);
                out.setShader(new android.graphics.RadialGradient(l + mCx * w, t + mCy * h, r, cs, fr, android.graphics.Shader.TileMode.CLAMP)); return out;
            }
            float x0 = l, y0 = t, x1 = l, y1 = t + h;
            switch (mOrientation) {
                case TOP_BOTTOM: break; case BOTTOM_TOP: y0 = t + h; y1 = t; break;
                case LEFT_RIGHT: y1 = t; x1 = l + w; break; case RIGHT_LEFT: x0 = l + w; y1 = t; break;
                case TL_BR: x1 = l + w; break; case BR_TL: x0 = l + w; y0 = t + h; y1 = t; break;
                case TR_BL: x0 = l + w; break; case BL_TR: y0 = t + h; x1 = l + w; y1 = t; break;
            }
            if (x0 == x1 && y0 == y1) y1 += 1;
            out.setShader(new android.graphics.LinearGradient(x0, y0, x1, y1, cs, fr, android.graphics.Shader.TileMode.CLAMP)); return out;
        }
        if (mColors != null && mColors.length == 1) { out.setColor(mColors[0]); return out; }
        if (mSolid != null) { out.setColor(stateColor(mSolid, state)); return out; }
        return null;
    }
    @Override public void jetDraw(android.graphics.Canvas c, int l, int t, int r, int b, int state) {
        int w = r - l, h = b - t; if (w <= 0 || h <= 0) return;
        double inset = mStrokeWidth > 0 ? mStrokeWidth / 2.0 : 0;
        android.graphics.Paint p = fill(l, t, w, h, state);
        if (p != null && mShape != LINE) c.jetFill(outline(l, t, w, h), p);
        if (mStrokeWidth > 0 && mStroke != null) {
            android.graphics.Paint s = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG); s.setStyle(android.graphics.Paint.Style.STROKE);
            s.setColor(stateColor(mStroke, state)); s.setStrokeWidth(mStrokeWidth); s.setStrokeMiter(10f);
            if (mDashW > 0) s.setPathEffect(new android.graphics.DashPathEffect(new float[]{mDashW, Math.max(0.01f, mDashGap)}, 0));
            c.jetStroke(outline(l + inset, t + inset, w - 2 * inset, h - 2 * inset), s);
        }
    }
}
