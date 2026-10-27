package android.view;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
public class ContextThemeWrapper extends ContextWrapper {
    private int themeRes;
    private Resources.Theme mWrapperTheme;
    private android.content.res.Configuration mOverride;
    public ContextThemeWrapper() { super(null); }
    public ContextThemeWrapper(Context base, int themeResId) { super(base); themeRes = themeResId; }
    public ContextThemeWrapper(Context base, Resources.Theme theme) { super(base); if (theme != null && getResources() != null) { mWrapperTheme = getResources().newTheme(); mWrapperTheme.jetSetPublish(false); mWrapperTheme.setTo(theme); } }
    @Override public Resources.Theme getTheme() {
        if (mWrapperTheme == null) {
            Resources r = getResources();
            if (r == null) return null;
            mWrapperTheme = r.newTheme();
            mWrapperTheme.jetSetPublish(false); // overlays must not replace the app jetLastTheme
            Context b = getBaseContext();
            if (b != null && b != this) { Resources.Theme bt = b.getTheme(); if (bt != null) mWrapperTheme.setTo(bt); }
            if (themeRes != 0) mWrapperTheme.applyStyle(themeRes, true);
        }
        return mWrapperTheme;
    }
    @Override public void setTheme(int resid) {
        if (themeRes == resid && mWrapperTheme != null) return;
        themeRes = resid; Resources.Theme t = getTheme(); if (t != null) t.applyStyle(resid, true);
    }
    public int getThemeResId() { return themeRes; }
    public void applyOverrideConfiguration(android.content.res.Configuration c) { mOverride = c; }
    public android.content.res.Configuration getOverrideConfiguration() { return mOverride; }
}
