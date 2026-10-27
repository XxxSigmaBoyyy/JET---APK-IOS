package android.graphics.drawable;
import android.graphics.*;
public class BitmapDrawable extends Drawable {
    /** Pixels drawn (the bitmap's backend-neutral store). */
    public jet.gfx.PixelBuffer img;
    private Bitmap mBitmap; private int mTargetDensity = 320; private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private int mGravity = android.view.Gravity.FILL;
    public BitmapDrawable(jet.gfx.PixelBuffer i) { img = i; mBitmap = i == null ? null : Bitmap.jetWrap(i, false); }
    public BitmapDrawable() { }
    public BitmapDrawable(Bitmap b) { setBitmap(b); }
    public BitmapDrawable(android.content.res.Resources r, Bitmap b) { if (r != null) mTargetDensity = r.getDisplayMetrics().densityDpi; setBitmap(b); }
    public BitmapDrawable(android.content.res.Resources r) { if (r != null) mTargetDensity = r.getDisplayMetrics().densityDpi; }
    public BitmapDrawable(android.content.res.Resources r, String file) { this(r, BitmapFactory.decodeFile(file)); }
    public BitmapDrawable(android.content.res.Resources r, java.io.InputStream in) { this(r, BitmapFactory.decodeStream(in)); }
    public BitmapDrawable(String file) { this(null, BitmapFactory.decodeFile(file)); }
    public BitmapDrawable(java.io.InputStream in) { this(null, BitmapFactory.decodeStream(in)); }
    private void setBitmap(Bitmap b) { mBitmap = b; img = b == null ? null : b.pb; }
    public final Bitmap getBitmap() { return mBitmap; }
    public final Paint getPaint() { return mPaint; }
    public void setTargetDensity(int d) { mTargetDensity = d == 0 ? 160 : d; }
    public void setTargetDensity(android.util.DisplayMetrics m) { setTargetDensity(m.densityDpi); }
    public void setTargetDensity(Canvas c) {}
    public int getGravity() { return mGravity; }
    public void setGravity(int g) { mGravity = g; }
    public void setAntiAlias(boolean a) { mPaint.setAntiAlias(a); }
    public boolean hasAntiAlias() { return mPaint.isAntiAlias(); }
    public void setMipMap(boolean m) {}
    public boolean hasMipMap() { return false; }
    public void setTileModeXY(Shader.TileMode x, Shader.TileMode y) {}
    public void setTileModeX(Shader.TileMode m) {} public void setTileModeY(Shader.TileMode m) {}
    @Override public int getIntrinsicWidth() { if (mBitmap == null) return -1; return img != null && mBitmap.pb != img ? img.w : mBitmap.getScaledWidth(mTargetDensity); }
    @Override public int getIntrinsicHeight() { if (mBitmap == null) return -1; return img != null && mBitmap.pb != img ? img.h : mBitmap.getScaledHeight(mTargetDensity); }
    @Override public int getOpacity() { return mBitmap == null || mBitmap.hasAlpha() ? -3 /*TRANSLUCENT*/ : -1 /*OPAQUE*/; }
    @Override public void jetDraw(android.graphics.Canvas c, int l, int t, int r, int b, int s) {
        if (img == null || r <= l || b <= t) return;
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG); p.setAlpha(Math.max(0, Math.min(255, getAlpha())));
        java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getTranslateInstance(l, t); at.scale((r - l) / (double) img.w, (b - t) / (double) img.h);
        c.jetDrawImage(img, at, p);
    }
    @Override public ConstantState getConstantState() {
        final Bitmap bm = mBitmap; final int td = mTargetDensity;
        return new ConstantState() { public Drawable newDrawable() { BitmapDrawable d = new BitmapDrawable(bm); d.mTargetDensity = td; return d; } };
    }
}
