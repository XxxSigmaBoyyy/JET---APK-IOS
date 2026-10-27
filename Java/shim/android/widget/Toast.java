package android.widget;
import android.content.Context;
public class Toast {
    public static final int LENGTH_SHORT = 0, LENGTH_LONG = 1;
    public static volatile String current;   // drawn by the host as an overlay
    private final CharSequence text;
    private Toast(CharSequence t) { text = t; }
    public static Toast makeText(Context c, CharSequence t, int d) { return new Toast(t); }
    public static Toast makeText(Context c, int res, int d) { return new Toast(c.getText(res)); }
    public void show() { current = String.valueOf(text); System.out.println("[Toast] " + text); jet.runtime.Looper.dirty = true; }
}
