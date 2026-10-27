package android.database.sqlite;
public final class SQLiteStatement extends SQLiteProgram {
    SQLiteStatement(SQLiteDatabase db, String sql, Object[] bindArgs) { super(db, sql, bindArgs); }
    public void execute() { mDatabase.jetExec(mSql, mBindArgs.clone()); }
    public int executeUpdateDelete() { return mDatabase.jetChanges(mSql, mBindArgs.clone()); }
    public long executeInsert() { int n = mDatabase.jetChanges(mSql, mBindArgs.clone()); return n > 0 ? mDatabase.jetLastInsertRowId() : -1; }
    public long simpleQueryForLong() { Object v = mDatabase.jetSimpleQuery(mSql, mBindArgs.clone()); if (v == null) throw new SQLiteDoneException(); return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString()); }
    public String simpleQueryForString() { Object v = mDatabase.jetSimpleQuery(mSql, mBindArgs.clone()); return v == null ? null : v.toString(); }
    @Override public String toString() { return "SQLiteProgram: " + mSql; }
}
