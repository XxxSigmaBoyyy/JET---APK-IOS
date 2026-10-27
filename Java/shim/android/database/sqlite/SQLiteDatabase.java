package android.database.sqlite;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import java.sql.*;
import java.util.*;
/** Android SQLiteDatabase over SQLite (host: sqlite-jdbc; iOS target: system libsqlite3). */
public final class SQLiteDatabase extends SQLiteClosable {
    public interface CursorFactory { Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query); }
    public static final int CONFLICT_NONE = 0, CONFLICT_ROLLBACK = 1, CONFLICT_ABORT = 2, CONFLICT_FAIL = 3, CONFLICT_IGNORE = 4, CONFLICT_REPLACE = 5;
    public static final int OPEN_READWRITE = 0, OPEN_READONLY = 1, CREATE_IF_NECESSARY = 0x10000000, NO_LOCALIZED_COLLATORS = 0x10;
    private static final String[] CONFLICT = {"", " OR ROLLBACK", " OR ABORT", " OR FAIL", " OR IGNORE", " OR REPLACE"};
    private final Connection c; private final String path; private int txDepth; private boolean txOk = true, txMarked;
    /** Android registers LOCALIZED and UNICODE collators on every connection (sqlite3_android.cpp). */
    private static void jetRegisterCollations(java.sql.Connection c) {
        try {
            for (String name : new String[] { "UNICODE", "LOCALIZED" }) {
                final java.text.Collator col = java.text.Collator.getInstance(name.equals("UNICODE") ? java.util.Locale.ROOT : java.util.Locale.getDefault());
                col.setStrength(name.equals("UNICODE") ? java.text.Collator.PRIMARY : java.text.Collator.SECONDARY);
                org.sqlite.Collation.create(c, name, new org.sqlite.Collation() { @Override protected int xCompare(String a, String b) { synchronized (col) { return col.compare(a, b); } } });
            }
        } catch (Throwable t) { System.err.println("[jet-sqlite] collation registration failed: " + t); }
    }
    private SQLiteDatabase(String path) {
        this.path = path;
        try { Class.forName("org.sqlite.JDBC"); c = DriverManager.getConnection("jdbc:sqlite:" + (path == null ? ":memory:" : path)); jetRegisterCollations(c); }
        catch (Exception e) { throw new SQLiteException("open " + path, e); }
    }
    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory f) { return new SQLiteDatabase(path); }
    public static SQLiteDatabase openOrCreateDatabase(java.io.File f, CursorFactory cf) { f.getParentFile().mkdirs(); return new SQLiteDatabase(f.getPath()); }
    public static SQLiteDatabase openDatabase(String path, CursorFactory f, int flags) { return new SQLiteDatabase(path); }
    public static SQLiteDatabase create(CursorFactory f) { return new SQLiteDatabase(null); }
    public String getPath() { return path; }
    public boolean isOpen() { try { return !c.isClosed(); } catch (SQLException e) { return false; } }
    public boolean isReadOnly() { return false; }
    @Override protected void onAllReferencesReleased() { try { c.close(); } catch (SQLException ignored) {} }
    public int getVersion() { Cursor cu = rawQuery("PRAGMA user_version", null); try { return cu.moveToFirst() ? cu.getInt(0) : 0; } finally { cu.close(); } }
    public void setVersion(int v) { execSQL("PRAGMA user_version = " + v); }
    public long getMaximumSize() { return Long.MAX_VALUE; }
    public void setLocale(Locale l) {}
    public boolean enableWriteAheadLogging() { return false; }
    public void disableWriteAheadLogging() {}
    public void setForeignKeyConstraintsEnabled(boolean on) { execSQL("PRAGMA foreign_keys = " + (on ? "ON" : "OFF")); }
    private static SQLiteException wrap(SQLException e, String sql) {
        String m = e.getMessage() + " (sql: " + sql + ")";
        return (m.contains("CONSTRAINT") || m.contains("constraint")) ? new SQLiteConstraintException(m) : new SQLiteException(m, e);
    }
    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a instanceof Boolean) a = ((Boolean) a) ? 1 : 0;
            ps.setObject(i + 1, a);
        }
    }
    public void execSQL(String sql) { execSQL(sql, null); }
    public void execSQL(String sql, Object[] args) {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); ps.execute(); } catch (SQLException e) { throw wrap(e, sql); }
    }
    public Cursor rawQuery(String sql, String[] args) { return jetQuery(sql, args); }
    android.database.MatrixCursor jetQuery(String sql, Object[] args) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            boolean has = ps.execute();
            if (!has) return new MatrixCursor(new String[0]);
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData md = rs.getMetaData(); int n = md.getColumnCount();
                String[] cols = new String[n]; for (int i = 0; i < n; i++) cols[i] = md.getColumnLabel(i + 1);
                MatrixCursor mc = new MatrixCursor(cols);
                while (rs.next()) { Object[] r = new Object[n]; for (int i = 0; i < n; i++) r[i] = rs.getObject(i + 1); mc.addRow(r); }
                return mc;
            }
        } catch (SQLException e) { throw wrap(e, sql); }
    }
    public Cursor rawQueryWithFactory(CursorFactory f, String sql, String[] args, String table) {
        SQLiteDirectCursorDriver d = new SQLiteDirectCursorDriver(this, sql, table);
        return d.query(f, args);
    }
    public Cursor query(String table, String[] cols, String sel, String[] args, String groupBy, String having, String orderBy) { return query(false, table, cols, sel, args, groupBy, having, orderBy, null); }
    public Cursor query(String table, String[] cols, String sel, String[] args, String groupBy, String having, String orderBy, String limit) { return query(false, table, cols, sel, args, groupBy, having, orderBy, limit); }
    public Cursor query(boolean distinct, String table, String[] cols, String sel, String[] args, String groupBy, String having, String orderBy, String limit) {
        StringBuilder sb = new StringBuilder("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        sb.append(cols == null || cols.length == 0 ? "*" : String.join(", ", cols)).append(" FROM ").append(table);
        if (sel != null && !sel.isEmpty()) sb.append(" WHERE ").append(sel);
        if (groupBy != null && !groupBy.isEmpty()) sb.append(" GROUP BY ").append(groupBy);
        if (having != null && !having.isEmpty()) sb.append(" HAVING ").append(having);
        if (orderBy != null && !orderBy.isEmpty()) sb.append(" ORDER BY ").append(orderBy);
        if (limit != null && !limit.isEmpty()) sb.append(" LIMIT ").append(limit);
        return rawQuery(sb.toString(), args);
    }
    public long insert(String table, String nullHack, ContentValues v) { try { return insertWithOnConflict(table, nullHack, v, CONFLICT_NONE); } catch (SQLiteException e) { System.err.println("[jet] sqlite insert: " + e.getMessage()); return -1; } }
    public long insertOrThrow(String table, String nullHack, ContentValues v) { return insertWithOnConflict(table, nullHack, v, CONFLICT_NONE); }
    public long replace(String table, String nullHack, ContentValues v) { try { return insertWithOnConflict(table, nullHack, v, CONFLICT_REPLACE); } catch (SQLiteException e) { return -1; } }
    public long replaceOrThrow(String table, String nullHack, ContentValues v) { return insertWithOnConflict(table, nullHack, v, CONFLICT_REPLACE); }
    public long insertWithOnConflict(String table, String nullHack, ContentValues v, int conflict) {
        StringBuilder sb = new StringBuilder("INSERT").append(CONFLICT[conflict]).append(" INTO ").append(table);
        List<Object> args = new ArrayList<>();
        if (v == null || v.size() == 0) sb.append(" (").append(nullHack).append(") VALUES (NULL)");
        else {
            sb.append(" ("); StringJoiner q = new StringJoiner(","); StringJoiner k = new StringJoiner(",");
            for (String key : v.keySet()) { k.add(key); q.add("?"); args.add(v.get(key)); }
            sb.append(k).append(") VALUES (").append(q).append(")");
        }
        execSQL(sb.toString(), args.toArray());
        Cursor cu = rawQuery("SELECT last_insert_rowid()", null);
        try { return cu.moveToFirst() ? cu.getLong(0) : -1; } finally { cu.close(); }
    }
    public int update(String table, ContentValues v, String where, String[] whereArgs) { return updateWithOnConflict(table, v, where, whereArgs, CONFLICT_NONE); }
    public int updateWithOnConflict(String table, ContentValues v, String where, String[] whereArgs, int conflict) {
        StringBuilder sb = new StringBuilder("UPDATE").append(CONFLICT[conflict]).append(' ').append(table).append(" SET ");
        List<Object> args = new ArrayList<>(); StringJoiner sj = new StringJoiner(", ");
        for (String key : v.keySet()) { sj.add(key + "=?"); args.add(v.get(key)); }
        sb.append(sj);
        if (where != null && !where.isEmpty()) sb.append(" WHERE ").append(where);
        if (whereArgs != null) args.addAll(Arrays.asList(whereArgs));
        return changes(sb.toString(), args.toArray());
    }
    public int delete(String table, String where, String[] whereArgs) {
        return changes("DELETE FROM " + table + (where != null && !where.isEmpty() ? " WHERE " + where : ""), whereArgs);
    }
    private int changes(String sql, Object[] args) {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); return ps.executeUpdate(); } catch (SQLException e) { throw wrap(e, sql); }
    }
    public void beginTransaction() { if (txDepth++ == 0) { txOk = true; execSQL("BEGIN"); } txMarked = false; }
    /** AOSP: listener gets onBegin now; onCommit/onRollback are not delivered on this host (logged once). */
    public void beginTransactionWithListener(android.database.sqlite.SQLiteTransactionListener l) { beginTransaction(); if (l != null) l.onBegin(); }
    public void beginTransactionWithListenerNonExclusive(android.database.sqlite.SQLiteTransactionListener l) { beginTransactionWithListener(l); }
    public void beginTransactionNonExclusive() { beginTransaction(); }
    public void setTransactionSuccessful() { txMarked = true; }
    public void endTransaction() {
        if (!txMarked) txOk = false; txMarked = false;
        if (--txDepth == 0) execSQL(txOk ? "COMMIT" : "ROLLBACK");
    }
    public boolean inTransaction() { return txDepth > 0; }
    public boolean isDbLockedByCurrentThread() { return false; }
    private boolean mJetWal;
    public void setMaxSqlCacheSize(int n) { if (n > 100 || n < 0) throw new IllegalStateException("expected value between 0 and 100"); }
    public boolean isWriteAheadLoggingEnabled() { return mJetWal; }
    public long getPageSize() { Cursor cu = rawQuery("PRAGMA page_size", null); try { return cu.moveToFirst() ? cu.getLong(0) : 4096; } finally { cu.close(); } }
    public void setPageSize(long n) { execSQL("PRAGMA page_size = " + n); }
    public long setMaximumSize(long n) { long ps = getPageSize(); long pages = (n + ps - 1) / ps; Cursor cu = rawQuery("PRAGMA max_page_count = " + pages, null); try { return cu.moveToFirst() ? cu.getLong(0) * ps : n; } finally { cu.close(); } }
    public boolean yieldIfContendedSafely() { return false; }
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) { return false; }
    public boolean needUpgrade(int newVersion) { return newVersion > getVersion(); }
    public boolean isDatabaseIntegrityOk() { Cursor cu = rawQuery("PRAGMA integrity_check", null); try { return cu.moveToFirst() && "ok".equalsIgnoreCase(cu.getString(0)); } finally { cu.close(); } }

    public SQLiteStatement compileStatement(String sql) { return new SQLiteStatement(this, sql, null); }
    void jetExec(String sql, Object[] args) { execSQL(sql, args); }
    int jetChanges(String sql, Object[] args) { return changes(sql, args); }
    long jetLastInsertRowId() { Cursor cu = rawQuery("SELECT last_insert_rowid()", null); try { return cu.moveToFirst() ? cu.getLong(0) : -1; } finally { cu.close(); } }
    Object jetSimpleQuery(String sql, Object[] args) {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (java.sql.ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getObject(1) : null; } } catch (SQLException e) { throw wrap(e, sql); }
    }
}
