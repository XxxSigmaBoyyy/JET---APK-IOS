package android.widget;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import jet.runtime.XmlAttrs;
/** AOSP CheckedTextView: a TextView with a check-mark drawable at the end, state_checked; toggles are explicit (ListView choice mode). */
public class CheckedTextView extends TextView implements Checkable {
    private static final int STATE_CHECKED = 16842912;
    private boolean mChecked; private Drawable mCheckMark; private ColorStateList mTint; private android.graphics.PorterDuff.Mode mMode; private android.graphics.BlendMode mBlend;
    public CheckedTextView(Context c) { this(c, null); }
    public CheckedTextView(Context c, AttributeSet a) { this(c, a, 0x010103c8 /*checkedTextViewStyle*/); }
    public CheckedTextView(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public CheckedTextView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        XmlAttrs x = XmlAttrs.of(a);
        if (x != null) { try { String v; if ((v = x.get("checkMark")) != null && v.startsWith("@") && !"@null".equals(v)) setCheckMarkDrawable(res().drawable(v)); if ((v = x.get("checked")) != null) setChecked("true".equals(res().resolve(v))); } catch (RuntimeException ignored) {} }
    }
    public void toggle() { setChecked(!mChecked); }
    public boolean isChecked() { return mChecked; }
    public void setChecked(boolean checked) { if (mChecked != checked) { mChecked = checked; refreshDrawableState(); invalidate(); } }
    public void setCheckMarkDrawable(int resId) { setCheckMarkDrawable(resId == 0 ? null : getContext().getDrawable(resId)); }
    public void setCheckMarkDrawable(Drawable d) { mCheckMark = d; if (d != null) { applyTint(); if (d.isStateful()) d.setState(getDrawableState()); setMinHeight(d.getIntrinsicHeight()); } requestLayout(); invalidate(); }
    private void applyTint() { if (mCheckMark != null && (mTint != null || mMode != null)) { mCheckMark = mCheckMark.mutate(); if (mTint != null) mCheckMark.setTintList(mTint); if (mMode != null) mCheckMark.setTintMode(mMode); } }
    public void setCheckMarkTintList(ColorStateList t) { mTint = t; applyTint(); } public ColorStateList getCheckMarkTintList() { return mTint; }
    public void setCheckMarkTintMode(android.graphics.PorterDuff.Mode m) { mMode = m; applyTint(); } public void setCheckMarkTintBlendMode(android.graphics.BlendMode m) { mBlend = m; }
    public android.graphics.PorterDuff.Mode getCheckMarkTintMode() { return mMode; } public android.graphics.BlendMode getCheckMarkTintBlendMode() { return mBlend; }
    @Override public void setVisibility(int v) { super.setVisibility(v); if (mCheckMark != null) mCheckMark.setVisible(v == VISIBLE, false); }
    public void jumpDrawablesToCurrentState() { super.jumpDrawablesToCurrentState(); if (mCheckMark != null) mCheckMark.jumpToCurrentState(); }
    public Drawable getCheckMarkDrawable() { return mCheckMark; }
    public void onRtlPropertiesChanged(int dir) { requestLayout(); }
    public void drawableHotspotChanged(float x, float y) { super.drawableHotspotChanged(x, y); if (mCheckMark != null) mCheckMark.setHotspot(x, y); }
    @Override public CharSequence getAccessibilityClassName() { return CheckedTextView.class.getName(); }
    private boolean rtl() { return getLayoutDirection() == 1; }
    private int markW() { return mCheckMark == null ? 0 : mCheckMark.getIntrinsicWidth(); }
    @Override public int getCompoundPaddingRight() { return super.getCompoundPaddingRight() + (rtl() ? 0 : markW()); }
    @Override public int getCompoundPaddingLeft() { return super.getCompoundPaddingLeft() + (rtl() ? markW() : 0); }
    @Override protected int[] onCreateDrawableState(int extra) { int[] s = super.onCreateDrawableState(extra + 1); if (mChecked) mergeDrawableStates(s, new int[]{STATE_CHECKED}); return s; }
    @Override protected void drawableStateChanged() { super.drawableStateChanged(); if (mCheckMark != null && mCheckMark.isStateful() && mCheckMark.setState(getDrawableState())) invalidate(); }
    @Override protected void onDraw(android.graphics.Canvas c) {
        super.onDraw(c);
        Drawable d = mCheckMark; if (d == null) return;
        int h = d.getIntrinsicHeight(), w = d.getIntrinsicWidth(), vg = mGravity & 0x70;
        int top = vg == 0x50 ? getHeight() - h : vg == 0x10 ? (getHeight() - h) / 2 : 0;
        int right = rtl() ? mPaddingLeft + w : getWidth() - mPaddingRight, left = right - w;
        if (d.isStateful()) d.setState(getDrawableState());
        d.setBounds(left, top, right, top + h); d.draw(c);
    }
    public android.os.Parcelable onSaveInstanceState() { return super.onSaveInstanceState(); }
    public void onRestoreInstanceState(android.os.Parcelable s) { super.onRestoreInstanceState(s); }
}
