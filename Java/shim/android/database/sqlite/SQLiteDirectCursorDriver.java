package android.database.sqlite;
public final class SQLiteDirectCursorDriver implements SQLiteCursorDriver {
    private final SQLiteDatabase mDatabase; private final String mEditTable, mSql; private SQLiteQuery mQuery;
    SQLiteDirectCursorDriver(SQLiteDatabase db, String sql, String editTable) { mDatabase = db; mEditTable = editTable; mSql = sql; }
    public android.database.Cursor query(SQLiteDatabase.CursorFactory factory, String[] selectionArgs) {
        SQLiteQuery query = new SQLiteQuery(mDatabase, mSql, null);
        query.bindAllArgsAsStrings(selectionArgs);
        android.database.Cursor cursor = factory == null ? new SQLiteCursor(this, mEditTable, query) : factory.newCursor(mDatabase, this, mEditTable, query);
        mQuery = query; return cursor;
    }
    public void cursorClosed() {} public void setBindArguments(String[] a) { mQuery.bindAllArgsAsStrings(a); }
    public void cursorDeactivated() {} public void cursorRequeried(android.database.Cursor c) {}
    @Override public String toString() { return "SQLiteDirectCursorDriver: " + mSql; }
}
