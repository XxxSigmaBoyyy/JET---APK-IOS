package android.widget;
import android.content.Context;
import android.util.AttributeSet;
/** AOSP MultiAutoCompleteTextView: completes the token under the cursor (Tokenizer), not the whole text. */
public class MultiAutoCompleteTextView extends AutoCompleteTextView {
    public interface Tokenizer { int findTokenStart(CharSequence text, int cursor); int findTokenEnd(CharSequence text, int cursor); CharSequence terminateToken(CharSequence text); }
    public static class CommaTokenizer implements Tokenizer {
        public CommaTokenizer() {}
        public int findTokenStart(CharSequence text, int cursor) { int i = cursor; while (i > 0 && text.charAt(i - 1) != ',') i--; while (i < cursor && text.charAt(i) == ' ') i++; return i; }
        public int findTokenEnd(CharSequence text, int cursor) { int i = cursor, len = text.length(); while (i < len) { if (text.charAt(i) == ',') return i; i++; } return len; }
        public CharSequence terminateToken(CharSequence text) { int i = text.length(); while (i > 0 && text.charAt(i - 1) == ' ') i--; if (i > 0 && text.charAt(i - 1) == ',') return text; return text + ", "; }
    }
    private Tokenizer mTokenizer;
    public MultiAutoCompleteTextView(Context c) { this(c, null); }
    public MultiAutoCompleteTextView(Context c, AttributeSet a) { this(c, a, 0x0101006b); }
    public MultiAutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public MultiAutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }
    public void setTokenizer(Tokenizer t) { mTokenizer = t; }
    @Override protected void performFiltering(CharSequence text, int keyCode) {
        if (enoughToFilter()) { int end = getSelectionEnd(); int start = mTokenizer.findTokenStart(text, end); performFiltering(text, start, end, keyCode); } else dismissDropDown();
    }
    protected void performFiltering(CharSequence text, int start, int end, int keyCode) { super.performFiltering(text.subSequence(start, end), keyCode); }
    @Override public boolean enoughToFilter() {
        if (mTokenizer == null) return false; CharSequence text = getText(); int end = getSelectionEnd(); if (end < 0) return false;
        int start = mTokenizer.findTokenStart(text, end); return end - start >= getThreshold();
    }
    @Override public void performValidation() {
        Validator v = getValidator(); if (v == null || mTokenizer == null) return;
        android.text.Editable e = getText(); int i = getText().length();
        while (i > 0) { int start = mTokenizer.findTokenStart(e, i), end = mTokenizer.findTokenEnd(e, start); CharSequence sub = e.subSequence(start, end);
            if (android.text.TextUtils.isEmpty(sub)) e.replace(start, i, ""); else if (!v.isValid(sub)) e.replace(start, i, mTokenizer.terminateToken(v.fixText(sub))); i = start; }
    }
    @Override protected void replaceText(CharSequence text) {
        int end = getSelectionEnd(); int start = mTokenizer.findTokenStart(getText(), end);
        android.text.Editable e = getText(); String original = e.subSequence(start, end).toString(); e.replace(start, end, mTokenizer.terminateToken(text)); setSelection(Math.min(e.length(), start + mTokenizer.terminateToken(text).length()));
    }
    @Override public CharSequence getAccessibilityClassName() { return MultiAutoCompleteTextView.class.getName(); }
}
