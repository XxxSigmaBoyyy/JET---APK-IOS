package android.content;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import java.io.*;
/** Host ContentResolver: no content providers are installed, so queries return null (as Android does for an unknown authority). file:// and android.resource:// streams work. */
public class ContentResolver {
    public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
    public static final java.lang.String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";
    public static final java.lang.String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";
    public static final java.lang.String EXTRA_HONORED_ARGS = "android.content.extra.HONORED_ARGS";
    public static final java.lang.String EXTRA_REFRESH_SUPPORTED = "android.content.extra.REFRESH_SUPPORTED";
    public static final java.lang.String EXTRA_SIZE = "android.content.extra.SIZE";
    public static final java.lang.String EXTRA_TOTAL_COUNT = "android.content.extra.TOTAL_COUNT";
    public static final int NOTIFY_DELETE = 16;
    public static final int NOTIFY_INSERT = 4;
    public static final int NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS = 2;
    public static final int NOTIFY_SYNC_TO_NETWORK = 1;
    public static final int NOTIFY_UPDATE = 8;
    public static final java.lang.String QUERY_ARG_GROUP_COLUMNS = "android:query-arg-group-columns";
    public static final java.lang.String QUERY_ARG_LIMIT = "android:query-arg-limit";
    public static final java.lang.String QUERY_ARG_OFFSET = "android:query-arg-offset";
    public static final java.lang.String QUERY_ARG_SORT_COLLATION = "android:query-arg-sort-collation";
    public static final java.lang.String QUERY_ARG_SORT_COLUMNS = "android:query-arg-sort-columns";
    public static final java.lang.String QUERY_ARG_SORT_DIRECTION = "android:query-arg-sort-direction";
    public static final java.lang.String QUERY_ARG_SORT_LOCALE = "android:query-arg-sort-locale";
    public static final java.lang.String QUERY_ARG_SQL_GROUP_BY = "android:query-arg-sql-group-by";
    public static final java.lang.String QUERY_ARG_SQL_HAVING = "android:query-arg-sql-having";
    public static final java.lang.String QUERY_ARG_SQL_LIMIT = "android:query-arg-sql-limit";
    public static final java.lang.String QUERY_ARG_SQL_SELECTION = "android:query-arg-sql-selection";
    public static final java.lang.String QUERY_ARG_SQL_SELECTION_ARGS = "android:query-arg-sql-selection-args";
    public static final java.lang.String QUERY_ARG_SQL_SORT_ORDER = "android:query-arg-sql-sort-order";
    public static final int QUERY_SORT_DIRECTION_ASCENDING = 0;
    public static final int QUERY_SORT_DIRECTION_DESCENDING = 1;
    public static final java.lang.String SCHEME_ANDROID_RESOURCE = "android.resource";
    public static final java.lang.String SCHEME_CONTENT = "content";
    public static final java.lang.String SCHEME_FILE = "file";
    public static final java.lang.String SYNC_EXTRAS_ACCOUNT = "account";
    public static final java.lang.String SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS = "discard_deletions";
    public static final java.lang.String SYNC_EXTRAS_DO_NOT_RETRY = "do_not_retry";
    public static final java.lang.String SYNC_EXTRAS_EXPEDITED = "expedited";
    public static final java.lang.String SYNC_EXTRAS_FORCE = "force";
    public static final java.lang.String SYNC_EXTRAS_IGNORE_BACKOFF = "ignore_backoff";
    public static final java.lang.String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";
    public static final java.lang.String SYNC_EXTRAS_INITIALIZE = "initialize";
    public static final java.lang.String SYNC_EXTRAS_MANUAL = "force";
    public static final java.lang.String SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS = "deletions_override";
    public static final java.lang.String SYNC_EXTRAS_REQUIRE_CHARGING = "require_charging";
    public static final java.lang.String SYNC_EXTRAS_SCHEDULE_AS_EXPEDITED_JOB = "schedule_as_expedited_job";
    public static final java.lang.String SYNC_EXTRAS_UPLOAD = "upload";
    public static final int SYNC_OBSERVER_TYPE_ACTIVE = 4;
    public static final int SYNC_OBSERVER_TYPE_PENDING = 2;
    public static final int SYNC_OBSERVER_TYPE_SETTINGS = 1;

    private final Context mContext;
    public ContentResolver() { this(null); }
    public ContentResolver(Context ctx) { mContext = ctx; }
    private static void log(String what, Uri u) { System.err.println("[CONTENT] " + what + " " + u + " -> no provider"); }
    public final Cursor query(Uri u, String[] p, String s, String[] a, String o) { if (jet.runtime.JetMediaProvider.handles(u)) return jet.runtime.JetMediaProvider.query(u, p, s, a, o); log("query", u); return null; }
    public final Cursor query(Uri u, String[] p, String s, String[] a, String o, CancellationSignal c) { return query(u, p, s, a, o); }
    public final Cursor query(Uri u, String[] p, Bundle q, CancellationSignal c) {
        if (jet.runtime.JetMediaProvider.handles(u)) {
            String sel = q == null ? null : q.getString("android:query-arg-sql-selection"); String[] args = q == null ? null : q.getStringArray("android:query-arg-sql-selection-args");
            String ord = q == null ? null : q.getString("android:query-arg-sql-sort-order");
            if (ord == null && q != null && q.getStringArray("android:query-arg-sort-columns") != null) { String[] sc = q.getStringArray("android:query-arg-sort-columns"); ord = sc.length > 0 ? sc[0] + (q.getInt("android:query-arg-sort-direction", 0) == 1 ? " DESC" : " ASC") : null; }
            if (q != null && q.getInt("android:query-arg-limit", 0) > 0) ord = (ord == null ? "_id" : ord) + " LIMIT " + q.getInt("android:query-arg-limit", 0);
            return jet.runtime.JetMediaProvider.query(u, p, sel, args, ord);
        }
        log("query", u); return null;
    }
    public final Uri insert(Uri u, ContentValues v) { log("insert", u); return null; }
    public final Uri insert(Uri u, ContentValues v, Bundle extras) { return insert(u, v); }
    public final int bulkInsert(Uri u, ContentValues[] v) { log("bulkInsert", u); return 0; }
    public final int delete(Uri u, String w, String[] a) { log("delete", u); return 0; }
    public final int delete(Uri u, Bundle extras) { return delete(u, null, null); }
    public final int update(Uri u, ContentValues v, String w, String[] a) { log("update", u); return 0; }
    public final int update(Uri u, ContentValues v, Bundle extras) { return update(u, v, null, null); }
    public final String getType(Uri u) { return null; }
    public String[] getStreamTypes(Uri u, String filter) { return null; }
    public final Bundle call(Uri u, String m, String arg, Bundle extras) { log("call " + m, u); return null; }
    public final Bundle call(String authority, String m, String arg, Bundle extras) { System.err.println("[CONTENT] call " + authority + " -> no provider"); return null; }
    private File file(Uri u) throws FileNotFoundException {
        if (u != null && "file".equals(u.getScheme()) && u.getPath() != null) return new File(u.getPath());
        throw new FileNotFoundException("No content provider: " + u);
    }
    public final InputStream openInputStream(Uri u) throws FileNotFoundException {
        if (u != null && "android.resource".equals(u.getScheme()) && mContext != null) {
            try { String last = u.getLastPathSegment(); int id = Integer.parseInt(last); return mContext.getResources().openRawResource(id); } catch (RuntimeException e) { throw new FileNotFoundException("Resource does not exist: " + u); }
        }
        return new FileInputStream(file(u));
    }
    public final OutputStream openOutputStream(Uri u) throws FileNotFoundException { return new FileOutputStream(file(u)); }
    public final OutputStream openOutputStream(Uri u, String mode) throws FileNotFoundException { return new FileOutputStream(file(u), mode != null && mode.contains("a")); }
    public final android.os.ParcelFileDescriptor openFileDescriptor(Uri u, String mode) throws FileNotFoundException { throw new FileNotFoundException("No content provider: " + u); }
    public final android.os.ParcelFileDescriptor openFileDescriptor(Uri u, String mode, CancellationSignal c) throws FileNotFoundException { return openFileDescriptor(u, mode); }
    public final android.content.res.AssetFileDescriptor openAssetFileDescriptor(Uri u, String mode) throws FileNotFoundException { throw new FileNotFoundException("No content provider: " + u); }
    public final android.content.res.AssetFileDescriptor openAssetFileDescriptor(Uri u, String mode, CancellationSignal c) throws FileNotFoundException { return openAssetFileDescriptor(u, mode); }
    public final android.content.res.AssetFileDescriptor openTypedAssetFileDescriptor(Uri u, String mime, Bundle o) throws FileNotFoundException { return openAssetFileDescriptor(u, "r"); }
    public final void registerContentObserver(Uri u, boolean desc, android.database.ContentObserver o) {}
    public final void unregisterContentObserver(android.database.ContentObserver o) {}
    public void notifyChange(Uri u, android.database.ContentObserver o) {}
    public void notifyChange(Uri u, android.database.ContentObserver o, boolean sync) {}
    public void notifyChange(Uri u, android.database.ContentObserver o, int flags) {}
    public final ContentProviderClient acquireContentProviderClient(Uri u) { return null; }
    public final ContentProviderClient acquireContentProviderClient(String a) { return null; }
    public final ContentProviderClient acquireUnstableContentProviderClient(Uri u) { return null; }
    public final ContentProviderClient acquireUnstableContentProviderClient(String a) { return null; }
    public void takePersistableUriPermission(Uri u, int flags) {}
    public void releasePersistableUriPermission(Uri u, int flags) {}
    public java.util.List<android.content.UriPermission> getPersistedUriPermissions() { return new java.util.ArrayList<>(); }
    public java.util.List<android.content.UriPermission> getOutgoingPersistedUriPermissions() { return new java.util.ArrayList<>(); }
    public ContentProviderResult[] applyBatch(String a, java.util.ArrayList<ContentProviderOperation> ops) throws OperationApplicationException { throw new OperationApplicationException("No content provider: " + a); }
    public final Uri canonicalize(Uri u) { return null; } public final Uri uncanonicalize(Uri u) { return u; }
    public final boolean refresh(Uri u, Bundle extras, CancellationSignal c) { return false; }
    public static void requestSync(android.accounts.Account a, String auth, Bundle extras) {}
    public static boolean getMasterSyncAutomatically() { return false; }
    public static void setSyncAutomatically(android.accounts.Account a, String auth, boolean s) {}
    public static boolean getSyncAutomatically(android.accounts.Account a, String auth) { return false; }
    public final android.graphics.Bitmap loadThumbnail(Uri u, android.util.Size s, CancellationSignal c) throws IOException { throw new FileNotFoundException("No content provider: " + u); }
}
