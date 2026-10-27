package android.graphics.drawable;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.*;
/** Android Drawable surface over Java2D. Subclasses override draw(Canvas) (app/androidx) or the JET Java2D draw (shim). */
public abstract class Drawable {
    public interface Callback {
        void invalidateDrawable(Drawable who);
        void scheduleDrawable(Drawable who, Runnable what, long when);
        void unscheduleDrawable(Drawable who, Runnable what);
    }
    public static abstract class ConstantState {
        public abstract Drawable newDrawable();
        public Drawable newDrawable(Resources r) { return newDrawable(); }
        public Drawable newDrawable(Resources r, Resources.Theme t) { return newDrawable(r); }
        public int getChangingConfigurations() { return 0; }
        public boolean canApplyTheme() { return false; }
    }
    private final Rect mBounds = new Rect();
    private int[] mState = new int[0];
    private int mLevel, mAlpha = 255, mLayoutDirection, mChanging;
    private boolean mVisible = true, mAutoMirrored, mFilterBitmap = true;
    private java.lang.ref.WeakReference<Callback> mCallback;
    protected ColorFilter mColorFilter;
    protected ColorStateList mTintList;
    protected int mTint; protected boolean mHasTint;
    private boolean mInDraw;

    public Drawable() {}
    /** JET Java2D entry; state bits: 1=pressed, 2=selected. */
    /** JET draw entry with explicit bounds; state bits: 1=pressed, 2=selected. Shim drawables override this. */
    public void jetDraw(Canvas c, int l, int t, int r, int b, int state) {
        if (mInDraw) return;
        mInDraw = true;
        Rect old = copyBounds();
        try { setBounds(l, t, r, b); draw(c); } finally { setBounds(old); mInDraw = false; }
    }
    public void draw(Canvas c) {
        if (mInDraw || c == null || c.jetGfx() == null) return;
        mInDraw = true;
        try { Rect r = getBounds(); jetDraw(c, r.left, r.top, r.right, r.bottom, 0); } finally { mInDraw = false; }
    }
    public void setBounds(int l, int t, int r, int b) {
        if (mBounds.left != l || mBounds.top != t || mBounds.right != r || mBounds.bottom != b) { mBounds.set(l, t, r, b); onBoundsChange(mBounds); }
    }
    public void setBounds(Rect r) { setBounds(r.left, r.top, r.right, r.bottom); }
    public final Rect getBounds() { return mBounds; }
    public final Rect copyBounds() { return new Rect(mBounds); }
    public final void copyBounds(Rect out) { out.set(mBounds); }
    public Rect getDirtyBounds() { return mBounds; }
    protected void onBoundsChange(Rect b) {}
    public int getIntrinsicWidth() { return -1; }
    public int getIntrinsicHeight() { return -1; }
    public int getMinimumWidth() { return Math.max(0, getIntrinsicWidth()); }
    public int getMinimumHeight() { return Math.max(0, getIntrinsicHeight()); }
    public boolean isStateful() { return false; }
    public boolean hasFocusStateSpecified() { return false; }
    public boolean setState(int[] s) { if (!java.util.Arrays.equals(mState, s)) { mState = s == null ? new int[0] : s; return onStateChange(mState); } return false; }
    public int[] getState() { return mState; }
    protected boolean onStateChange(int[] s) { return false; }
    public void jumpToCurrentState() {}
    public Drawable getCurrent() { return this; }
    public final boolean setLevel(int level) { if (mLevel != level) { mLevel = level; return onLevelChange(level); } return false; }
    public final int getLevel() { return mLevel; }
    protected boolean onLevelChange(int level) { return false; }
    public boolean setVisible(boolean v, boolean restart) { boolean ch = mVisible != v; mVisible = v; return ch; }
    public final boolean isVisible() { return mVisible; }
    public void setAlpha(int a) { mAlpha = a; }
    public int getAlpha() { return mAlpha; }
    public void setColorFilter(ColorFilter cf) { mColorFilter = cf; }
    public void setColorFilter(int color, PorterDuff.Mode mode) { setColorFilter(new PorterDuffColorFilter(color, mode)); }
    public ColorFilter getColorFilter() { return mColorFilter; }
    public void clearColorFilter() { setColorFilter(null); }
    public void setTint(int c) { mTint = c; mHasTint = true; }
    public void setTintList(ColorStateList l) { mTintList = l; if (l != null) setTint(l.getColorForState(new int[]{16842910}, l.getDefaultColor())); }
    public void setTintMode(PorterDuff.Mode m) {}
    public int getOpacity() { return -3; }
    public static int resolveOpacity(int a, int b) { return a == b ? a : -3; }
    public boolean getPadding(Rect p) { p.set(0, 0, 0, 0); return false; }
    public Drawable mutate() { return this; }
    public ConstantState getConstantState() { if (!getClass().getName().startsWith("android.")) return null; final Drawable self = this; return new ConstantState() { public Drawable newDrawable() { return self; } }; } // JET approximation: framework drawables share the instance
    public final void setCallback(Callback cb) { mCallback = cb == null ? null : new java.lang.ref.WeakReference<>(cb); }
    public Callback getCallback() { return mCallback == null ? null : mCallback.get(); }
    public void invalidateSelf() { Callback c = getCallback(); if (c != null) c.invalidateDrawable(this); }
    public void scheduleSelf(Runnable what, long when) { Callback c = getCallback(); if (c != null) c.scheduleDrawable(this, what, when); }
    public void unscheduleSelf(Runnable what) { Callback c = getCallback(); if (c != null) c.unscheduleDrawable(this, what); }
    public void setDither(boolean d) {}
    public void setFilterBitmap(boolean f) { mFilterBitmap = f; }
    public boolean isFilterBitmap() { return mFilterBitmap; }
    public void setAutoMirrored(boolean m) { mAutoMirrored = m; }
    public boolean isAutoMirrored() { return mAutoMirrored; }
    public boolean setLayoutDirection(int d) { mLayoutDirection = d; return onLayoutDirectionChanged(d); }
    public int getLayoutDirection() { return mLayoutDirection; }
    public boolean onLayoutDirectionChanged(int d) { return false; }
    public void setHotspot(float x, float y) {}
    public void setHotspotBounds(int l, int t, int r, int b) {}
    public void getHotspotBounds(Rect out) { out.set(mBounds); }
    public boolean isProjected() { return false; }
    public void applyTheme(Resources.Theme t) {}
    public boolean canApplyTheme() { return false; }
    public int getChangingConfigurations() { return mChanging; }
    public void setChangingConfigurations(int c) { mChanging = c; }
    public static Drawable createFromPath(String p) { return null; }
    public static Drawable createFromStream(java.io.InputStream is, String src) { return null; }
}
