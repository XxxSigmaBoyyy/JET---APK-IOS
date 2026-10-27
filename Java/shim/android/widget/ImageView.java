package android.widget;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import jet.runtime.XmlAttrs;
public class ImageView extends View {
    public enum ScaleType { MATRIX, FIT_XY, FIT_START, FIT_CENTER, FIT_END, CENTER, CENTER_CROP, CENTER_INSIDE }
    private Drawable drawable; private ScaleType mScale = ScaleType.FIT_CENTER; private boolean mAdjust, mCropToPadding; private int mMaxW = Integer.MAX_VALUE, mMaxH = Integer.MAX_VALUE, mBaseline = -1, mAlpha = 255, mResource, mLevel;
    private android.content.res.ColorStateList mTint; private android.graphics.PorterDuff.Mode mTintMode; private android.graphics.ColorFilter mFilter; private android.graphics.Matrix mMatrix = new android.graphics.Matrix(); private android.net.Uri mUri; private boolean mBaselineAlignBottom;
    public ImageView(Context c) { this(c, null); }
    public ImageView(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public ImageView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }
    public ImageView(Context c, AttributeSet a) {
        super(c, a);
        XmlAttrs x = XmlAttrs.of(a);
        if (x != null) {
            String src = x.get("src"); if (src == null) src = x.get("srcCompat");
            try { if (src != null) drawable = res().drawable(src); } catch (RuntimeException ignored) {}
            String st = x.get("scaleType"); if (st != null) try { int i = jet.runtime.ResTable.intVal(res().resolve(st)); if (i >= 0 && i < 8) mScale = ScaleType.values()[i]; } catch (RuntimeException ignored) {}
            mAdjust = "true".equals(x.get("adjustViewBounds"));
            try { if (x.get("maxWidth") != null) mMaxW = (int) res().dimen(x.get("maxWidth")); if (x.get("maxHeight") != null) mMaxH = (int) res().dimen(x.get("maxHeight")); } catch (RuntimeException ignored) {}
        }
    }
    public void setImageDrawable(Drawable d) { if (d == drawable) return; drawable = d; mResource = 0; if (d != null) { d.setLevel(mLevel); if (mTint != null) d.setTintList(mTint); } requestLayout(); invalidate(); }
    public void setImageBitmap(Bitmap b) { setImageDrawable(b == null ? null : new BitmapDrawable(b)); }
    public void setImageResource(int id) { Drawable d = id == 0 ? null : res().drawable("@" + Integer.toHexString(id)); setImageDrawable(d); mResource = id; }
    public void setImageURI(android.net.Uri u) { mUri = u; setImageDrawable(null); }
    public void setImageIcon(android.graphics.drawable.Icon i) { setImageDrawable(null); }
    public Drawable getDrawable() { return drawable; }
    public void setScaleType(ScaleType s) { if (s == null) throw new NullPointerException(); mScale = s; requestLayout(); invalidate(); } public ScaleType getScaleType() { return mScale; }
    public void setAdjustViewBounds(boolean b) { mAdjust = b; if (b) mScale = ScaleType.FIT_CENTER; requestLayout(); } public boolean getAdjustViewBounds() { return mAdjust; }
    public void setMaxWidth(int w) { mMaxW = w; } public int getMaxWidth() { return mMaxW; } public void setMaxHeight(int h) { mMaxH = h; } public int getMaxHeight() { return mMaxH; }
    public void setCropToPadding(boolean b) { mCropToPadding = b; } public boolean getCropToPadding() { return mCropToPadding; }
    public void setBaseline(int b) { mBaseline = b; } @Override public int getBaseline() { return mBaselineAlignBottom ? getMeasuredHeight() : mBaseline; } public void setBaselineAlignBottom(boolean b) { mBaselineAlignBottom = b; } public boolean getBaselineAlignBottom() { return mBaselineAlignBottom; }
    public void setImageLevel(int l) { mLevel = l; if (drawable != null) drawable.setLevel(l); } public void setImageState(int[] s, boolean merge) {}
    public void setImageTintList(android.content.res.ColorStateList t) { mTint = t; if (drawable != null) drawable.setTintList(t); invalidate(); } public android.content.res.ColorStateList getImageTintList() { return mTint; }
    public void setImageTintMode(android.graphics.PorterDuff.Mode m) { mTintMode = m; } public android.graphics.PorterDuff.Mode getImageTintMode() { return mTintMode; }
    public void setImageTintBlendMode(android.graphics.BlendMode m) {} public android.graphics.BlendMode getImageTintBlendMode() { return null; }
    public final void setColorFilter(int color, android.graphics.PorterDuff.Mode m) { setColorFilter(new android.graphics.PorterDuffColorFilter(color, m)); } public final void setColorFilter(int color) { setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP); }
    public void setColorFilter(android.graphics.ColorFilter f) { mFilter = f; if (drawable != null) drawable.setColorFilter(f); invalidate(); } public android.graphics.ColorFilter getColorFilter() { return mFilter; } public final void clearColorFilter() { setColorFilter((android.graphics.ColorFilter) null); }
    public void setImageAlpha(int a) { mAlpha = a; invalidate(); } public int getImageAlpha() { return mAlpha; } @Deprecated public void setAlpha(int a) { setImageAlpha(a); }
    public android.graphics.Matrix getImageMatrix() { return mMatrix; } public void setImageMatrix(android.graphics.Matrix m) { mMatrix = m == null ? new android.graphics.Matrix() : m; invalidate(); }
    public void animateTransform(android.graphics.Matrix m) { setImageMatrix(m); }
    public boolean isOpaque() { return false; }
    private int iw() { return drawable == null ? -1 : drawable.getIntrinsicWidth(); } private int ih() { return drawable == null ? -1 : drawable.getIntrinsicHeight(); }
    @Override protected void onMeasure(int ws, int hs) {
        int w = Math.max(0, iw()), h = Math.max(0, ih());
        int pw = w + mPaddingLeft + mPaddingRight, ph = h + mPaddingTop + mPaddingBottom;
        int rw = resolve(pw, Math.min(mMaxW, Integer.MAX_VALUE), ws), rh = resolve(ph, mMaxH, hs);
        if (mAdjust && w > 0 && h > 0) {
            boolean exW = MeasureSpec.getMode(ws) == MeasureSpec.EXACTLY, exH = MeasureSpec.getMode(hs) == MeasureSpec.EXACTLY;
            float ar = w / (float) h;
            if (!exH && exW) rh = Math.min(resolve(Math.round((rw - mPaddingLeft - mPaddingRight) / ar) + mPaddingTop + mPaddingBottom, mMaxH, hs), rh == 0 ? Integer.MAX_VALUE : Math.max(rh, Math.round((rw - mPaddingLeft - mPaddingRight) / ar) + mPaddingTop + mPaddingBottom));
            else if (!exW && exH) rw = resolve(Math.round((rh - mPaddingTop - mPaddingBottom) * ar) + mPaddingLeft + mPaddingRight, mMaxW, ws);
        }
        setMeasuredDimension(rw, rh);
    }
    private static int resolve(int desired, int max, int spec) {
        int m = MeasureSpec.getMode(spec), s = MeasureSpec.getSize(spec); desired = Math.min(desired, max);
        return m == MeasureSpec.EXACTLY ? s : m == MeasureSpec.AT_MOST ? Math.min(desired, s) : desired;
    }
    @Override protected void onDraw(android.graphics.Canvas g) {
        if (drawable == null || mAlpha == 0) return;
        int l = mPaddingLeft, t = mPaddingTop, r = getWidth() - mPaddingRight, b = getHeight() - mPaddingBottom, aw = r - l, ah = b - t;
        int dw = iw(), dh = ih();
        int sc = g.save(); g.clipRect(l, t, l + Math.max(0, aw), t + Math.max(0, ah));
        if (dw <= 0 || dh <= 0 || mScale == ScaleType.FIT_XY) { drawable.jetDraw(g, l, t, r, b, drawState()); g.restoreToCount(sc); return; }
        float sx = aw / (float) dw, sy = ah / (float) dh, s;
        switch (mScale) {
            case CENTER: s = 1; break;
            case CENTER_CROP: s = Math.max(sx, sy); break;
            case CENTER_INSIDE: s = Math.min(1f, Math.min(sx, sy)); break;
            case MATRIX: s = 1; break;
            default: s = Math.min(sx, sy);
        }
        int w = Math.round(dw * s), h = Math.round(dh * s), x, y;
        if (mScale == ScaleType.FIT_START || mScale == ScaleType.MATRIX) { x = l; y = t; } else if (mScale == ScaleType.FIT_END) { x = r - w; y = b - h; } else { x = l + (aw - w) / 2; y = t + (ah - h) / 2; }
        drawable.jetDraw(g, x, y, x + w, y + h, drawState());
        g.restoreToCount(sc);
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    protected boolean setFrame(int p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.widget.ImageView.setFrame(IIII)Z"); return false; }
    public void onVisibilityAggregated(boolean p0) { jet.runtime.ShimStubs.hit("android.widget.ImageView.onVisibilityAggregated(Z)V"); }
    public java.lang.CharSequence getAccessibilityClassName() { jet.runtime.ShimStubs.hit("android.widget.ImageView.getAccessibilityClassName()Ljava/lang/CharSequence;"); return null; }
    // ---- END generated SDK surface ----
}
