package android.window;
/** JET: API 31 SplashScreen. The host shows no system splash window, so the exit listener is never invoked
 *  (as on a warm start); the app's content is shown directly after onCreate. Theme is recorded only. */
public interface SplashScreen {
    int SPLASH_SCREEN_STYLE_UNDEFINED = -1, SPLASH_SCREEN_STYLE_SOLID_COLOR = 0, SPLASH_SCREEN_STYLE_ICON = 1;
    void setOnExitAnimationListener(OnExitAnimationListener listener);
    void clearOnExitAnimationListener();
    void setSplashScreenTheme(int themeId);
    interface OnExitAnimationListener { void onSplashScreenExit(SplashScreenView view); }
    final class Jet implements SplashScreen {
        public OnExitAnimationListener listener; public int theme;
        public void setOnExitAnimationListener(OnExitAnimationListener l) { listener = l; }
        public void clearOnExitAnimationListener() { listener = null; }
        public void setSplashScreenTheme(int t) { theme = t; }
    }
}
