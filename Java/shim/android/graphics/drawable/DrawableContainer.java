package android.graphics.drawable;
/** android.graphics.drawable.DrawableContainer: base of StateListDrawable/LevelListDrawable/AnimationDrawable. JET keeps fade durations
 *  and the selected child index; subclasses decide which child is current. */
public class DrawableContainer extends Drawable implements Drawable.Callback {
    private int mEnterFade, mExitFade, mCurIndex = -1; private boolean mDither, mAutoMirrored;
    private DrawableContainerState mState;
    public DrawableContainer() {}
    public void setEnterFadeDuration(int ms) { mEnterFade = ms; }
    public void setExitFadeDuration(int ms) { mExitFade = ms; }
    public void setDither(boolean dither) { mDither = dither; }
    public void setAutoMirrored(boolean m) { mAutoMirrored = m; }
    public boolean isAutoMirrored() { return mAutoMirrored; }
    /** Index of the child to show; -1 = none. */
    public boolean selectDrawable(int index) { if (index == mCurIndex) return false; mCurIndex = index; invalidateSelf(); return true; }
    public int jetCurrentIndex() { return mCurIndex; }
    protected void setConstantState(DrawableContainerState state) { mState = state; }
    public void invalidateDrawable(Drawable who) { invalidateSelf(); }
    public void scheduleDrawable(Drawable who, Runnable what, long when) { scheduleSelf(what, when); }
    public void unscheduleDrawable(Drawable who, Runnable what) { unscheduleSelf(what); }
    public void draw(android.graphics.Canvas canvas) { Drawable c = getCurrent(); if (c != null && c != this) { c.setBounds(getBounds()); c.draw(canvas); } }
    public void setAlpha(int alpha) {}
    public void setColorFilter(android.graphics.ColorFilter cf) {}
    public int getOpacity() { return -3; /* TRANSLUCENT */ }
    public abstract static class DrawableContainerState extends Drawable.ConstantState {
        public final int getChildCount() { return 0; }
        public int getChangingConfigurations() { return 0; }
    }
}
