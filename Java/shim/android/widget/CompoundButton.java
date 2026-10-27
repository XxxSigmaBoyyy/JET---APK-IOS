package android.widget;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import jet.runtime.XmlAttrs;
/** AOSP CompoundButton: checked state, button drawable (state_checked), listener, toggle on click. */
public abstract class CompoundButton extends Button implements Checkable {
    public interface OnCheckedChangeListener { void onCheckedChanged(CompoundButton buttonView, boolean isChecked); }
    private static final int STATE_CHECKED = 16842912;
    private boolean mChecked, mBroadcasting; private OnCheckedChangeListener mOnCheckedChangeListener;
    /** AOSP hidden mOnCheckedChangeWidgetListener: RadioGroup tracks its children through this, never the app's listener slot. */
    OnCheckedChangeListener mJetWidgetListener;
    private Drawable mButtonDrawable; private ColorStateList mButtonTintList; private android.graphics.PorterDuff.Mode mButtonTintMode; private android.graphics.BlendMode mButtonBlendMode;
    private CharSequence mStateDescription;
    public CompoundButton(Context c) { this(c, null); }
    public CompoundButton(Context c, AttributeSet a) { this(c, a, 0); }
    public CompoundButton(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public CompoundButton(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        mGravity = 0x10 | 0x800003; setClickable(true); setFocusable(true);
        XmlAttrs x = mJetAttrs != null ? mJetAttrs : XmlAttrs.of(a);   // element + style + default style (checkboxStyle, ...)
        if (x != null) {
            try { String b = x.get("button"); if (b != null && !b.startsWith("?") && !"@null".equals(b)) setButtonDrawable(res().drawable(b)); } catch (RuntimeException ignored) {}
            String ch = x.get("checked"); if (ch != null) setChecked("true".equals(res().resolve(ch)));
        }
    }
    @Override protected void initDefaults() { mGravity = 0x10 | 0x800003; mTextColor = 0xDE000000; }
    public void toggle() { setChecked(!mChecked); }
    @Override public boolean performClick() { toggle(); boolean h = super.performClick(); if (!h) playSoundEffect(0); return h; }
    public boolean isChecked() { return mChecked; }
    public void setStateDescription(CharSequence s) { mStateDescription = s; }
    public void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked; refreshDrawableState(); invalidate();
        if (mBroadcasting) return;   // AOSP: avoid infinite recursion if the listener calls setChecked
        mBroadcasting = true;
        try { if (mOnCheckedChangeListener != null) mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
              if (mJetWidgetListener != null) mJetWidgetListener.onCheckedChanged(this, mChecked); } finally { mBroadcasting = false; }
    }
    public void setOnCheckedChangeListener(OnCheckedChangeListener l) { mOnCheckedChangeListener = l; }
    public void setButtonDrawable(int resId) { setButtonDrawable(resId == 0 ? null : getContext().getDrawable(resId)); }
    public void setButtonDrawable(Drawable d) {
        if (mButtonDrawable == d) return;
        if (mButtonDrawable != null) mButtonDrawable.setCallback(null);
        mButtonDrawable = d;
        if (d != null) { if (d.isStateful()) d.setState(getDrawableState()); d.setVisible(getVisibility() == VISIBLE, false); setMinHeight(d.getIntrinsicHeight()); applyButtonTint(); }
        requestLayout(); invalidate();
    }
    public Drawable getButtonDrawable() { return mButtonDrawable; }
    public void setButtonIcon(android.graphics.drawable.Icon icon) { setButtonDrawable(icon == null ? null : icon.loadDrawable(getContext())); }
    public void setButtonTintList(ColorStateList t) { mButtonTintList = t; applyButtonTint(); }
    public ColorStateList getButtonTintList() { return mButtonTintList; }
    public void setButtonTintMode(android.graphics.PorterDuff.Mode m) { mButtonTintMode = m; applyButtonTint(); }
    public void setButtonTintBlendMode(android.graphics.BlendMode m) { mButtonBlendMode = m; applyButtonTint(); }
    public android.graphics.PorterDuff.Mode getButtonTintMode() { return mButtonTintMode; }
    public android.graphics.BlendMode getButtonTintBlendMode() { return mButtonBlendMode; }
    private void applyButtonTint() { if (mButtonDrawable != null && (mButtonTintList != null || mButtonTintMode != null)) { mButtonDrawable = mButtonDrawable.mutate(); if (mButtonTintList != null) mButtonDrawable.setTintList(mButtonTintList); if (mButtonTintMode != null) mButtonDrawable.setTintMode(mButtonTintMode); if (mButtonDrawable.isStateful()) mButtonDrawable.setState(getDrawableState()); } }
    public CharSequence getAccessibilityClassName() { return CompoundButton.class.getName(); }
    private boolean jetRtl() { return getLayoutDirection() == 1; }
    @Override public int getCompoundPaddingLeft() { int p = super.getCompoundPaddingLeft(); if (!jetRtl() && mButtonDrawable != null) p += mButtonDrawable.getIntrinsicWidth(); return p; }
    @Override public int getCompoundPaddingRight() { int p = super.getCompoundPaddingRight(); if (jetRtl() && mButtonDrawable != null) p += mButtonDrawable.getIntrinsicWidth(); return p; }
    @Override protected int[] onCreateDrawableState(int extraSpace) { int[] s = super.onCreateDrawableState(extraSpace + 1); if (mChecked) mergeDrawableStates(s, new int[]{STATE_CHECKED}); return s; }
    @Override protected void drawableStateChanged() { super.drawableStateChanged(); if (mButtonDrawable != null && mButtonDrawable.isStateful() && mButtonDrawable.setState(getDrawableState())) invalidate(); }
    @Override protected boolean verifyDrawable(Drawable who) { return super.verifyDrawable(who) || who == mButtonDrawable; }
    public void drawableHotspotChanged(float x, float y) { super.drawableHotspotChanged(x, y); if (mButtonDrawable != null) mButtonDrawable.setHotspot(x, y); }
    public void jumpDrawablesToCurrentState() { super.jumpDrawablesToCurrentState(); if (mButtonDrawable != null) mButtonDrawable.jumpToCurrentState(); }
    @Override protected void onDraw(android.graphics.Canvas canvas) {
        Drawable b = mButtonDrawable;
        if (b != null) {
            int vg = mGravity & 0x70, h = b.getIntrinsicHeight(), w = b.getIntrinsicWidth();
            int top = vg == 0x50 ? getHeight() - h : vg == 0x10 ? (getHeight() - h) / 2 : 0;
            int left = jetRtl() ? getWidth() - w : 0;
            b.setBounds(left, top, left + w, top + h);
            if (b.isStateful()) b.setState(getDrawableState());
            b.draw(canvas);
        }
        super.onDraw(canvas);
    }
    public android.os.Parcelable onSaveInstanceState() { return super.onSaveInstanceState(); }
    public void onRestoreInstanceState(android.os.Parcelable state) { super.onRestoreInstanceState(state); }
    public void autofill(android.view.autofill.AutofillValue value) { if (value != null && value.isToggle()) setChecked(value.getToggleValue()); }
    public int getAutofillType() { return isEnabled() ? 2 /*AUTOFILL_TYPE_TOGGLE*/ : 0; }
    public android.view.autofill.AutofillValue getAutofillValue() { return isEnabled() ? android.view.autofill.AutofillValue.forToggle(isChecked()) : null; }
}
