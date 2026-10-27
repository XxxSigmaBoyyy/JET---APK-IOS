package android.graphics.drawable;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import java.util.ArrayList;
public class LayerDrawable extends Drawable implements Drawable.Callback {
    public static final int INSET_UNDEFINED = Integer.MIN_VALUE, PADDING_MODE_NEST = 0, PADDING_MODE_STACK = 1;
    static final class Layer { Drawable d; int id = -1, l, t, r, b, s = INSET_UNDEFINED, e = INSET_UNDEFINED, w = -1, h = -1, gravity = 0; }
    final ArrayList<Layer> mLayers = new ArrayList<>(); private int mPaddingMode = PADDING_MODE_NEST;
    private int mPadL = -1, mPadT = -1, mPadR = -1, mPadB = -1;
    public LayerDrawable(Drawable[] layers) { if (layers == null) throw new IllegalArgumentException("layers must be non-null"); for (Drawable d : layers) addLayer(d); }
    LayerDrawable() {}
    public int addLayer(Drawable d) { Layer L = new Layer(); L.d = d; if (d != null) d.setCallback(this); mLayers.add(L); onBoundsChange(getBounds()); return mLayers.size() - 1; }
    public int getNumberOfLayers() { return mLayers.size(); }
    public Drawable getDrawable(int i) { return mLayers.get(i).d; }
    public void setDrawable(int i, Drawable d) { mLayers.get(i).d = d; if (d != null) d.setCallback(this); onBoundsChange(getBounds()); invalidateSelf(); }
    public Drawable findDrawableByLayerId(int id) { for (int i = mLayers.size() - 1; i >= 0; i--) if (mLayers.get(i).id == id) return mLayers.get(i).d; return null; }
    public boolean setDrawableByLayerId(int id, Drawable d) { for (Layer L : mLayers) if (L.id == id) { L.d = d; if (d != null) d.setCallback(this); onBoundsChange(getBounds()); return true; } return false; }
    public int findIndexByLayerId(int id) { for (int i = 0; i < mLayers.size(); i++) if (mLayers.get(i).id == id) return i; return -1; }
    public void setId(int i, int id) { mLayers.get(i).id = id; } public int getId(int i) { return mLayers.get(i).id; }
    public void setLayerInset(int i, int l, int t, int r, int b) { Layer L = mLayers.get(i); L.l = l; L.t = t; L.r = r; L.b = b; onBoundsChange(getBounds()); }
    public void setLayerInsetRelative(int i, int s, int t, int e, int b) { Layer L = mLayers.get(i); L.s = s; L.t = t; L.e = e; L.b = b; onBoundsChange(getBounds()); }
    public void setLayerInsetLeft(int i, int v) { mLayers.get(i).l = v; onBoundsChange(getBounds()); } public int getLayerInsetLeft(int i) { return mLayers.get(i).l; }
    public void setLayerInsetTop(int i, int v) { mLayers.get(i).t = v; onBoundsChange(getBounds()); } public int getLayerInsetTop(int i) { return mLayers.get(i).t; }
    public void setLayerInsetRight(int i, int v) { mLayers.get(i).r = v; onBoundsChange(getBounds()); } public int getLayerInsetRight(int i) { return mLayers.get(i).r; }
    public void setLayerInsetBottom(int i, int v) { mLayers.get(i).b = v; onBoundsChange(getBounds()); } public int getLayerInsetBottom(int i) { return mLayers.get(i).b; }
    public void setLayerInsetStart(int i, int v) { mLayers.get(i).s = v; onBoundsChange(getBounds()); } public int getLayerInsetStart(int i) { return mLayers.get(i).s; }
    public void setLayerInsetEnd(int i, int v) { mLayers.get(i).e = v; onBoundsChange(getBounds()); } public int getLayerInsetEnd(int i) { return mLayers.get(i).e; }
    public void setLayerSize(int i, int w, int h) { Layer L = mLayers.get(i); L.w = w; L.h = h; onBoundsChange(getBounds()); }
    public void setLayerWidth(int i, int w) { mLayers.get(i).w = w; } public int getLayerWidth(int i) { return mLayers.get(i).w; }
    public void setLayerHeight(int i, int h) { mLayers.get(i).h = h; } public int getLayerHeight(int i) { return mLayers.get(i).h; }
    public void setLayerGravity(int i, int g) { mLayers.get(i).gravity = g; } public int getLayerGravity(int i) { return mLayers.get(i).gravity; }
    public void setPaddingMode(int m) { mPaddingMode = m; } public int getPaddingMode() { return mPaddingMode; }
    public void setPadding(int l, int t, int r, int b) { mPadL = l; mPadT = t; mPadR = r; mPadB = b; }
    public void setPaddingRelative(int s, int t, int e, int b) { setPadding(s, t, e, b); }
    public int getLeftPadding() { return mPadL; } public int getTopPadding() { return mPadT; } public int getRightPadding() { return mPadR; } public int getBottomPadding() { return mPadB; }
    public int getStartPadding() { return mPadL; } public int getEndPadding() { return mPadR; }
    @Override protected void onBoundsChange(Rect b) {
        for (Layer L : mLayers) {
            if (L.d == null) continue;
            int l = L.s != INSET_UNDEFINED ? L.s : L.l, r = L.e != INSET_UNDEFINED ? L.e : L.r;
            int x0 = b.left + l, y0 = b.top + L.t, x1 = b.right - r, y1 = b.bottom - L.b;
            int w = L.w >= 0 ? L.w : (L.gravity != 0 && L.d.getIntrinsicWidth() > 0 ? L.d.getIntrinsicWidth() : x1 - x0);
            int h = L.h >= 0 ? L.h : (L.gravity != 0 && L.d.getIntrinsicHeight() > 0 ? L.d.getIntrinsicHeight() : y1 - y0);
            int hg = L.gravity & 7, vg = L.gravity & 0x70;
            int x = hg == 1 ? x0 + (x1 - x0 - w) / 2 : hg == 5 ? x1 - w : x0;
            int y = vg == 0x10 ? y0 + (y1 - y0 - h) / 2 : vg == 0x50 ? y1 - h : y0;
            L.d.setBounds(x, y, x + w, y + h);
        }
    }
    @Override public void draw(Canvas c) { for (Layer L : mLayers) if (L.d != null && L.d.isVisible()) L.d.draw(c); }
    @Override public int getIntrinsicWidth() { int m = -1; for (Layer L : mLayers) if (L.d != null) { int w = L.w >= 0 ? L.w : L.d.getIntrinsicWidth(); if (w >= 0) m = Math.max(m, w + L.l + L.r); } return m; }
    @Override public int getIntrinsicHeight() { int m = -1; for (Layer L : mLayers) if (L.d != null) { int h = L.h >= 0 ? L.h : L.d.getIntrinsicHeight(); if (h >= 0) m = Math.max(m, h + L.t + L.b); } return m; }
    @Override public boolean getPadding(Rect p) {
        if (mPadL >= 0) { p.set(mPadL, mPadT, mPadR, mPadB); return true; }
        p.set(0, 0, 0, 0); Rect t = new Rect();
        for (Layer L : mLayers) if (L.d != null && L.d.getPadding(t)) { p.left = Math.max(p.left, t.left); p.top = Math.max(p.top, t.top); p.right = Math.max(p.right, t.right); p.bottom = Math.max(p.bottom, t.bottom); }
        return p.left != 0 || p.top != 0 || p.right != 0 || p.bottom != 0;
    }
    @Override public boolean isStateful() { for (Layer L : mLayers) if (L.d != null && L.d.isStateful()) return true; return false; }
    @Override protected boolean onStateChange(int[] s) { boolean ch = false; for (Layer L : mLayers) if (L.d != null) ch |= L.d.setState(s); return ch; }
    @Override protected boolean onLevelChange(int level) { boolean ch = false; for (Layer L : mLayers) if (L.d != null) ch |= L.d.setLevel(level); return ch; }
    @Override public boolean setVisible(boolean v, boolean r) { boolean ch = super.setVisible(v, r); for (Layer L : mLayers) if (L.d != null) L.d.setVisible(v, r); return ch; }
    @Override public void setAlpha(int a) { super.setAlpha(a); for (Layer L : mLayers) if (L.d != null) L.d.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { super.setColorFilter(cf); for (Layer L : mLayers) if (L.d != null) L.d.setColorFilter(cf); }
    @Override public void setTintList(android.content.res.ColorStateList t) { for (Layer L : mLayers) if (L.d != null) L.d.setTintList(t); }
    @Override public void setTintMode(android.graphics.PorterDuff.Mode m) { for (Layer L : mLayers) if (L.d != null) L.d.setTintMode(m); }
    @Override public void setHotspot(float x, float y) { for (Layer L : mLayers) if (L.d != null) L.d.setHotspot(x, y); }
    @Override public void jumpToCurrentState() { for (Layer L : mLayers) if (L.d != null) L.d.jumpToCurrentState(); }
    @Override public Drawable mutate() { for (Layer L : mLayers) if (L.d != null) L.d.mutate(); return this; }
    @Override public boolean onLayoutDirectionChanged(int d) { boolean ch = false; for (Layer L : mLayers) if (L.d != null) ch |= L.d.setLayoutDirection(d); return ch; }
    @Override public int getOpacity() { return -3; }
    @Override public void applyTheme(Resources.Theme t) { for (Layer L : mLayers) if (L.d != null) L.d.applyTheme(t); }
    public void invalidateDrawable(Drawable who) { invalidateSelf(); }
    public void scheduleDrawable(Drawable who, Runnable what, long when) { scheduleSelf(what, when); }
    public void unscheduleDrawable(Drawable who, Runnable what) { unscheduleSelf(what); }
}
