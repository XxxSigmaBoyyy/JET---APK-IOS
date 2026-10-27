package android.database.sqlite;
import android.content.Context;
public abstract class SQLiteOpenHelper implements AutoCloseable {
    private final Context ctx; private final String name; private final int version; private SQLiteDatabase db;
    public SQLiteOpenHelper(Context c, String name, SQLiteDatabase.CursorFactory f, int version) { this.ctx = c; this.name = name; this.version = version; }
    public SQLiteOpenHelper(Context c, String name, SQLiteDatabase.CursorFactory f, int version, android.database.DatabaseErrorHandler errorHandler) { this(c, name, f, version); }
    public String getDatabaseName() { return name; }
    public void setWriteAheadLoggingEnabled(boolean e) {}
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (db != null && db.isOpen()) return db;
        SQLiteDatabase d = name == null ? SQLiteDatabase.create(null) : SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name), null);
        onConfigure(d);
        int v = d.getVersion();
        if (v != version) {
            d.beginTransaction();
            try {
                if (v == 0) onCreate(d); else if (v < version) onUpgrade(d, v, version); else onDowngrade(d, v, version);
                d.setVersion(version); d.setTransactionSuccessful();
            } finally { d.endTransaction(); }
        }
        onOpen(d);
        db = d; return d;
    }
    public SQLiteDatabase getReadableDatabase() { return getWritableDatabase(); }
    public synchronized void close() { if (db != null) { db.close(); db = null; } }
    public void onConfigure(SQLiteDatabase db) {}
    public abstract void onCreate(SQLiteDatabase db);
    public abstract void onUpgrade(SQLiteDatabase db, int oldV, int newV);
    public void onDowngrade(SQLiteDatabase db, int oldV, int newV) { throw new SQLiteException("Can't downgrade database from version " + oldV + " to " + newV); }
    public void onOpen(SQLiteDatabase db) {}
}
