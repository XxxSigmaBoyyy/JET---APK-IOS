package android.text.method;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
/** AOSP base class: every navigation hook returns false (not handled) by default. */
public class BaseMovementMethod implements MovementMethod {
    public boolean canSelectArbitrarily() { return false; }
    public void initialize(TextView widget, Spannable text) {}
    public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event) { return false; }
    public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) { return false; }
    public boolean onKeyOther(TextView widget, Spannable text, KeyEvent event) { return false; }
    public void onTakeFocus(TextView widget, Spannable text, int direction) {}
    public boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event) { return false; }
    public boolean onTrackballEvent(TextView widget, Spannable text, MotionEvent event) { return false; }
    public boolean onGenericMotionEvent(TextView widget, Spannable text, MotionEvent event) { return false; }
    protected int getMovementMetaState(Spannable buffer, KeyEvent event) { return 0; }
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event) { return false; }
    protected boolean left(TextView w, Spannable b) { return false; } protected boolean right(TextView w, Spannable b) { return false; }
    protected boolean up(TextView w, Spannable b) { return false; } protected boolean down(TextView w, Spannable b) { return false; }
    protected boolean pageUp(TextView w, Spannable b) { return false; } protected boolean pageDown(TextView w, Spannable b) { return false; }
    protected boolean top(TextView w, Spannable b) { return false; } protected boolean bottom(TextView w, Spannable b) { return false; }
    protected boolean lineStart(TextView w, Spannable b) { return false; } protected boolean lineEnd(TextView w, Spannable b) { return false; }
    protected boolean home(TextView w, Spannable b) { return false; } protected boolean end(TextView w, Spannable b) { return false; }
}
