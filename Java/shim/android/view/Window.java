package android.view;
import android.content.Context;
public class Window {
    public interface Callback {
        boolean dispatchKeyEvent(KeyEvent event);
        boolean dispatchTouchEvent(MotionEvent event);
        boolean dispatchTrackballEvent(MotionEvent event);
        boolean dispatchGenericMotionEvent(MotionEvent event);
        boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event);
        View onCreatePanelView(int featureId);
        boolean onCreatePanelMenu(int featureId, Menu menu);
        boolean onPreparePanel(int featureId, View view, Menu menu);
        boolean onMenuOpened(int featureId, Menu menu);
        boolean onMenuItemSelected(int featureId, MenuItem item);
        void onWindowAttributesChanged(WindowManager.LayoutParams params);
        void onContentChanged();
        void onWindowFocusChanged(boolean hasFocus);
        void onAttachedToWindow();
        void onDetachedFromWindow();
        void onPanelClosed(int featureId, Menu menu);
        boolean onSearchRequested();
        ActionMode onWindowStartingActionMode(ActionMode.Callback callback);
        void onActionModeStarted(ActionMode mode);
        void onActionModeFinished(ActionMode mode);
    }
    private final Context ctx;
    private View content;
    public static final int ID_ANDROID_CONTENT = 0x01020002;
    private android.widget.FrameLayout decor, contentParent;
    private void ensureDecor() {
        if (decor != null) return;
        decor = new android.widget.FrameLayout(ctx);
        contentParent = new android.widget.FrameLayout(ctx);
        contentParent.setId(ID_ANDROID_CONTENT);
        decor.addView(contentParent, new ViewGroup.LayoutParams(-1, -1));
        // PhoneWindow.generateLayout: decor background = theme android:windowBackground
        try {
            if (ctx.getTheme() != null) {
                android.content.res.TypedArray a = ctx.getTheme().obtainStyledAttributes(new int[] { 0x01010054 });
                android.graphics.drawable.Drawable d = a.getDrawable(0); a.recycle();
                if (d != null) decor.setBackground(d);
            }
        } catch (Throwable e) { System.err.println("[WINDOW] windowBackground: " + e); }
    }
    public void setBackgroundDrawable(android.graphics.drawable.Drawable d) { ensureDecor(); decor.setBackground(d); }
    public void setBackgroundDrawableResource(int id) { setBackgroundDrawable(id == 0 ? null : ctx.getDrawable(id)); }
    private Callback callback;
    public Window(Context c) { ctx = c; }
    public void setCallback(Callback callback) { this.callback = callback; }
    public Callback getCallback() { return callback; }
    private final jet.runtime.JetBackDispatcher mBack = new jet.runtime.JetBackDispatcher();
    public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher() { return mBack; }
    public void setContentView(View v) {
        ensureDecor(); content = v; contentParent.removeAllViews(); if (v != null) { if (v.mParent instanceof ViewGroup) ((ViewGroup) v.mParent).removeView(v); contentParent.addView(v, new ViewGroup.LayoutParams(-1, -1)); }  // PhoneWindow.setContentView(View): MATCH_PARENT
    }
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureDecor(); content = v; contentParent.removeAllViews(); if (v != null) { if (v.mParent instanceof ViewGroup) ((ViewGroup) v.mParent).removeView(v); contentParent.addView(v, lp != null ? lp : new ViewGroup.LayoutParams(-1, -1)); }
    }
    public void addContentView(View v, ViewGroup.LayoutParams lp) { ensureDecor(); if (lp != null) v.setLayoutParams(lp); contentParent.addView(v); }
    public <T extends View> T findViewById(int id) { ensureDecor(); return (T) decor.findViewById(id); }
    public void setContentView(int layoutResID) {}
    public View getDecorView() { ensureDecor(); return decor; }
    public View peekDecorView() { return decor; }
    public View jetContent() { return content; }
    /** PhoneWindow.saveHierarchyState: content views' states by id + the focused view id. */
    public android.os.Bundle saveHierarchyState() {
        android.os.Bundle out = new android.os.Bundle(); if (contentParent == null) return out;
        android.util.SparseArray<android.os.Parcelable> states = new android.util.SparseArray<>();
        contentParent.saveHierarchyState(states); out.putSparseParcelableArray("android:views", states);
        View f = contentParent.findFocus(); if (f != null && f.getId() != View.NO_ID) out.putInt("android:focusedViewId", f.getId());
        return out;
    }
    public void restoreHierarchyState(android.os.Bundle in) {
        if (contentParent == null || in == null) return;
        android.util.SparseArray<android.os.Parcelable> states = in.getSparseParcelableArray("android:views");
        if (states != null) contentParent.restoreHierarchyState(states);
        int fid = in.getInt("android:focusedViewId", View.NO_ID);
        if (fid != View.NO_ID) { View f = contentParent.findViewById(fid); if (f != null) f.requestFocus(); }
    }
    public void setFlags(int flags, int mask) {}
    public void addFlags(int flags) {}
    public void clearFlags(int flags) {}
    public void setFormat(int format) {}
    public void setSoftInputMode(int mode) {}
    public void setTitle(CharSequence title) {}
    public void setStatusBarColor(int color) { jetStatusBarColor = color; }
    public void setNavigationBarColor(int color) { jetNavigationBarColor = color; }
    public void setLayout(int width, int height) {}
    private int jetFeatures;
    /** Records the feature like PhoneWindow (before setContentView); FEATURE_NO_TITLE clears the action bar feature. */
    public boolean requestFeature(int featureId) { if (featureId < 0 || featureId >= 32) return false; if (featureId == 1) jetFeatures &= ~(1 << 8); jetFeatures |= 1 << featureId; return true; }
    public void setFeatureInt(int featureId, int value) {}
    public WindowManager.LayoutParams getAttributes() { return new WindowManager.LayoutParams(); }
    public void setAttributes(WindowManager.LayoutParams a) {}
    public Context getContext() { return ctx; }
    private android.view.WindowInsetsController mJetInsetsController;
    public android.view.WindowInsetsController getInsetsController() { if (mJetInsetsController == null) mJetInsetsController = new android.view.WindowInsetsController.Jet(); return mJetInsetsController; }
    private int jetStatusBarColor = 0xFF000000, jetNavigationBarColor = 0xFF000000; private boolean jetDecorFits = true;
    public int getStatusBarColor() { return jetStatusBarColor; }
    public int getNavigationBarColor() { return jetNavigationBarColor; }
    public void setDecorFitsSystemWindows(boolean fits) { jetDecorFits = fits; }
    public boolean jetDecorFitsSystemWindows() { return jetDecorFits; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.transition.Transition jet_EnterTransition;
    private android.transition.Transition jet_ExitTransition;
    private android.transition.Transition jet_ReenterTransition;
    private android.transition.Transition jet_ReturnTransition;
    private android.transition.Transition jet_SharedElementEnterTransition;
    private android.transition.Transition jet_SharedElementExitTransition;
    private android.transition.Transition jet_SharedElementReenterTransition;
    private android.transition.Transition jet_SharedElementReturnTransition;
    private android.view.Window jet_Container;
    private boolean jet_AllowEnterTransitionOverlap;
    private boolean jet_AllowReturnTransitionOverlap;
    private boolean jet_NavigationBarContrastEnforced;
    private boolean jet_SharedElementsUseOverlay;
    private boolean jet_StatusBarContrastEnforced;
    private int jet_ColorMode;
    private int jet_NavigationBarDividerColor;
    private int jet_VolumeControlStream;
    private java.util.List jet_SystemGestureExclusionRects;
    private long jet_TransitionBackgroundFadeDuration;
    public android.content.res.TypedArray getWindowStyle() { jet.runtime.ShimStubs.hit("android.view.Window.getWindowStyle()Landroid/content/res/TypedArray;"); return null; }
    public void setContainer(android.view.Window p0) { jet_Container = p0; }
    public android.view.Window getContainer() { return jet_Container; }
    public boolean hasChildren() { jet.runtime.ShimStubs.hit("android.view.Window.hasChildren()Z"); return false; }
    public void setWindowManager(android.view.WindowManager p0, android.os.IBinder p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.view.Window.setWindowManager(Landroid/view/WindowManager;Landroid/os/IBinder;Ljava/lang/String;)V"); }
    public void setWindowManager(android.view.WindowManager p0, android.os.IBinder p1, java.lang.String p2, boolean p3) { jet.runtime.ShimStubs.hit("android.view.Window.setWindowManager(Landroid/view/WindowManager;Landroid/os/IBinder;Ljava/lang/String;Z)V"); }
    public android.view.WindowManager getWindowManager() { return ctx == null ? null : (android.view.WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE); }
    public void setHideOverlayWindows(boolean p0) { jet.runtime.ShimStubs.hit("android.view.Window.setHideOverlayWindows(Z)V"); }
    public void takeSurface(android.view.SurfaceHolder.Callback2 p0) { jet.runtime.ShimStubs.hit("android.view.Window.takeSurface(Landroid/view/SurfaceHolder$Callback2;)V"); }
    public boolean isFloating() { jet.runtime.ShimStubs.hit("android.view.Window.isFloating()Z"); return false; }
    public void setGravity(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setGravity(I)V"); }
    public void setType(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setType(I)V"); }
    public void setWindowAnimations(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setWindowAnimations(I)V"); }
    public void setColorMode(int p0) { jet_ColorMode = p0; }
    public void setPreferMinimalPostProcessing(boolean p0) { jet.runtime.ShimStubs.hit("android.view.Window.setPreferMinimalPostProcessing(Z)V"); }
    public int getColorMode() { return jet_ColorMode; }
    public boolean isWideColorGamut() { jet.runtime.ShimStubs.hit("android.view.Window.isWideColorGamut()Z"); return false; }
    public void setDimAmount(float p0) { jet.runtime.ShimStubs.hit("android.view.Window.setDimAmount(F)V"); }
    protected int getForcedWindowFlags() { jet.runtime.ShimStubs.hit("android.view.Window.getForcedWindowFlags()I"); return 0; }
    protected boolean hasSoftInputMode() { jet.runtime.ShimStubs.hit("android.view.Window.hasSoftInputMode()Z"); return false; }
    public void setSustainedPerformanceMode(boolean p0) { jet.runtime.ShimStubs.hit("android.view.Window.setSustainedPerformanceMode(Z)V"); }
    public void makeActive() { jet.runtime.ShimStubs.hit("android.view.Window.makeActive()V"); }
    public boolean isActive() { jet.runtime.ShimStubs.hit("android.view.Window.isActive()Z"); return false; }
    @SuppressWarnings("unchecked") public <T extends android.view.View> T requireViewById(int p0) { T v = (T) findViewById(p0); if (v == null) throw new IllegalArgumentException("ID does not reference a View inside this Window"); return v; }
    public android.view.View getCurrentFocus() { jet.runtime.ShimStubs.hit("android.view.Window.getCurrentFocus()Landroid/view/View;"); return null; }
    public android.view.LayoutInflater getLayoutInflater() { jet.runtime.ShimStubs.hit("android.view.Window.getLayoutInflater()Landroid/view/LayoutInflater;"); return null; }
    public void setTitleColor(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setTitleColor(I)V"); }
    public void openPanel(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.Window.openPanel(ILandroid/view/KeyEvent;)V"); }
    public void closePanel(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.closePanel(I)V"); }
    public void togglePanel(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.Window.togglePanel(ILandroid/view/KeyEvent;)V"); }
    public void invalidatePanelMenu(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.invalidatePanelMenu(I)V"); }
    public boolean performPanelShortcut(int p0, int p1, android.view.KeyEvent p2, int p3) { jet.runtime.ShimStubs.hit("android.view.Window.performPanelShortcut(IILandroid/view/KeyEvent;I)Z"); return false; }
    public boolean performPanelIdentifierAction(int p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.view.Window.performPanelIdentifierAction(III)Z"); return false; }
    public void closeAllPanels() { jet.runtime.ShimStubs.hit("android.view.Window.closeAllPanels()V"); }
    public boolean performContextMenuIdentifierAction(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.Window.performContextMenuIdentifierAction(II)Z"); return false; }
    public void onConfigurationChanged(android.content.res.Configuration p0) { jet.runtime.ShimStubs.hit("android.view.Window.onConfigurationChanged(Landroid/content/res/Configuration;)V"); }
    public void setElevation(float p0) { jet.runtime.ShimStubs.hit("android.view.Window.setElevation(F)V"); }
    public void setClipToOutline(boolean p0) { jet.runtime.ShimStubs.hit("android.view.Window.setClipToOutline(Z)V"); }
    public void setBackgroundBlurRadius(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setBackgroundBlurRadius(I)V"); }
    public void setFeatureDrawableResource(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.Window.setFeatureDrawableResource(II)V"); }
    public void setFeatureDrawableUri(int p0, android.net.Uri p1) { jet.runtime.ShimStubs.hit("android.view.Window.setFeatureDrawableUri(ILandroid/net/Uri;)V"); }
    public void setFeatureDrawable(int p0, android.graphics.drawable.Drawable p1) { jet.runtime.ShimStubs.hit("android.view.Window.setFeatureDrawable(ILandroid/graphics/drawable/Drawable;)V"); }
    public void setFeatureDrawableAlpha(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.Window.setFeatureDrawableAlpha(II)V"); }
    public void takeKeyEvents(boolean p0) { jet.runtime.ShimStubs.hit("android.view.Window.takeKeyEvents(Z)V"); }
    public boolean superDispatchKeyEvent(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.superDispatchKeyEvent(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean superDispatchKeyShortcutEvent(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.superDispatchKeyShortcutEvent(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean superDispatchTouchEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.superDispatchTouchEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean superDispatchTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.superDispatchTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean superDispatchGenericMotionEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.superDispatchGenericMotionEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected void onActive() { jet.runtime.ShimStubs.hit("android.view.Window.onActive()V"); }
    protected int getFeatures() { jet.runtime.ShimStubs.hit("android.view.Window.getFeatures()I"); return 0; }
    public static int getDefaultFeatures(android.content.Context p0) { jet.runtime.ShimStubs.hit("android.view.Window.getDefaultFeatures(Landroid/content/Context;)I"); return 0; }
    public boolean hasFeature(int f) { return f >= 0 && f < 32 && (jetFeatures & (1 << f)) != 0; }
    protected int getLocalFeatures() { jet.runtime.ShimStubs.hit("android.view.Window.getLocalFeatures()I"); return 0; }
    protected void setDefaultWindowFormat(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setDefaultWindowFormat(I)V"); }
    public void setChildDrawable(int p0, android.graphics.drawable.Drawable p1) { jet.runtime.ShimStubs.hit("android.view.Window.setChildDrawable(ILandroid/graphics/drawable/Drawable;)V"); }
    public void setChildInt(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.Window.setChildInt(II)V"); }
    public boolean isShortcutKey(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.Window.isShortcutKey(ILandroid/view/KeyEvent;)Z"); return false; }
    public void setVolumeControlStream(int p0) { jet_VolumeControlStream = p0; }
    public int getVolumeControlStream() { return jet_VolumeControlStream; }
    public void setUiOptions(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setUiOptions(I)V"); }
    public void setUiOptions(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.Window.setUiOptions(II)V"); }
    public void setIcon(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setIcon(I)V"); }
    public void setLogo(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setLogo(I)V"); }
    public void setLocalFocus(boolean p0, boolean p1) { jet.runtime.ShimStubs.hit("android.view.Window.setLocalFocus(ZZ)V"); }
    public void injectInputEvent(android.view.InputEvent p0) { jet.runtime.ShimStubs.hit("android.view.Window.injectInputEvent(Landroid/view/InputEvent;)V"); }
    public void setEnterTransition(android.transition.Transition p0) { jet_EnterTransition = p0; }
    public void setReturnTransition(android.transition.Transition p0) { jet_ReturnTransition = p0; }
    public void setExitTransition(android.transition.Transition p0) { jet_ExitTransition = p0; }
    public void setReenterTransition(android.transition.Transition p0) { jet_ReenterTransition = p0; }
    public android.transition.Transition getEnterTransition() { return jet_EnterTransition; }
    public android.transition.Transition getReturnTransition() { return jet_ReturnTransition; }
    public android.transition.Transition getExitTransition() { return jet_ExitTransition; }
    public android.transition.Transition getReenterTransition() { return jet_ReenterTransition; }
    public void setSharedElementEnterTransition(android.transition.Transition p0) { jet_SharedElementEnterTransition = p0; }
    public void setSharedElementReturnTransition(android.transition.Transition p0) { jet_SharedElementReturnTransition = p0; }
    public android.transition.Transition getSharedElementEnterTransition() { return jet_SharedElementEnterTransition; }
    public android.transition.Transition getSharedElementReturnTransition() { return jet_SharedElementReturnTransition; }
    public void setSharedElementExitTransition(android.transition.Transition p0) { jet_SharedElementExitTransition = p0; }
    public void setSharedElementReenterTransition(android.transition.Transition p0) { jet_SharedElementReenterTransition = p0; }
    public android.transition.Transition getSharedElementExitTransition() { return jet_SharedElementExitTransition; }
    public android.transition.Transition getSharedElementReenterTransition() { return jet_SharedElementReenterTransition; }
    public void setAllowEnterTransitionOverlap(boolean p0) { jet_AllowEnterTransitionOverlap = p0; }
    public boolean getAllowEnterTransitionOverlap() { return jet_AllowEnterTransitionOverlap; }
    public void setAllowReturnTransitionOverlap(boolean p0) { jet_AllowReturnTransitionOverlap = p0; }
    public boolean getAllowReturnTransitionOverlap() { return jet_AllowReturnTransitionOverlap; }
    public long getTransitionBackgroundFadeDuration() { return jet_TransitionBackgroundFadeDuration; }
    public void setTransitionBackgroundFadeDuration(long p0) { jet_TransitionBackgroundFadeDuration = p0; }
    public boolean getSharedElementsUseOverlay() { return jet_SharedElementsUseOverlay; }
    public void setSharedElementsUseOverlay(boolean p0) { jet_SharedElementsUseOverlay = p0; }
    public void setNavigationBarDividerColor(int p0) { jet_NavigationBarDividerColor = p0; }
    public int getNavigationBarDividerColor() { return jet_NavigationBarDividerColor; }
    public void setStatusBarContrastEnforced(boolean p0) { jet_StatusBarContrastEnforced = p0; }
    public boolean isStatusBarContrastEnforced() { return jet_StatusBarContrastEnforced; }
    public void setNavigationBarContrastEnforced(boolean p0) { jet_NavigationBarContrastEnforced = p0; }
    public boolean isNavigationBarContrastEnforced() { return jet_NavigationBarContrastEnforced; }
    public void setSystemGestureExclusionRects(java.util.List p0) { jet_SystemGestureExclusionRects = p0; }
    public java.util.List getSystemGestureExclusionRects() { return jet_SystemGestureExclusionRects; }
    public void setDecorCaptionShade(int p0) { jet.runtime.ShimStubs.hit("android.view.Window.setDecorCaptionShade(I)V"); }
    public void setResizingCaptionDrawable(android.graphics.drawable.Drawable p0) { jet.runtime.ShimStubs.hit("android.view.Window.setResizingCaptionDrawable(Landroid/graphics/drawable/Drawable;)V"); }
    // ---- END generated SDK surface ----
}
