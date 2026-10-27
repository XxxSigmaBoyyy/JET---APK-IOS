package android.graphics.drawable;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
/** Static ripple: draws content layers; ripple/mask only matter while pressed (no animation on host). */
public class RippleDrawable extends LayerDrawable {
    public static final int RADIUS_AUTO = -1;
    private ColorStateList mColor; private int mRadius = RADIUS_AUTO; private Drawable mMask;
    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask) {
        super(content == null ? new Drawable[0] : new Drawable[] { content }); mColor = color; mMask = mask;
        if (mask != null) { int i = addLayer(mask); setId(i, 0x0102002e); }
    }
    @Override public void draw(Canvas c) { for (Layer L : mLayers) if (L.d != null && L.d != mMask && L.d.isVisible()) L.d.draw(c); }
    public void setColor(ColorStateList c) { mColor = c; invalidateSelf(); }
    public ColorStateList getEffectColor() { return mColor; } public void setEffectColor(ColorStateList c) { mColor = c; }
    public void setRadius(int r) { mRadius = r; } public int getRadius() { return mRadius; }
    @Override public boolean isStateful() { return true; }
    @Override public boolean setDrawableByLayerId(int id, Drawable d) { if (id == 0x0102002e) mMask = d; return super.setDrawableByLayerId(id, d); }
    public boolean isProjected() { return false; }
}
