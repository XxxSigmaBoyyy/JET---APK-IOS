package android.widget;
/** Minimal android.widget.Editor for apps that reflect TextView.mEditor (Telegram EditTextBoldCursor). */
public class Editor {
    private final android.view.ViewTreeObserver.OnPreDrawListener mPositionListener = () -> true;
    /** Reflected by Telegram EditTextBoldCursor. */
    android.view.ViewTreeObserver.OnPreDrawListener getPositionListener() { return mPositionListener; }
    void invalidateTextDisplayList() {}
    void invalidateTextDisplayList(android.text.Layout layout, int start, int end) {}
}
