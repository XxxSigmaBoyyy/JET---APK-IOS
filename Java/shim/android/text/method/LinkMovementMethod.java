package android.text.method;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;
/** AOSP: ACTION_UP over a ClickableSpan calls its onClick(widget). Spans are matched by class hierarchy name (ClickableSpan
 *  may be an SDK auto-stub here). */
public class LinkMovementMethod extends ScrollingMovementMethod {
    private static LinkMovementMethod sInstance;
    public static MovementMethod getInstance() { if (sInstance == null) sInstance = new LinkMovementMethod(); return sInstance; }
    @Override public boolean canSelectArbitrarily() { return true; }
    @Override public void initialize(TextView widget, Spannable text) {}
    @Override public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int a = event.getAction();
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_DOWN) {
            android.text.Layout layout = widget.getLayout();
            if (layout != null && buffer != null) {
                int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX(), y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();
                int line = layout.getLineForVertical(y), off = layout.getOffsetForHorizontal(line, x);
                for (Object s : buffer.getSpans(off, off, Object.class)) {
                    if (!isClickable(s.getClass())) continue;
                    if (a == MotionEvent.ACTION_UP) try { s.getClass().getMethod("onClick", android.view.View.class).invoke(s, widget); } catch (ReflectiveOperationException ignored) {}
                    return true;
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }
    private static boolean isClickable(Class<?> c) { for (; c != null; c = c.getSuperclass()) if (c.getName().equals("android.text.style.ClickableSpan")) return true; return false; }
}
