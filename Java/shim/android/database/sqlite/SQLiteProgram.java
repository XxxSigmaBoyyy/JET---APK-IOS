package android.database.sqlite;
/** AOSP-shaped program: SQL + 1-based bind args, executed through the owning SQLiteDatabase (JDBC). */
public abstract class SQLiteProgram extends SQLiteClosable {
    final SQLiteDatabase mDatabase; final String mSql; Object[] mBindArgs;
    /** For auto-stubbed SDK subclasses (e.g. SQLiteQuery) whose generated ctor calls super(). */
    protected SQLiteProgram() { this(null, "", null); }
    SQLiteProgram(SQLiteDatabase db, String sql, Object[] bindArgs) {
        mDatabase = db; mSql = sql.trim();
        int n = 0; boolean q = false; char qc = 0;
        for (int i = 0; i < mSql.length(); i++) { char ch = mSql.charAt(i); if (q) { if (ch == qc) q = false; } else if (ch == '\'' || ch == '"') { q = true; qc = ch; } else if (ch == '?') n++; }
        mBindArgs = new Object[n];
        if (bindArgs != null) System.arraycopy(bindArgs, 0, mBindArgs, 0, Math.min(n, bindArgs.length));
    }
    private void put(int index, Object v) {
        if (index < 1 || index > mBindArgs.length) throw new IllegalArgumentException("Cannot bind argument at index " + index + " because the index is out of range.  The statement has " + mBindArgs.length + " parameters.");
        mBindArgs[index - 1] = v;
    }
    public void bindNull(int index) { put(index, null); }
    public void bindLong(int index, long value) { put(index, value); }
    public void bindDouble(int index, double value) { put(index, value); }
    public void bindString(int index, String value) { if (value == null) throw new IllegalArgumentException("the bind value at index " + index + " is null"); put(index, value); }
    public void bindBlob(int index, byte[] value) { if (value == null) throw new IllegalArgumentException("the bind value at index " + index + " is null"); put(index, value); }
    public void bindAllArgsAsStrings(String[] args) { if (args != null) for (int i = args.length; i != 0; i--) bindString(i, args[i - 1]); }
    public void clearBindings() { java.util.Arrays.fill(mBindArgs, null); }
    @Override protected void onAllReferencesReleased() { clearBindings(); }
}
