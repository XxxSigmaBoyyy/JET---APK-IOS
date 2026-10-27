package android.widget;
import android.content.Context;
import android.util.AttributeSet;
/** AOSP ImageButton: an ImageView that is focusable and clickable by default (theme imageButtonStyle background). */
public class ImageButton extends ImageView {
    public ImageButton(Context c) { this(c, null); }
    public ImageButton(Context c, AttributeSet a) { this(c, a, 0x01010072 /*imageButtonStyle*/); }
    public ImageButton(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public ImageButton(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); setFocusable(true); setClickable(true); }
    @Override public CharSequence getAccessibilityClassName() { return ImageButton.class.getName(); }
    public android.view.PointerIcon onResolvePointerIcon(android.view.MotionEvent event, int pointerIndex) { return null; }
}
