package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class EditText extends TextView {
    public EditText(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public EditText(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); mBufferType = BufferType.EDITABLE;
        // Widget.EditText default style: focusable, focusableInTouchMode, clickable (an explicit android:focusable="false" wins).
        String nf = a == null ? null : a.getAttributeValue("http://schemas.android.com/apk/res/android", "focusable");
        if (!"false".equals(nf)) { setFocusable(true); setFocusableInTouchMode(true); } setClickable(true); }

    public EditText(Context c) { this(c, null); }
    public EditText(Context c, AttributeSet a) { this(c, a, 0x0101006e /* editTextStyle */); }
    /** Like Android: an EditText's text is always an Editable (SpannableStringBuilder) carrying the selection spans. */
    @Override public android.text.Editable getText() {
        boolean fresh = !(mText instanceof android.text.Editable);
        android.text.Editable e = jetEditable();
        if (fresh && android.text.Selection.getSelectionStart(e) < 0) android.text.Selection.setSelection(e, e.length());
        return e;
    }
    /** AOSP: EditText always keeps an EDITABLE buffer. */
    @Override public void setText(CharSequence s, BufferType t) { super.setText(s, BufferType.EDITABLE); }
    public void setSelection(int start, int stop) { android.text.Selection.setSelection(getText(), start, stop); }
    public void setSelection(int index) { setSelection(index, index); }
    public void selectAll() { android.text.Selection.selectAll(getText()); }
    public void extendSelection(int index) { android.text.Selection.extendSelection(getText(), index); }
    /** No selection span yet (text never set through setText/IME): behave as a cursor at the end. */
    @Override public int getSelectionStart() { int s = android.text.Selection.getSelectionStart(mText); return s < 0 ? length() : Math.min(s, length()); }
    @Override public int getSelectionEnd() { int s = android.text.Selection.getSelectionEnd(mText); return s < 0 ? length() : Math.min(s, length()); }
    @Override public boolean hasSelection() { return getSelectionStart() != getSelectionEnd(); }
    @Override public boolean onCheckIsTextEditor() { return mInputType != 0; }
    @Override protected void jetSetSel(int s, int e) { setSelection(s, e); }
    /** Tap on an editor: focus it and bring up the (virtual) soft keyboard, like TextView.onTouchEvent + Editor. */
    @Override public boolean onTouchEvent(android.view.MotionEvent ev) {
        boolean r = super.onTouchEvent(ev);
        if (ev.getActionMasked() == android.view.MotionEvent.ACTION_UP && isEnabled() && onCheckIsTextEditor()) {
            requestFocus();
            Object imm = getContext().getSystemService("input_method");
            if (imm instanceof android.view.inputmethod.InputMethodManager) ((android.view.inputmethod.InputMethodManager) imm).showSoftInput(this, 0);
            return true;
        }
        return r;
    }
    /** Hardware/IME key events on an editor: printable keys insert, DEL deletes before the cursor. */
    @Override public boolean onKeyDown(int kc, android.view.KeyEvent e) {
        if (!isEnabled() || !onCheckIsTextEditor()) return super.onKeyDown(kc, e);
        int a = getSelectionStart(), b = getSelectionEnd();
        if (kc == android.view.KeyEvent.KEYCODE_DEL) { if (a != b) jetEditReplace(a, b, ""); else if (a > 0) jetEditReplace(a - 1, a, ""); return true; }
        if (kc == android.view.KeyEvent.KEYCODE_FORWARD_DEL) { if (a != b) jetEditReplace(a, b, ""); else if (b < length()) jetEditReplace(b, b + 1, ""); return true; }
        int u = e == null ? 0 : e.getUnicodeChar();
        if (u > 0 && u != '\n') { jetEditReplace(a, b, new String(Character.toChars(u))); return true; }
        return super.onKeyDown(kc, e);
    }
    @Override public boolean getFreezesText() { return true; }
}
