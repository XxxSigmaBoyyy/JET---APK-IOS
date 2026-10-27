package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class CheckBox extends CompoundButton {
    public CheckBox(Context c) { this(c, null); }
    public CheckBox(Context c, AttributeSet a) { this(c, a, 0x0101006c /* checkboxStyle */); }
    public CheckBox(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public CheckBox(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }
    @Override public CharSequence getAccessibilityClassName() { return CheckBox.class.getName(); }

}
