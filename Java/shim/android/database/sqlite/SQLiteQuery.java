package android.database.sqlite;
public final class SQLiteQuery extends SQLiteProgram {
    SQLiteQuery(SQLiteDatabase db, String query, Object[] bindArgs) { super(db, query, bindArgs); }
    android.database.MatrixCursor jetRun() { return mDatabase.jetQuery(mSql, mBindArgs.clone()); }
    @Override public String toString() { return "SQLiteQuery: " + mSql; }
}
