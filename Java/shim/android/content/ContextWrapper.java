package android.content;
import android.content.res.Resources;
public class ContextWrapper extends Context {
    Context base;
    public ContextWrapper(Context base) { this.base = base; if (base != null) mResources = base.getResources(); }
    public void attachBaseContext(Context base) { this.base = base; if (base != null) mResources = base.getResources(); }
    public Context getBaseContext() { return base; }
    /** AOSP: delegate to the base context (returning the wrapper itself makes AppCompat's ContextThemeWrapper.getResources recurse). */
    @Override public Context createConfigurationContext(android.content.res.Configuration c) { return base != null ? base.createConfigurationContext(c) : super.createConfigurationContext(c); }
    @Override public Resources getResources() { return base != null ? base.getResources() : super.getResources(); }
    /** AOSP: ContextWrapper.getTheme/setTheme delegate to mBase (Hilt FragmentContextWrapper etc. otherwise got a fresh empty Theme). */
    @Override public Resources.Theme getTheme() { return base != null && base != this ? base.getTheme() : super.getTheme(); }
    @Override public void setTheme(int resid) { if (base != null && base != this) base.setTheme(resid); else super.setTheme(resid); }
    @Override public String getPackageName() { return base != null ? base.getPackageName() : super.getPackageName(); }
}
