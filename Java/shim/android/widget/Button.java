package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class Button extends TextView {
    public Button(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public Button(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }

    public Button(Context c) { this(c, null); }
    public Button(Context c, AttributeSet a) { this(c, a, 0x01010048, 0); }
    @Override protected void initDefaults() { mGravity = 0x11; mTextColor = 0xFF000000; }
}
