package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import jet.runtime.XmlAttrs;
public class ToggleButton extends CompoundButton {
    private CharSequence mTextOn = "ON", mTextOff = "OFF"; private float mDisabledAlpha = 0.5f;
    public ToggleButton(Context c) { this(c, null); }
    public ToggleButton(Context c, AttributeSet a) { this(c, a, 0x0101004b /* buttonStyleToggle */); }
    public ToggleButton(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public ToggleButton(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        XmlAttrs x = mJetAttrs != null ? mJetAttrs : XmlAttrs.of(a);
        if (x != null) { String v; if ((v = x.get("textOn")) != null) mTextOn = res().resolve(v); if ((v = x.get("textOff")) != null) mTextOff = res().resolve(v); }
        mGravity = 0x11; syncTextState();
    }
    @Override public void setChecked(boolean checked) { super.setChecked(checked); syncTextState(); }
    private void syncTextState() { boolean on = isChecked(); if (on && mTextOn != null) setText(mTextOn); else if (!on && mTextOff != null) setText(mTextOff); }
    public CharSequence getTextOn() { return mTextOn; } public void setTextOn(CharSequence t) { mTextOn = t; }
    public CharSequence getTextOff() { return mTextOff; } public void setTextOff(CharSequence t) { mTextOff = t; }
    public float getDisabledAlpha() { return mDisabledAlpha; }
    @Override public void setBackgroundDrawable(android.graphics.drawable.Drawable d) { super.setBackgroundDrawable(d); }
    @Override public CharSequence getAccessibilityClassName() { return ToggleButton.class.getName(); }
}
