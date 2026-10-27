package android.content;

import android.content.pm.ProviderInfo;

/** ContentProvider lifecycle: attachInfo -> onCreate (run by the host before Application.onCreate). Queries return null. */
public abstract class ContentProvider implements ComponentCallbacks2 {
    private Context mContext; private String mAuthority;
    public ContentProvider() {}
    public boolean onCreate() { return true; }
    public void attachInfo(Context context, ProviderInfo info) {
        if (mContext != null) return;
        mContext = context;
        if (info != null) mAuthority = info.authority;
        onCreate();
    }
    public final Context getContext() { return mContext; }
    public final Context requireContext() { if (mContext == null) throw new IllegalStateException("Cannot find context from the provider."); return mContext; }
    public final String getCallingPackage() { return mContext != null ? mContext.getPackageName() : null; }
    protected final void setAuthorities(String a) { mAuthority = a; }
    public final String getAuthority() { return mAuthority; }
    public android.database.Cursor query(android.net.Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    public String getType(android.net.Uri uri) { return null; }
    public android.net.Uri insert(android.net.Uri uri, ContentValues values) { return null; }
    public int delete(android.net.Uri uri, String selection, String[] selectionArgs) { return 0; }
    public int update(android.net.Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    public void shutdown() {}
    public void onConfigurationChanged(android.content.res.Configuration c) {}
    public void onLowMemory() {}
    public void onTrimMemory(int level) {}
}
