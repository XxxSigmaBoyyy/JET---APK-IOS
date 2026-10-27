package android.graphics.drawable;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
public abstract class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable mDrawable;
    public DrawableWrapper(Drawable d) { setDrawable(d); }
    public void setDrawable(Drawable d) { if (mDrawable != null) mDrawable.setCallback(null); mDrawable = d; if (d != null) { d.setCallback(this); onBoundsChange(getBounds()); } invalidateSelf(); }
    public Drawable getDrawable() { return mDrawable; }
    @Override public void draw(Canvas c) { if (mDrawable != null) mDrawable.draw(c); }
    @Override protected void onBoundsChange(Rect b) { if (mDrawable != null) mDrawable.setBounds(b); }
    @Override public int getIntrinsicWidth() { return mDrawable == null ? -1 : mDrawable.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return mDrawable == null ? -1 : mDrawable.getIntrinsicHeight(); }
    @Override public boolean getPadding(Rect p) { return mDrawable != null ? mDrawable.getPadding(p) : super.getPadding(p); }
    @Override public boolean isStateful() { return mDrawable != null && mDrawable.isStateful(); }
    @Override protected boolean onStateChange(int[] s) { return mDrawable != null && mDrawable.isStateful() && mDrawable.setState(s); }
    @Override protected boolean onLevelChange(int l) { return mDrawable != null && mDrawable.setLevel(l); }
    @Override public void setAlpha(int a) { super.setAlpha(a); if (mDrawable != null) mDrawable.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { super.setColorFilter(cf); if (mDrawable != null) mDrawable.setColorFilter(cf); }
    @Override public void setTintList(android.content.res.ColorStateList t) { if (mDrawable != null) mDrawable.setTintList(t); }
    @Override public void setTintMode(android.graphics.PorterDuff.Mode m) { if (mDrawable != null) mDrawable.setTintMode(m); }
    @Override public boolean setVisible(boolean v, boolean r) { boolean ch = super.setVisible(v, r); if (mDrawable != null) mDrawable.setVisible(v, r); return ch; }
    @Override public int getOpacity() { return mDrawable == null ? -2 : mDrawable.getOpacity(); }
    @Override public Drawable mutate() { if (mDrawable != null) mDrawable.mutate(); return this; }
    public void invalidateDrawable(Drawable who) { invalidateSelf(); }
    public void scheduleDrawable(Drawable who, Runnable what, long when) { scheduleSelf(what, when); }
    public void unscheduleDrawable(Drawable who, Runnable what) { unscheduleSelf(what); }
}
