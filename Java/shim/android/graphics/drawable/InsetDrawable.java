package android.graphics.drawable;
import android.graphics.Rect;
public class InsetDrawable extends DrawableWrapper {
    private int l, t, r, b;
    public InsetDrawable(Drawable d, int inset) { this(d, inset, inset, inset, inset); }
    public InsetDrawable(Drawable d, float f) { this(d, 0); }
    public InsetDrawable(Drawable d, float fl, float ft, float fr, float fb) { this(d, 0); }
    public InsetDrawable(Drawable d, int l, int t, int r, int b) { super(null); this.l = l; this.t = t; this.r = r; this.b = b; setDrawable(d); }
    @Override protected void onBoundsChange(Rect bb) { Drawable d = getDrawable(); if (d != null) d.setBounds(bb.left + l, bb.top + t, bb.right - r, bb.bottom - b); }
    @Override public int getIntrinsicWidth() { int w = super.getIntrinsicWidth(); return w < 0 ? -1 : w + l + r; }
    @Override public int getIntrinsicHeight() { int h = super.getIntrinsicHeight(); return h < 0 ? -1 : h + t + b; }
    @Override public boolean getPadding(Rect p) { boolean pad = super.getPadding(p); p.left += l; p.top += t; p.right += r; p.bottom += b; return pad || (l | t | r | b) != 0; }
}
