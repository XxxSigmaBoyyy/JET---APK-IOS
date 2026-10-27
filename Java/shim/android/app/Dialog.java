package android.app;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
public class Dialog implements DialogInterface, android.view.Window.Callback, android.view.KeyEvent.Callback {
    protected final Context ctx;
    private boolean cancelable = true;
    private Window window;
    public Dialog(Context context) { this.ctx = context; this.window = new Window(context); this.window.setCallback(this); }
    public Dialog(Context context, int themeResId) { this(context); }
    public void show() {}
    public void dismiss() {}
    public void cancel() {}
    public void setCancelable(boolean flag) { cancelable = flag; }
    private boolean jetCancelOnTouchOutside = true;
    public void setCanceledOnTouchOutside(boolean cancel) { if (cancel && !cancelable) cancelable = true; jetCancelOnTouchOutside = cancel; }
    public boolean jetCanceledOnTouchOutside() { return jetCancelOnTouchOutside; }
    public void setContentView(View view) {}
    public void setTitle(CharSequence title) {}
    public Window getWindow() { return window; }
    public Context getContext() { return ctx; }
    public boolean isShowing() { return false; }
    public void setOnDismissListener(OnDismissListener listener) {}
    public void setOnCancelListener(OnCancelListener listener) {}
    // Window.Callback / KeyEvent.Callback defaults (AOSP Dialog implements both)
    public boolean dispatchKeyEvent(android.view.KeyEvent e) { return false; }
    public boolean dispatchTouchEvent(android.view.MotionEvent e) { return false; }
    public boolean dispatchTrackballEvent(android.view.MotionEvent e) { return false; }
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent e) { return false; }
    public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) { return false; }
    public android.view.View onCreatePanelView(int f) { return null; }
    public boolean onCreatePanelMenu(int f, android.view.Menu m) { return false; }
    public boolean onPreparePanel(int f, android.view.View v, android.view.Menu m) { return true; }
    public boolean onMenuOpened(int f, android.view.Menu m) { return true; }
    public boolean onMenuItemSelected(int f, android.view.MenuItem i) { return false; }
    public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams p) {}
    public void onContentChanged() {}
    public void onWindowFocusChanged(boolean h) {}
    public void onAttachedToWindow() {}
    public void onDetachedFromWindow() {}
    public void onPanelClosed(int f, android.view.Menu m) {}
    public boolean onSearchRequested() { return false; }
    public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback c) { return null; }
    public void onActionModeStarted(android.view.ActionMode m) {}
    public void onActionModeFinished(android.view.ActionMode m) {}
    public boolean onKeyDown(int k, android.view.KeyEvent e) { return false; }
    public boolean onKeyLongPress(int k, android.view.KeyEvent e) { return false; }
    public boolean onKeyUp(int k, android.view.KeyEvent e) { return false; }
    public boolean onKeyMultiple(int k, int n, android.view.KeyEvent e) { return false; }
}
