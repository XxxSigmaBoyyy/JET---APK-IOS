package android.app;
import android.content.Intent;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

/** Activity lifecycle subset. On iOS the host maps this to a UIScene/UIViewController. */
public class Activity extends ContextThemeWrapper implements ComponentCallbacks2, android.view.Window.Callback {
    private View mContent;
    private boolean mCalled;
    private Intent mIntent = new Intent();
    private Application mApp;
    private Window mWindow;
    public Activity() { super(); }
    public final void jetAttach(Resources r) {
        mResources = r;
        mApp = Application.jetCurrent; if (mApp == null) { mApp = new Application(); mApp.jetAttach(r); }
        attachBaseContext(mApp);
        mWindow = new Window(this); mWindow.setCallback(this);
    }
    public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher() { return mWindow != null ? mWindow.getOnBackInvokedDispatcher() : new jet.runtime.JetBackDispatcher(); }
    protected void onCreate(Bundle b) { mCalled = true; }
    protected void onStart() {}
    protected void onResume() {}
    protected void onPause() {}
    protected void onStop() {}
    protected void onDestroy() {}
    /** AOSP: the window's view hierarchy state goes under android:viewHierarchyState. */
    protected void onSaveInstanceState(Bundle out) { if (mWindow != null) out.putBundle("android:viewHierarchyState", mWindow.saveHierarchyState()); }
    protected void onRestoreInstanceState(Bundle saved) { if (mWindow != null && saved != null) { Bundle w = saved.getBundle("android:viewHierarchyState"); if (w != null) mWindow.restoreHierarchyState(w); } }
    public void setContentView(View v) { mContent = v; if (mWindow != null) mWindow.setContentView(v); }
    public void setContentView(View v, android.view.ViewGroup.LayoutParams lp) { mContent = v; if (mWindow != null) mWindow.setContentView(v, lp); }
    public void setContentView(int layout) { setContentView(getLayoutInflater().inflate(layout, null, false)); }
    public <T extends View> T findViewById(int id) {
        View v = mContent == null ? null : mContent.findViewById(id);
        if (v == null && mWindow != null) v = mWindow.findViewById(id); // AOSP: getWindow().findViewById (decor incl. android.R.id.content)
        @SuppressWarnings("unchecked") T t = (T) v; return t;
    }
    public void addContentView(View v, android.view.ViewGroup.LayoutParams lp) { if (mWindow != null) mWindow.addContentView(v, lp); if (mContent == null) mContent = v; }
    public boolean isInMultiWindowMode() { return false; }
    public boolean isInPictureInPictureMode() { return false; }
    private boolean jetChangingConfigurations, jetDestroyed; private Object jetLastNonConfig; private boolean jetHasLastNonConfig;
    public boolean isChangingConfigurations() { return jetChangingConfigurations; }
    public boolean isDestroyed() { return jetDestroyed; }
    /** ActivityThread.handleRelaunchActivity: destroy for a configuration change. isChangingConfigurations() is true and
     *  onRetainNonConfigurationInstance() runs between onStop and onDestroy; the result goes to the new instance. */
    public final Object[] jetPerformRelaunchDestroy() {
        jetChangingConfigurations = true; Object[] r = new Object[2];
        jetPerformSave(o -> { try { o[0] = onRetainNonConfigurationInstance(); } catch (Throwable t) { jetLifecycleErrors.add(new Object[]{"onRetainNonConfigurationInstance", t}); } }, r);
        return r;
    }
    /** New instance of a relaunched activity: getLastNonConfigurationInstance() returns this until onResume finished. */
    public final void jetSetLastNonConfigurationInstance(Object o) { jetLastNonConfig = o; jetHasLastNonConfig = true; }
    public LayoutInflater getLayoutInflater() { return LayoutInflater.from(this); }
    public View jetContent() { return mContent; }
    private boolean mFinishing;
    public void finish() { mFinishing = true; }
    public void finishAffinity() { finish(); }
    public void finishAndRemoveTask() { finish(); }
    public Intent getIntent() {
        // ActivityThread always delivers an Intent with the activity component set.
        // Apps (e.g. Signal PassphraseRequiredActivity) reuse getIntent() as next_intent;
        // without a component, startActivity is a no-op in the harness.
        if (mIntent != null && mIntent.getComponent() == null) {
            String pkg = "";
            try { pkg = getPackageName(); } catch (Throwable ignore) {}
            if (pkg == null) pkg = "";
            mIntent.setClassName(pkg, getClass().getName());
        }
        return mIntent;
    }
    public android.content.ComponentName getComponentName() {
        return new android.content.ComponentName(getPackageName(), getClass().getName());
    }
    public void setIntent(Intent i) { mIntent = i; }
    public Application getApplication() { return mApp; }
    public Window getWindow() { return mWindow; }
    private CharSequence mTitle; private int mTitleColor, mResultCode, mRequestedOrientation = -1; private Intent mResultData;
    public void setTitle(CharSequence title) { mTitle = title; onTitleChanged(title, mTitleColor); }
    public void setTitle(int resId) { setTitle(getText(resId)); }
    public CharSequence getTitle() { if (mTitle == null) { try { String l = System.getProperty("jet.app.label"); mTitle = l != null ? l : getClass().getSimpleName(); } catch (Throwable t) { mTitle = ""; } } return mTitle; }
    public void setTitleColor(int c) { mTitleColor = c; } public final int getTitleColor() { return mTitleColor; }
    protected void onTitleChanged(CharSequence title, int color) {}
    protected void onChildTitleChanged(Activity child, CharSequence title) {}
    public ActionBar getActionBar() { return null; }
    public android.view.MenuInflater getMenuInflater() { return new android.view.MenuInflater(this); }
    public final boolean requestWindowFeature(int f) { return getWindow() != null && getWindow().requestFeature(f); }
    public void setRequestedOrientation(int o) { mRequestedOrientation = o; Boolean l = jetIsLandscape(o); if (l != null && android.util.DisplayMetrics.jetSetLandscape(l)) { jetOrientationChanges++; System.err.println("[jet-orient] " + getClass().getName() + " requested " + o + " -> " + android.util.DisplayMetrics.jetScreenW + "x" + android.util.DisplayMetrics.jetScreenH); } }
    public static int jetOrientationChanges;
    /** ActivityInfo.SCREEN_ORIENTATION_*: true = landscape family, false = portrait family, null = unspecified/sensor/user/locked. */
    public static Boolean jetIsLandscape(int o) { switch (o) { case 0: case 6: case 8: case 11: return Boolean.TRUE; case 1: case 7: case 9: case 12: return Boolean.FALSE; default: return null; } } public int getRequestedOrientation() { return mRequestedOrientation; }
    public android.view.WindowManager getWindowManager() { return (android.view.WindowManager) getSystemService(WINDOW_SERVICE); }
    public void overridePendingTransition(int enter, int exit) {}
    public void startActivityForResult(Intent i, int code) { startActivity(i); }
    public void startActivityForResult(Intent i, int code, Bundle o) { startActivity(i); }
    public final void setResult(int code) { mResultCode = code; } public final void setResult(int code, Intent data) { mResultCode = code; mResultData = data; }
    protected void onActivityResult(int req, int res, Intent data) {}
    protected void onNewIntent(Intent i) {}
    protected void onPostCreate(Bundle b) {}
    protected void onPostResume() {}
    protected void onRestart() {}
    public void onUserInteraction() {}
    protected void onUserLeaveHint() {}
    public void onAttachFragment(Fragment f) {}
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {}
    public final void requestPermissions(String[] perms, int code) { int[] r = new int[perms.length]; for (int i = 0; i < r.length; i++) r[i] = checkSelfPermission(perms[i]); onRequestPermissionsResult(code, perms, r); }
    public boolean shouldShowRequestPermissionRationale(String p) { return false; }
    private android.window.SplashScreen jetSplash;
    public final android.window.SplashScreen getSplashScreen() { if (jetSplash == null) jetSplash = new android.window.SplashScreen.Jet(); return jetSplash; }
    public final Activity getParent() { return null; }
    public boolean isTaskRoot() { return true; }
    public boolean moveTaskToBack(boolean nonRoot) { return false; }
    public void recreate() {}
    public android.view.View getCurrentFocus() { return null; }
    public final void setVolumeControlStream(int s) {}
    public void takeKeyEvents(boolean get) {}
    public void setFinishOnTouchOutside(boolean f) {}
    public void setShowWhenLocked(boolean s) {} public void setTurnScreenOn(boolean t) {}
    public void reportFullyDrawn() {}
    public void postponeEnterTransition() {} public void startPostponedEnterTransition() {}
    public void setEnterSharedElementCallback(SharedElementCallback cb) {} public void setExitSharedElementCallback(SharedElementCallback cb) {}
    public boolean onKeyDown(int k, android.view.KeyEvent e) { return false; } public boolean onKeyUp(int k, android.view.KeyEvent e) { return false; }
    public boolean onKeyLongPress(int k, android.view.KeyEvent e) { return false; }
    public boolean dispatchKeyShortcutEvent(android.view.KeyEvent e) { return false; }
    public boolean onKeyShortcut(int k, android.view.KeyEvent e) { return false; }
    public boolean onGenericMotionEvent(android.view.MotionEvent e) { return false; }
    public void openOptionsMenu() {} public void closeOptionsMenu() {}
    public void onOptionsMenuClosed(android.view.Menu m) {}
    public void registerForContextMenu(android.view.View v) {} public void unregisterForContextMenu(android.view.View v) {}
    public void onCreateContextMenu(android.view.ContextMenu m, android.view.View v, android.view.ContextMenu.ContextMenuInfo i) {}
    public boolean onContextItemSelected(android.view.MenuItem item) { return false; }
    public android.net.Uri getReferrer() { return null; }
    public String getCallingPackage() { return null; } public android.content.ComponentName getCallingActivity() { return null; }
    public int getTaskId() { return 1; }
    public android.content.SharedPreferences getPreferences(int mode) { return getSharedPreferences(getLocalClassName(), mode); }
    public String getLocalClassName() { String n = getClass().getName(), p = getPackageName(); return p != null && n.startsWith(p + ".") ? n.substring(p.length() + 1) : n; }
    public void onPictureInPictureModeChanged(boolean in) {} public void onMultiWindowModeChanged(boolean in) {}
    public void onPictureInPictureModeChanged(boolean in, android.content.res.Configuration c) {} public void onMultiWindowModeChanged(boolean in, android.content.res.Configuration c) {}
    public boolean enterPictureInPictureMode(PictureInPictureParams params) { return false; } public void setPictureInPictureParams(PictureInPictureParams p) {}
    public void onProvideAssistData(Bundle b) {}
    public void onTopResumedActivityChanged(boolean top) {}
    public boolean onNavigateUp() { return false; }
    public boolean navigateUpTo(Intent up) { return false; } public boolean shouldUpRecreateTask(Intent t) { return false; }
    public Intent getParentActivityIntent() { return null; }
    public void onLocalVoiceInteractionStarted() {}
    public Object getSystemService(String name) { return super.getSystemService(name); }
    public void setTaskDescription(ActivityManager.TaskDescription td) {}
    public void startLockTask() {} public void stopLockTask() {}
    public void showDialog(int id) {} public void dismissDialog(int id) {}
    public boolean isFinishing() { return mFinishing; }
    public void runOnUiThread(Runnable r) { r.run(); }
    private FragmentManager mFragments;
    public FragmentManager getFragmentManager() { if (mFragments == null) mFragments = new FragmentManager(this); return mFragments; }
    /** {phase, Throwable} for lifecycle callbacks that threw after onCreate (reported by the harness). */
    public static final java.util.List<Object[]> jetLifecycleErrors = new java.util.ArrayList<>();
    // Activity-scoped lifecycle callbacks (API 29). Dispatch order follows AOSP: activity's own, then Application's.
    private final java.util.List<Application.ActivityLifecycleCallbacks> jetOwnCallbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks c) { jetOwnCallbacks.add(c); }
    public void unregisterActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks c) { jetOwnCallbacks.remove(c); }
    private void jetCb(java.util.function.Consumer<Application.ActivityLifecycleCallbacks> f, String phase) {
        java.util.List<Application.ActivityLifecycleCallbacks> all = new java.util.ArrayList<>(jetOwnCallbacks);
        Application app = getApplication(); if (app != null) all.addAll(app.jetLifecycleCallbacks());
        for (Application.ActivityLifecycleCallbacks c : all) {
            try { f.accept(c); } catch (Throwable t) { System.err.println("[jet] lifecycle callback " + phase + ": " + t); t.printStackTrace(); jetLifecycleErrors.add(new Object[]{"callback:" + phase, t}); }
        }
    }
    public final void jetPerformCreate(Bundle b) {
        jetLifecycleErrors.clear();
        jetCb(c -> c.onActivityPreCreated(this, b), "preCreated");
        onCreate(b); if (!mCalled) throw new RuntimeException("SuperNotCalledException: onCreate()");
        jetCb(c -> c.onActivityCreated(this, b), "created");
        getFragmentManager().dispatch(2);
        jetCb(c -> c.onActivityPostCreated(this, b), "postCreated");
        jetCb(c -> c.onActivityPreStarted(this), "preStarted");
        try { onStart(); } catch (Throwable t) { System.err.println("[jet] onStart: "+t); t.printStackTrace(); jetLifecycleErrors.add(new Object[]{"onStart", t}); }
        jetCb(c -> c.onActivityStarted(this), "started");
        jetCb(c -> c.onActivityPostStarted(this), "postStarted");
        if (b != null) jetStep("onRestoreInstanceState", () -> onRestoreInstanceState(b)); // AOSP: after onStart, before onPostCreate
        try { onPostCreate(b); } catch (Throwable t) { jetLifecycleErrors.add(new Object[]{"onPostCreate", t}); }
        getFragmentManager().dispatch(4);
        jetCb(c -> c.onActivityPreResumed(this), "preResumed");
        try { onResume(); } catch (Throwable t) { System.err.println("[jet] onResume: "+t); t.printStackTrace(); jetLifecycleErrors.add(new Object[]{"onResume", t}); }
        jetCb(c -> c.onActivityResumed(this), "resumed");
        jetCb(c -> c.onActivityPostResumed(this), "postResumed");
        try { onPostResume(); } catch (Throwable t) { jetLifecycleErrors.add(new Object[]{"onPostResume", t}); }
        getFragmentManager().dispatch(5);
        jetHasLastNonConfig = false; jetLastNonConfig = null; // AOSP: mLastNonConfigurationInstances = null after performResume
    }
    public final Bundle jetPerformSave() { return jetPerformSave(null, null); }
    private Bundle jetPerformSave(java.util.function.Consumer<Object[]> retain, Object[] out) {
        Bundle b = new Bundle();
        jetCb(c -> c.onActivityPrePaused(this), "prePaused"); onPause(); jetCb(c -> c.onActivityPaused(this), "paused"); jetCb(c -> c.onActivityPostPaused(this), "postPaused");
        jetCb(c -> c.onActivityPreSaveInstanceState(this, b), "preSave"); onSaveInstanceState(b); jetCb(c -> c.onActivitySaveInstanceState(this, b), "save"); jetCb(c -> c.onActivityPostSaveInstanceState(this, b), "postSave");
        jetCb(c -> c.onActivityPreStopped(this), "preStopped"); onStop(); jetCb(c -> c.onActivityStopped(this), "stopped"); jetCb(c -> c.onActivityPostStopped(this), "postStopped");
        if (retain != null) retain.accept(out);
        jetCb(c -> c.onActivityPreDestroyed(this), "preDestroyed"); jetStep("onDestroy", this::onDestroy); jetDestroyed = true; jetCb(c -> c.onActivityDestroyed(this), "destroyed"); jetCb(c -> c.onActivityPostDestroyed(this), "postDestroyed");
        if (out != null) out[1] = b;
        return b;
    }
    private void jetStep(String phase, Runnable r) { try { r.run(); } catch (Throwable t) { System.err.println("[jet] " + phase + ": " + t); t.printStackTrace(); jetLifecycleErrors.add(new Object[]{phase, t}); } }
    /** Home button: onPause, onStop, then onSaveInstanceState (API 28+ order); the instance stays alive. */
    public final Bundle jetPerformBackground() {
        Bundle b = new Bundle();
        jetCb(c -> c.onActivityPrePaused(this), "prePaused"); jetStep("onPause", this::onPause); jetCb(c -> c.onActivityPaused(this), "paused"); jetCb(c -> c.onActivityPostPaused(this), "postPaused");
        getFragmentManager().dispatch(4);
        jetCb(c -> c.onActivityPreStopped(this), "preStopped"); jetStep("onStop", this::onStop); jetCb(c -> c.onActivityStopped(this), "stopped"); jetCb(c -> c.onActivityPostStopped(this), "postStopped");
        jetCb(c -> c.onActivityPreSaveInstanceState(this, b), "preSave"); jetStep("onSaveInstanceState", () -> onSaveInstanceState(b)); jetCb(c -> c.onActivitySaveInstanceState(this, b), "save"); jetCb(c -> c.onActivityPostSaveInstanceState(this, b), "postSave");
        return b;
    }
    /** Back to the foreground from {@link #jetPerformBackground}: onRestart, onStart, onResume, onPostResume. */
    public final void jetPerformForeground() {
        jetStep("onRestart", this::onRestart);
        jetCb(c -> c.onActivityPreStarted(this), "preStarted"); jetStep("onStart", this::onStart); jetCb(c -> c.onActivityStarted(this), "started"); jetCb(c -> c.onActivityPostStarted(this), "postStarted");
        jetCb(c -> c.onActivityPreResumed(this), "preResumed"); jetStep("onResume", this::onResume); jetCb(c -> c.onActivityResumed(this), "resumed"); jetCb(c -> c.onActivityPostResumed(this), "postResumed");
        jetStep("onPostResume", this::onPostResume); getFragmentManager().dispatch(5);
    }
    public Object onRetainNonConfigurationInstance() { return null; }
    public Object getLastNonConfigurationInstance() { return jetHasLastNonConfig ? jetLastNonConfig : null; }
    public void invalidateOptionsMenu() {}
    public boolean onCreateOptionsMenu(android.view.Menu menu) { return true; }
    public boolean onPrepareOptionsMenu(android.view.Menu menu) { return true; }
    public boolean onOptionsItemSelected(android.view.MenuItem item) { return false; }
    public void onBackPressed() { finish(); }
    public boolean onTouchEvent(android.view.MotionEvent e) { return false; }

    @Override public void onConfigurationChanged(Configuration newConfig) {}
    @Override public void onLowMemory() {}
    @Override public void onTrimMemory(int level) {}

    // Window.Callback
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        // AOSP-ish: decor first, then activity callbacks.
        android.view.Window w = getWindow();
        android.view.View decor = w != null ? w.getDecorView() : null;
        if (decor != null && decor.dispatchKeyEvent(event)) return true;
        int keyCode = event.getKeyCode();
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { onBackPressed(); return true; }
            return onKeyDown(keyCode, event);
        }
        if (event.getAction() == android.view.KeyEvent.ACTION_UP) return onKeyUp(keyCode, event);
        return false;
    }
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        android.view.Window w = getWindow();
        android.view.View decor = w != null ? w.getDecorView() : null;
        if (decor != null && decor.dispatchTouchEvent(event)) return true;
        return onTouchEvent(event);
    }
    public boolean dispatchTrackballEvent(android.view.MotionEvent event) { return false; }
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) { return false; }
    public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) { return false; }
    public android.view.View onCreatePanelView(int featureId) { return null; }
    public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) { return onCreateOptionsMenu(menu); }
    public boolean onPreparePanel(int featureId, android.view.View view, android.view.Menu menu) { return onPrepareOptionsMenu(menu); }
    public boolean onMenuOpened(int featureId, android.view.Menu menu) { return true; }
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) { return onOptionsItemSelected(item); }
    public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams params) {}
    public void onContentChanged() {}
    public void onWindowFocusChanged(boolean hasFocus) {}
    public void onAttachedToWindow() {}
    public void onDetachedFromWindow() {}
    public void onPanelClosed(int featureId, android.view.Menu menu) {}
    public boolean onSearchRequested() { return false; }
    public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback) { return null; }
    public void onActionModeStarted(android.view.ActionMode mode) {}
    public void onActionModeFinished(android.view.ActionMode mode) {}

    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private boolean jet_Immersive;
    public boolean isChild() { jet.runtime.ShimStubs.hit("android.app.Activity.isChild()Z"); return false; }
    public void onCreate(android.os.Bundle p0, android.os.PersistableBundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreate(Landroid/os/Bundle;Landroid/os/PersistableBundle;)V"); }
    public void onRestoreInstanceState(android.os.Bundle b, android.os.PersistableBundle pb) { if (b != null) onRestoreInstanceState(b); }
    public void onPostCreate(android.os.Bundle p0, android.os.PersistableBundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onPostCreate(Landroid/os/Bundle;Landroid/os/PersistableBundle;)V"); }
    public void onStateNotSaved() { jet.runtime.ShimStubs.hit("android.app.Activity.onStateNotSaved()V"); }
    public boolean isVoiceInteraction() { jet.runtime.ShimStubs.hit("android.app.Activity.isVoiceInteraction()Z"); return false; }
    public boolean isVoiceInteractionRoot() { jet.runtime.ShimStubs.hit("android.app.Activity.isVoiceInteractionRoot()Z"); return false; }
    public boolean isLocalVoiceInteractionSupported() { jet.runtime.ShimStubs.hit("android.app.Activity.isLocalVoiceInteractionSupported()Z"); return false; }
    public void startLocalVoiceInteraction(android.os.Bundle p0) { jet.runtime.ShimStubs.hit("android.app.Activity.startLocalVoiceInteraction(Landroid/os/Bundle;)V"); }
    public void onLocalVoiceInteractionStopped() { jet.runtime.ShimStubs.hit("android.app.Activity.onLocalVoiceInteractionStopped()V"); }
    public void stopLocalVoiceInteraction() { jet.runtime.ShimStubs.hit("android.app.Activity.stopLocalVoiceInteraction()V"); }
    public void onSaveInstanceState(android.os.Bundle b, android.os.PersistableBundle pb) { onSaveInstanceState(b); }
    public boolean onCreateThumbnail(android.graphics.Bitmap p0, android.graphics.Canvas p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateThumbnail(Landroid/graphics/Bitmap;Landroid/graphics/Canvas;)Z"); return false; }
    public java.lang.CharSequence onCreateDescription() { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateDescription()Ljava/lang/CharSequence;"); return null; }
    public void requestShowKeyboardShortcuts() { jet.runtime.ShimStubs.hit("android.app.Activity.requestShowKeyboardShortcuts()V"); }
    public void dismissKeyboardShortcutsHelper() { jet.runtime.ShimStubs.hit("android.app.Activity.dismissKeyboardShortcutsHelper()V"); }
    public void onProvideKeyboardShortcuts(java.util.List p0, android.view.Menu p1, int p2) { jet.runtime.ShimStubs.hit("android.app.Activity.onProvideKeyboardShortcuts(Ljava/util/List;Landroid/view/Menu;I)V"); }
    public boolean showAssist(android.os.Bundle p0) { jet.runtime.ShimStubs.hit("android.app.Activity.showAssist(Landroid/os/Bundle;)Z"); return false; }
    public void enterPictureInPictureMode() { jet.runtime.ShimStubs.hit("android.app.Activity.enterPictureInPictureMode()V"); }
    public int getMaxNumPictureInPictureActions() { jet.runtime.ShimStubs.hit("android.app.Activity.getMaxNumPictureInPictureActions()I"); return 0; }
    public boolean onPictureInPictureRequested() { jet.runtime.ShimStubs.hit("android.app.Activity.onPictureInPictureRequested()Z"); return false; }
    public void setShouldDockBigOverlays(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setShouldDockBigOverlays(Z)V"); }
    public boolean shouldDockBigOverlays() { jet.runtime.ShimStubs.hit("android.app.Activity.shouldDockBigOverlays()Z"); return false; }
    public int getChangingConfigurations() { jet.runtime.ShimStubs.hit("android.app.Activity.getChangingConfigurations()I"); return 0; }
    public android.database.Cursor managedQuery(android.net.Uri p0, java.lang.String[] p1, java.lang.String p2, java.lang.String[] p3, java.lang.String p4) { jet.runtime.ShimStubs.hit("android.app.Activity.managedQuery(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;"); return null; }
    public void startManagingCursor(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.app.Activity.startManagingCursor(Landroid/database/Cursor;)V"); }
    public void stopManagingCursor(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.app.Activity.stopManagingCursor(Landroid/database/Cursor;)V"); }
    @SuppressWarnings("unchecked") public <T extends android.view.View> T requireViewById(int p0) { T v = (T) findViewById(p0); if (v == null) throw new IllegalArgumentException("ID does not reference a View inside this Activity"); return v; }
    public void setDefaultKeyMode(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setDefaultKeyMode(I)V"); }
    public boolean onKeyMultiple(int p0, int p1, android.view.KeyEvent p2) { jet.runtime.ShimStubs.hit("android.app.Activity.onKeyMultiple(IILandroid/view/KeyEvent;)Z"); return false; }
    public boolean onTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.app.Activity.onTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean hasWindowFocus() { jet.runtime.ShimStubs.hit("android.app.Activity.hasWindowFocus()Z"); return false; }
    public boolean onNavigateUpFromChild(android.app.Activity p0) { jet.runtime.ShimStubs.hit("android.app.Activity.onNavigateUpFromChild(Landroid/app/Activity;)Z"); return false; }
    public void openContextMenu(android.view.View p0) { jet.runtime.ShimStubs.hit("android.app.Activity.openContextMenu(Landroid/view/View;)V"); }
    public void closeContextMenu() { jet.runtime.ShimStubs.hit("android.app.Activity.closeContextMenu()V"); }
    public void onContextMenuClosed(android.view.Menu p0) { jet.runtime.ShimStubs.hit("android.app.Activity.onContextMenuClosed(Landroid/view/Menu;)V"); }
    protected android.app.Dialog onCreateDialog(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateDialog(I)Landroid/app/Dialog;"); return null; }
    protected android.app.Dialog onCreateDialog(int p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateDialog(ILandroid/os/Bundle;)Landroid/app/Dialog;"); return null; }
    protected void onPrepareDialog(int p0, android.app.Dialog p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onPrepareDialog(ILandroid/app/Dialog;)V"); }
    protected void onPrepareDialog(int p0, android.app.Dialog p1, android.os.Bundle p2) { jet.runtime.ShimStubs.hit("android.app.Activity.onPrepareDialog(ILandroid/app/Dialog;Landroid/os/Bundle;)V"); }
    public boolean showDialog(int p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.showDialog(ILandroid/os/Bundle;)Z"); return false; }
    public void removeDialog(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.removeDialog(I)V"); }
    public void startSearch(java.lang.String p0, boolean p1, android.os.Bundle p2, boolean p3) { jet.runtime.ShimStubs.hit("android.app.Activity.startSearch(Ljava/lang/String;ZLandroid/os/Bundle;Z)V"); }
    public void triggerSearch(java.lang.String p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.triggerSearch(Ljava/lang/String;Landroid/os/Bundle;)V"); }
    public void setFeatureDrawableResource(int p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.setFeatureDrawableResource(II)V"); }
    public void setFeatureDrawableUri(int p0, android.net.Uri p1) { jet.runtime.ShimStubs.hit("android.app.Activity.setFeatureDrawableUri(ILandroid/net/Uri;)V"); }
    public void setFeatureDrawable(int p0, android.graphics.drawable.Drawable p1) { jet.runtime.ShimStubs.hit("android.app.Activity.setFeatureDrawable(ILandroid/graphics/drawable/Drawable;)V"); }
    public void setFeatureDrawableAlpha(int p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.setFeatureDrawableAlpha(II)V"); }
    protected void onApplyThemeResource(android.content.res.Resources.Theme p0, int p1, boolean p2) { jet.runtime.ShimStubs.hit("android.app.Activity.onApplyThemeResource(Landroid/content/res/Resources$Theme;IZ)V"); }
    public boolean isActivityTransitionRunning() { jet.runtime.ShimStubs.hit("android.app.Activity.isActivityTransitionRunning()Z"); return false; }
    public void startIntentSenderForResult(android.content.IntentSender p0, int p1, android.content.Intent p2, int p3, int p4, int p5) { jet.runtime.ShimStubs.hit("android.app.Activity.startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;III)V"); }
    public void startIntentSenderForResult(android.content.IntentSender p0, int p1, android.content.Intent p2, int p3, int p4, int p5, android.os.Bundle p6) { jet.runtime.ShimStubs.hit("android.app.Activity.startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V"); }
    public void startIntentSender(android.content.IntentSender p0, android.content.Intent p1, int p2, int p3, int p4, android.os.Bundle p5) { jet.runtime.ShimStubs.hit("android.app.Activity.startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;IIILandroid/os/Bundle;)V"); }
    public boolean startActivityIfNeeded(android.content.Intent p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityIfNeeded(Landroid/content/Intent;I)Z"); return false; }
    public boolean startActivityIfNeeded(android.content.Intent p0, int p1, android.os.Bundle p2) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityIfNeeded(Landroid/content/Intent;ILandroid/os/Bundle;)Z"); return false; }
    public boolean startNextMatchingActivity(android.content.Intent p0) { jet.runtime.ShimStubs.hit("android.app.Activity.startNextMatchingActivity(Landroid/content/Intent;)Z"); return false; }
    public boolean startNextMatchingActivity(android.content.Intent p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.app.Activity.startNextMatchingActivity(Landroid/content/Intent;Landroid/os/Bundle;)Z"); return false; }
    public void startActivityFromChild(android.app.Activity p0, android.content.Intent p1, int p2) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityFromChild(Landroid/app/Activity;Landroid/content/Intent;I)V"); }
    public void startActivityFromChild(android.app.Activity p0, android.content.Intent p1, int p2, android.os.Bundle p3) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityFromChild(Landroid/app/Activity;Landroid/content/Intent;ILandroid/os/Bundle;)V"); }
    public void startActivityFromFragment(android.app.Fragment p0, android.content.Intent p1, int p2) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityFromFragment(Landroid/app/Fragment;Landroid/content/Intent;I)V"); }
    public void startActivityFromFragment(android.app.Fragment p0, android.content.Intent p1, int p2, android.os.Bundle p3) { jet.runtime.ShimStubs.hit("android.app.Activity.startActivityFromFragment(Landroid/app/Fragment;Landroid/content/Intent;ILandroid/os/Bundle;)V"); }
    public void startIntentSenderFromChild(android.app.Activity p0, android.content.IntentSender p1, int p2, android.content.Intent p3, int p4, int p5, int p6) { jet.runtime.ShimStubs.hit("android.app.Activity.startIntentSenderFromChild(Landroid/app/Activity;Landroid/content/IntentSender;ILandroid/content/Intent;III)V"); }
    public void startIntentSenderFromChild(android.app.Activity p0, android.content.IntentSender p1, int p2, android.content.Intent p3, int p4, int p5, int p6, android.os.Bundle p7) { jet.runtime.ShimStubs.hit("android.app.Activity.startIntentSenderFromChild(Landroid/app/Activity;Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V"); }
    public void overridePendingTransition(int p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.app.Activity.overridePendingTransition(III)V"); }
    public android.net.Uri onProvideReferrer() { jet.runtime.ShimStubs.hit("android.app.Activity.onProvideReferrer()Landroid/net/Uri;"); return null; }
    public void setVisible(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setVisible(Z)V"); }
    public void finishFromChild(android.app.Activity p0) { jet.runtime.ShimStubs.hit("android.app.Activity.finishFromChild(Landroid/app/Activity;)V"); }
    public void finishAfterTransition() { jet.runtime.ShimStubs.hit("android.app.Activity.finishAfterTransition()V"); }
    public void finishActivity(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.finishActivity(I)V"); }
    public void finishActivityFromChild(android.app.Activity p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.finishActivityFromChild(Landroid/app/Activity;I)V"); }
    public boolean releaseInstance() { jet.runtime.ShimStubs.hit("android.app.Activity.releaseInstance()Z"); return false; }
    public void onActivityReenter(int p0, android.content.Intent p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onActivityReenter(ILandroid/content/Intent;)V"); }
    public boolean isLaunchedFromBubble() { jet.runtime.ShimStubs.hit("android.app.Activity.isLaunchedFromBubble()Z"); return false; }
    public void setProgressBarVisibility(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setProgressBarVisibility(Z)V"); }
    public void setProgressBarIndeterminateVisibility(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setProgressBarIndeterminateVisibility(Z)V"); }
    public void setProgressBarIndeterminate(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setProgressBarIndeterminate(Z)V"); }
    public void setProgress(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setProgress(I)V"); }
    public void setSecondaryProgress(int p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setSecondaryProgress(I)V"); }
    public int getVolumeControlStream() { jet.runtime.ShimStubs.hit("android.app.Activity.getVolumeControlStream()I"); return 0; }
    public android.view.View onCreateView(java.lang.String p0, android.content.Context p1, android.util.AttributeSet p2) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateView(Ljava/lang/String;Landroid/content/Context;Landroid/util/AttributeSet;)Landroid/view/View;"); return null; }
    public android.view.View onCreateView(android.view.View p0, java.lang.String p1, android.content.Context p2, android.util.AttributeSet p3) { jet.runtime.ShimStubs.hit("android.app.Activity.onCreateView(Landroid/view/View;Ljava/lang/String;Landroid/content/Context;Landroid/util/AttributeSet;)Landroid/view/View;"); return null; }
    public void dump(java.lang.String p0, java.io.FileDescriptor p1, java.io.PrintWriter p2, java.lang.String[] p3) { jet.runtime.ShimStubs.hit("android.app.Activity.dump(Ljava/lang/String;Ljava/io/FileDescriptor;Ljava/io/PrintWriter;[Ljava/lang/String;)V"); }
    public boolean isImmersive() { return jet_Immersive; }
    public boolean setTranslucent(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setTranslucent(Z)Z"); return false; }
    public boolean requestVisibleBehind(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.requestVisibleBehind(Z)Z"); return false; }
    public void onVisibleBehindCanceled() { jet.runtime.ShimStubs.hit("android.app.Activity.onVisibleBehindCanceled()V"); }
    public void onEnterAnimationComplete() { jet.runtime.ShimStubs.hit("android.app.Activity.onEnterAnimationComplete()V"); }
    public void setImmersive(boolean p0) { jet_Immersive = p0; }
    public void setVrModeEnabled(boolean p0, android.content.ComponentName p1) { jet.runtime.ShimStubs.hit("android.app.Activity.setVrModeEnabled(ZLandroid/content/ComponentName;)V"); }
    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback p0) { jet.runtime.ShimStubs.hit("android.app.Activity.startActionMode(Landroid/view/ActionMode$Callback;)Landroid/view/ActionMode;"); return null; }
    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.startActionMode(Landroid/view/ActionMode$Callback;I)Landroid/view/ActionMode;"); return null; }
    public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback p0, int p1) { jet.runtime.ShimStubs.hit("android.app.Activity.onWindowStartingActionMode(Landroid/view/ActionMode$Callback;I)Landroid/view/ActionMode;"); return null; }
    public boolean navigateUpToFromChild(android.app.Activity p0, android.content.Intent p1) { jet.runtime.ShimStubs.hit("android.app.Activity.navigateUpToFromChild(Landroid/app/Activity;Landroid/content/Intent;)Z"); return false; }
    public void showLockTaskEscapeMessage() { jet.runtime.ShimStubs.hit("android.app.Activity.showLockTaskEscapeMessage()V"); }
    private boolean jetRecentsScreenshot = true;
    /** Recorded; the iOS host honours it by blanking the app-switcher snapshot. */
    public void setRecentsScreenshotEnabled(boolean e) { jetRecentsScreenshot = e; }
    public boolean jetRecentsScreenshotEnabled() { return jetRecentsScreenshot; }
    public void setInheritShowWhenLocked(boolean p0) { jet.runtime.ShimStubs.hit("android.app.Activity.setInheritShowWhenLocked(Z)V"); }
    // ---- END generated SDK surface ----
}
