package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class RadioButton extends CompoundButton {
    public RadioButton(Context c) { this(c, null); }
    public RadioButton(Context c, AttributeSet a) { this(c, a, 0x0101007e /* radioButtonStyle */); }
    public RadioButton(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public RadioButton(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }
    @Override public CharSequence getAccessibilityClassName() { return RadioButton.class.getName(); }
    /** AOSP: a checked radio button cannot be unchecked by the user. */
    @Override public void toggle() { if (!isChecked()) super.toggle(); }
    public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {}
}
