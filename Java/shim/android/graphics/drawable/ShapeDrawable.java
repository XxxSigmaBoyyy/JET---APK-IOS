package android.graphics.drawable;
import android.graphics.*;
import android.graphics.drawable.shapes.Shape;
public class ShapeDrawable extends Drawable {
    public static abstract class ShaderFactory { public abstract Shader resize(int w, int h); }
    private Shape mShape; private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG); private int mIntrinsicW = -1, mIntrinsicH = -1; private Rect mPadding; private ShaderFactory mShaderFactory;
    public ShapeDrawable() { this(null); }
    public ShapeDrawable(Shape s) { mShape = s; mPaint.setColor(0xFF000000); }
    public Shape getShape() { return mShape; } public void setShape(Shape s) { mShape = s; onBoundsChange(getBounds()); invalidateSelf(); }
    public Paint getPaint() { return mPaint; }
    public void setShaderFactory(ShaderFactory f) { mShaderFactory = f; } public ShaderFactory getShaderFactory() { return mShaderFactory; }
    public void setPadding(int l, int t, int r, int b) { mPadding = (l | t | r | b) == 0 ? null : new Rect(l, t, r, b); } public void setPadding(Rect p) { mPadding = p == null ? null : new Rect(p); }
    @Override public boolean getPadding(Rect p) { if (mPadding != null) { p.set(mPadding); return true; } return super.getPadding(p); }
    public void setIntrinsicWidth(int w) { mIntrinsicW = w; } public void setIntrinsicHeight(int h) { mIntrinsicH = h; }
    @Override public int getIntrinsicWidth() { return mIntrinsicW; } @Override public int getIntrinsicHeight() { return mIntrinsicH; }
    @Override protected void onBoundsChange(Rect b) { if (mShape != null) mShape.resize(b.width(), b.height()); if (mShaderFactory != null) mPaint.setShader(mShaderFactory.resize(b.width(), b.height())); }
    @Override public void setAlpha(int a) { super.setAlpha(a); mPaint.setAlpha(a); }
    @Override public int getAlpha() { return mPaint.getAlpha(); }
    @Override public void setColorFilter(ColorFilter cf) { super.setColorFilter(cf); mPaint.setColorFilter(cf); }
    @Override public void setTint(int c) { super.setTint(c); mPaint.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN)); }
    @Override public int getOpacity() { return -3; }
    protected void onDraw(Shape s, Canvas c, Paint p) { s.draw(c, p); }
    @Override public void draw(Canvas c) {
        Rect r = getBounds(); int n = c.save(); c.translate(r.left, r.top);
        if (mShape != null) onDraw(mShape, c, mPaint); else c.drawRect(0, 0, r.width(), r.height(), mPaint);
        c.restoreToCount(n);
    }
    @Override public Drawable mutate() { return this; }
}
