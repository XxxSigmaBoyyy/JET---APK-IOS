package android.view.inputmethod;
public interface InputConnection {
    int GET_TEXT_WITH_STYLES = 1, GET_EXTRACTED_TEXT_MONITOR = 1, CURSOR_UPDATE_IMMEDIATE = 1, CURSOR_UPDATE_MONITOR = 2, CURSOR_UPDATE_FILTER_EDITOR_BOUNDS = 4, CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS = 8, CURSOR_UPDATE_FILTER_INSERTION_MARKER = 16, INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 1;
    CharSequence getTextBeforeCursor(int n, int flags);
    CharSequence getTextAfterCursor(int n, int flags);
    CharSequence getSelectedText(int flags);
    int getCursorCapsMode(int reqModes);
    ExtractedText getExtractedText(ExtractedTextRequest request, int flags);
    boolean deleteSurroundingText(int beforeLength, int afterLength);
    boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength);
    boolean setComposingText(CharSequence text, int newCursorPosition);
    boolean setComposingRegion(int start, int end);
    boolean finishComposingText();
    boolean commitText(CharSequence text, int newCursorPosition);
    boolean commitCompletion(CompletionInfo text);
    boolean commitCorrection(CorrectionInfo correctionInfo);
    boolean setSelection(int start, int end);
    boolean performEditorAction(int editorAction);
    boolean performContextMenuAction(int id);
    boolean beginBatchEdit();
    boolean endBatchEdit();
    boolean sendKeyEvent(android.view.KeyEvent event);
    boolean clearMetaKeyStates(int states);
    boolean reportFullscreenMode(boolean enabled);
    boolean performPrivateCommand(String action, android.os.Bundle data);
    boolean requestCursorUpdates(int cursorUpdateMode);
    android.os.Handler getHandler();
    void closeConnection();
    boolean commitContent(InputContentInfo inputContentInfo, int flags, android.os.Bundle opts);
    default SurroundingText getSurroundingText(int beforeLength, int afterLength, int flags) { CharSequence b = getTextBeforeCursor(beforeLength, flags), s = getSelectedText(flags), a = getTextAfterCursor(afterLength, flags); if (b == null || a == null) return null; if (s == null) s = ""; return new SurroundingText(b.toString() + s + a, b.length(), b.length() + s.length(), -1); }
    default boolean setComposingText(CharSequence text, int newCursorPosition, TextAttribute ta) { return setComposingText(text, newCursorPosition); }
    default boolean setComposingRegion(int start, int end, TextAttribute ta) { return setComposingRegion(start, end); }
    default boolean commitText(CharSequence text, int newCursorPosition, TextAttribute ta) { return commitText(text, newCursorPosition); }
    default boolean performSpellCheck() { return false; }
    default boolean requestCursorUpdates(int cursorUpdateMode, int cursorUpdateFilter) { return requestCursorUpdates(cursorUpdateMode | cursorUpdateFilter); }
    default boolean setImeConsumesInput(boolean imeConsumesInput) { return false; }
    default TextSnapshot takeSnapshot() { return null; }
}
