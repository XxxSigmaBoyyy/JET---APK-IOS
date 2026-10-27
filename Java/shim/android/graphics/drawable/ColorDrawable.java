package android.graphics.drawable;
import java.awt.*;
public class ColorDrawable extends Drawable {
    public int argb; private int mAlphaMul = 255; private android.graphics.ColorFilter mCf;
    public ColorDrawable() { this(0); }
    public ColorDrawable(int argb) { this.argb = argb; }
    public int getColor() { return (argb & 0xFFFFFF) | ((((argb >>> 24) * mAlphaMul) / 255) << 24); }
    public void setColor(int c) { if (argb != c) { argb = c; invalidateSelf(); } }
    @Override public int getAlpha() { return getColor() >>> 24; }
    @Override public void setAlpha(int a) { super.setAlpha(a); int base = argb >>> 24; mAlphaMul = base == 0 ? 255 : Math.max(0, Math.min(255, a * 255 / Math.max(1, base))); invalidateSelf(); }
    @Override public int getOpacity() { int a = getColor() >>> 24; return a == 255 ? -1 : a == 0 ? -2 : -3; }
    @Override public void setColorFilter(android.graphics.ColorFilter cf) { super.setColorFilter(cf); mCf = cf; }
    @Override public void setTintList(android.content.res.ColorStateList t) { super.setTintList(t); }
    @Override public ConstantState getConstantState() { final int c = argb; return new ConstantState() { public Drawable newDrawable() { return new ColorDrawable(c); } }; }
    @Override public Drawable mutate() { return this; }
    @Override public void jetDraw(android.graphics.Canvas cv, int l, int t, int r, int b, int s) {
        int c = getColor();
        if (mHasTint) c = (mTint & 0xFFFFFF) | ((((mTint >>> 24) * (c >>> 24)) / 255) << 24);
        android.graphics.Paint p = new android.graphics.Paint(); p.setColor(c); cv.drawRect(l, t, r, b, p);
    }
}
