package android.text;
/** AOSP android.text.Selection: the cursor/selection lives in the text as two point spans, so it follows edits. */
public class Selection {
    private Selection() {}
    private static final class START implements NoCopySpan { public String toString() { return "SELECTION_START"; } }
    private static final class END implements NoCopySpan { public String toString() { return "SELECTION_END"; } }
    public static final Object SELECTION_START = new START(), SELECTION_END = new END();
    public static int getSelectionStart(CharSequence text) { return text instanceof Spanned ? ((Spanned) text).getSpanStart(SELECTION_START) : -1; }
    public static int getSelectionEnd(CharSequence text) { return text instanceof Spanned ? ((Spanned) text).getSpanStart(SELECTION_END) : -1; }
    public static void setSelection(Spannable text, int start, int stop) {
        int n = text.length(); start = Math.max(0, Math.min(start, n)); stop = Math.max(0, Math.min(stop, n));
        text.setSpan(SELECTION_START, start, start, Spanned.SPAN_POINT_POINT | Spanned.SPAN_INTERMEDIATE);
        text.setSpan(SELECTION_END, stop, stop, Spanned.SPAN_POINT_POINT);
    }
    public static void setSelection(Spannable text, int index) { setSelection(text, index, index); }
    public static void selectAll(Spannable text) { setSelection(text, 0, text.length()); }
    public static void extendSelection(Spannable text, int index) { text.setSpan(SELECTION_END, index, index, Spanned.SPAN_POINT_POINT); }
    public static void removeSelection(Spannable text) { text.removeSpan(SELECTION_START); text.removeSpan(SELECTION_END); }
    public static boolean moveLeft(Spannable text, Layout l) { int a = getSelectionStart(text), b = getSelectionEnd(text); if (a != b) { setSelection(text, Math.min(a, b)); return true; } if (a > 0) { setSelection(text, a - 1); return true; } return false; }
    public static boolean moveRight(Spannable text, Layout l) { int a = getSelectionStart(text), b = getSelectionEnd(text); if (a != b) { setSelection(text, Math.max(a, b)); return true; } if (b < text.length()) { setSelection(text, b + 1); return true; } return false; }
}
