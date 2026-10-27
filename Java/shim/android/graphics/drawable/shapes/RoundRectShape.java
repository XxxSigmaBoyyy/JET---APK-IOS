package android.graphics.drawable.shapes;
public class RoundRectShape extends RectShape {
    private final float[] mOuter; private final android.graphics.RectF mInset; private final float[] mInner; private final android.graphics.Path mPath = new android.graphics.Path();
    public RoundRectShape(float[] outer, android.graphics.RectF inset, float[] inner) { mOuter = outer; mInset = inset; mInner = inner; }
    @Override public void draw(android.graphics.Canvas c, android.graphics.Paint p) { c.drawPath(mPath, p); }
    @Override protected void onResize(float w, float h) {
        super.onResize(w, h); mPath.reset();
        mPath.addRoundRect(rect(), mOuter != null ? mOuter : new float[8], android.graphics.Path.Direction.CW);
        if (mInset != null) { android.graphics.RectF r = new android.graphics.RectF(mInset.left, mInset.top, w - mInset.right, h - mInset.bottom); android.graphics.Path in = new android.graphics.Path(); in.addRoundRect(r, mInner != null ? mInner : new float[8], android.graphics.Path.Direction.CCW); mPath.op(in, android.graphics.Path.Op.DIFFERENCE); }
    }
}
