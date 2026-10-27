package android.view.inputmethod;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
/** AOSP BaseInputConnection over an Editable: composing region, selection, commit/delete, key events routed to the target view. */
public class BaseInputConnection implements InputConnection {
    final View mTargetView; final boolean mFallbackMode;
    private Editable mFallback; private int mSelStart, mSelEnd, mCompStart = -1, mCompEnd = -1, mBatch;
    public BaseInputConnection(View targetView, boolean fullEditor) { mTargetView = targetView; mFallbackMode = !fullEditor; }
    public static final void removeComposingSpans(android.text.Spannable text) {}
    public static void setComposingSpans(android.text.Spannable text) {}
    public static int getComposingSpanStart(android.text.Spannable text) { return -1; }
    public static int getComposingSpanEnd(android.text.Spannable text) { return -1; }
    public Editable getEditable() {
        if (mTargetView instanceof android.widget.TextView) { Editable e = ((android.widget.TextView) mTargetView).getEditableText(); if (e != null) return e; }
        if (mFallback == null) mFallback = new android.text.SpannableStringBuilder(""); return mFallback;
    }
    private void clampSel() { int n = getEditable().length(); mSelStart = Math.max(0, Math.min(mSelStart, n)); mSelEnd = Math.max(0, Math.min(mSelEnd, n)); if (mCompEnd > n) { mCompStart = mCompEnd = -1; } }
    public boolean beginBatchEdit() { mBatch++; return true; }
    public boolean endBatchEdit() { if (mBatch > 0) mBatch--; return mBatch > 0; }
    public void endComposingRegionEditInternal() {}
    public boolean clearMetaKeyStates(int states) { return true; }
    public boolean commitCompletion(CompletionInfo text) { return false; }
    public boolean commitCorrection(CorrectionInfo correctionInfo) { return false; }
    public boolean commitText(CharSequence text, int newCursorPosition) { replaceText(text, newCursorPosition, false); return true; }
    public boolean deleteSurroundingText(int before, int after) {
        clampSel(); Editable e = getEditable(); int a = Math.min(mSelStart, mSelEnd), b = Math.max(mSelStart, mSelEnd);
        int ds = Math.max(0, a - before), de = Math.min(e.length(), b + after);
        if (de > b) e.delete(b, de); if (a > ds) e.delete(ds, a);
        mSelStart = ds; mSelEnd = ds + (b - a); mCompStart = mCompEnd = -1; return true;
    }
    public boolean deleteSurroundingTextInCodePoints(int before, int after) { return deleteSurroundingText(before, after); }
    public void closeConnection() { finishComposingText(); }
    public boolean finishComposingText() { mCompStart = mCompEnd = -1; return true; }
    public int getCursorCapsMode(int reqModes) { clampSel(); CharSequence t = getEditable(); int i = Math.min(mSelStart, mSelEnd) - 1; while (i >= 0 && t.charAt(i) == ' ') i--; return (i < 0 || ".!?".indexOf(t.charAt(i)) >= 0) ? reqModes & 0x4000 : 0; }
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) { return null; }
    public CharSequence getTextBeforeCursor(int n, int flags) { clampSel(); Editable e = getEditable(); int a = Math.min(mSelStart, mSelEnd); return e.subSequence(Math.max(0, a - n), a).toString(); }
    public CharSequence getSelectedText(int flags) { clampSel(); int a = Math.min(mSelStart, mSelEnd), b = Math.max(mSelStart, mSelEnd); return a == b ? null : getEditable().subSequence(a, b).toString(); }
    public CharSequence getTextAfterCursor(int n, int flags) { clampSel(); Editable e = getEditable(); int b = Math.max(mSelStart, mSelEnd); return e.subSequence(b, Math.min(e.length(), b + n)).toString(); }
    public SurroundingText getSurroundingText(int before, int after, int flags) { return InputConnection.super.getSurroundingText(before, after, flags); }
    public boolean performEditorAction(int actionCode) { long t = android.os.SystemClock.uptimeMillis(); sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0)); sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0)); return true; }
    public boolean performContextMenuAction(int id) { return false; }
    public boolean performPrivateCommand(String action, android.os.Bundle data) { return false; }
    public boolean requestCursorUpdates(int cursorUpdateMode) { return false; }
    public android.os.Handler getHandler() { return null; }
    public boolean setComposingText(CharSequence text, int newCursorPosition) { replaceText(text, newCursorPosition, true); return true; }
    public boolean setComposingRegion(int start, int end) { int n = getEditable().length(); int a = Math.max(0, Math.min(Math.min(start, end), n)), b = Math.max(0, Math.min(Math.max(start, end), n)); if (a == b) mCompStart = mCompEnd = -1; else { mCompStart = a; mCompEnd = b; } return true; }
    public boolean setSelection(int start, int end) { int n = getEditable().length(); if (start > n || end > n || start < 0 || end < 0) return true; mSelStart = start; mSelEnd = end; return true; }
    public boolean sendKeyEvent(KeyEvent event) { if (mTargetView != null) mTargetView.dispatchKeyEvent(event); return false; }
    public boolean reportFullscreenMode(boolean enabled) { return true; }
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, android.os.Bundle opts) { return false; }
    private void replaceText(CharSequence text, int newCursorPosition, boolean composing) {
        clampSel(); Editable e = getEditable();
        int a, b; if (mCompStart >= 0) { a = mCompStart; b = mCompEnd; } else { a = Math.min(mSelStart, mSelEnd); b = Math.max(mSelStart, mSelEnd); }
        if (text == null) text = ""; e.replace(a, b, text);
        if (composing) { mCompStart = a; mCompEnd = a + text.length(); } else mCompStart = mCompEnd = -1;
        int c = newCursorPosition > 0 ? a + text.length() + newCursorPosition - 1 : a + newCursorPosition;
        c = Math.max(0, Math.min(c, e.length())); mSelStart = mSelEnd = c;
    }
}
