package android.view.inputmethod;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;
/** Headless soft-keyboard model (Linux harness): one virtual IME. Tracks the served editor and its InputConnection (from
 *  View.onCreateInputConnection, so app overrides/wrappers are honored), shown/hidden state, and replies to ResultReceivers
 *  like the real service. Text reaches the editor only through that InputConnection (harness "type:" input). */
public final class InputMethodManager {
    public static final int SHOW_IMPLICIT = 1, SHOW_FORCED = 2, HIDE_IMPLICIT_ONLY = 1, HIDE_NOT_ALWAYS = 2;
    public static final int RESULT_UNCHANGED_SHOWN = 0, RESULT_UNCHANGED_HIDDEN = 1, RESULT_SHOWN = 2, RESULT_HIDDEN = 3;
    private static View sServed; private static InputConnection sConn; private static EditorInfo sInfo; private static boolean sShown;
    public static int jetShowCount, jetStartCount;
    InputMethodManager() {}
    // ---- harness hooks ----
    public static synchronized View jetServedView() { return sServed; }
    public static synchronized InputConnection jetConnection() { return sConn; }
    public static synchronized EditorInfo jetEditorInfo() { return sInfo; }
    public static synchronized boolean jetShown() { return sShown; }
    /** startInput: like IMM.startInputInner after an editor gains focus. */
    public static synchronized boolean jetStartInput(View v) {
        if (v == null || !v.onCheckIsTextEditor()) return false;
        if (sServed == v && sConn != null) return true;
        if (sConn != null) try { sConn.closeConnection(); } catch (Throwable ignore) {}
        EditorInfo ei = new EditorInfo(); try { ei.packageName = v.getContext().getPackageName(); } catch (Throwable ignore) {}
        ei.fieldId = v.getId(); InputConnection ic = null;
        try { ic = v.onCreateInputConnection(ei); } catch (Throwable t) { System.err.println("[jet-ime] onCreateInputConnection failed: " + t); }
        sServed = v; sConn = ic; sInfo = ei; jetStartCount++;
        System.err.println("[jet-ime] startInput " + v.getClass().getName() + " inputType=0x" + Integer.toHexString(ei.inputType) + " imeOptions=0x" + Integer.toHexString(ei.imeOptions) + (ic == null ? " (no InputConnection)" : ""));
        return true;
    }
    /** Focus moved to another editor while an input session is active: restart input on it (IMM.onViewFocusChanged). */
    public static synchronized void jetFocusIn(View v) { if (sServed != null && sServed != v && v.onCheckIsTextEditor()) jetStartInput(v); }
    private static void reply(ResultReceiver r, int code) { if (r != null) try { r.send(code, null); } catch (Throwable ignore) {} }
    // ---- SDK surface ----
    public java.util.List<InputMethodInfo> getInputMethodList() { return new java.util.ArrayList<>(); }
    public java.util.List<InputMethodInfo> getEnabledInputMethodList() { return new java.util.ArrayList<>(); }
    public java.util.List<InputMethodSubtype> getEnabledInputMethodSubtypeList(InputMethodInfo i, boolean b) { return new java.util.ArrayList<>(); }
    public void showStatusIcon(IBinder t, String p, int id) {} public void hideStatusIcon(IBinder t) {}
    public boolean isFullscreenMode() { return false; }
    public boolean isActive(View v) { synchronized (InputMethodManager.class) { return v != null && (sServed == v || (v.isFocused() && v.onCheckIsTextEditor())); } }
    public boolean isActive() { synchronized (InputMethodManager.class) { return sServed != null; } }
    public boolean isAcceptingText() { synchronized (InputMethodManager.class) { return sConn != null; } }
    public boolean isInputMethodSuppressingSpellChecker() { return false; }
    public void displayCompletions(View v, CompletionInfo[] c) {}
    public void updateExtractedText(View v, int token, ExtractedText t) {}
    public boolean showSoftInput(View v, int flags) { return showSoftInput(v, flags, null); }
    public boolean showSoftInput(View v, int flags, ResultReceiver r) {
        synchronized (InputMethodManager.class) {
            if (v == null || !jetStartInput(v)) return false;
            boolean was = sShown; sShown = true; jetShowCount++; reply(r, was ? RESULT_UNCHANGED_SHOWN : RESULT_SHOWN); return true;
        }
    }
    public boolean hideSoftInputFromWindow(IBinder t, int f) { return hideSoftInputFromWindow(t, f, null); }
    public boolean hideSoftInputFromWindow(IBinder t, int f, ResultReceiver r) { synchronized (InputMethodManager.class) { boolean was = sShown; sShown = false; reply(r, was ? RESULT_HIDDEN : RESULT_UNCHANGED_HIDDEN); return was; } }
    public void startStylusHandwriting(View v) {}
    public void toggleSoftInputFromWindow(IBinder t, int s, int h) { toggleSoftInput(s, h); }
    public void toggleSoftInput(int s, int h) { synchronized (InputMethodManager.class) { sShown = !sShown && sServed != null; } }
    public void restartInput(View v) { synchronized (InputMethodManager.class) { if (v != null && (sServed == v || v.isFocused())) { /* AOSP: served or focused view */ sServed = null; if (!jetStartInput(v)) { sServed = v; sConn = null; } } } }
    public void invalidateInput(View v) { restartInput(v); }
    public void updateSelection(View v, int a, int b, int c, int d) {}
    public void viewClicked(View v) {}
    public boolean isWatchingCursor(View v) { return false; }
    public void updateCursor(View v, int l, int t, int r, int b) {}
    public void updateCursorAnchorInfo(View v, CursorAnchorInfo i) {}
    public void sendAppPrivateCommand(View v, String a, android.os.Bundle d) {}
    public void setInputMethod(IBinder t, String id) {} public void setInputMethodAndSubtype(IBinder t, String id, InputMethodSubtype s) {}
    public void hideSoftInputFromInputMethod(IBinder t, int f) { hideSoftInputFromWindow(t, f); }
    public void showSoftInputFromInputMethod(IBinder t, int f) { synchronized (InputMethodManager.class) { if (sServed != null) sShown = true; } }
    public void dispatchKeyEventFromInputMethod(View v, android.view.KeyEvent e) { View t = v != null ? v : jetServedView(); if (t != null) t.dispatchKeyEvent(e); }
    public void showInputMethodPicker() {} public void showInputMethodAndSubtypeEnabler(String id) {}
    public InputMethodSubtype getCurrentInputMethodSubtype() { return null; }
    public boolean setCurrentInputMethodSubtype(InputMethodSubtype s) { return false; }
    public java.util.Map<InputMethodInfo, java.util.List<InputMethodSubtype>> getShortcutInputMethodsAndSubtypes() { return new java.util.HashMap<>(); }
    public boolean switchToLastInputMethod(IBinder t) { return false; } public boolean switchToNextInputMethod(IBinder t, boolean o) { return false; }
    public boolean shouldOfferSwitchingToNextInputMethod(IBinder t) { return false; }
    public void setAdditionalInputMethodSubtypes(String id, InputMethodSubtype[] s) {}
    public InputMethodSubtype getLastInputMethodSubtype() { return null; }
}
