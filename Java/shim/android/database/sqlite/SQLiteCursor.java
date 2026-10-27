package android.database.sqlite;
/** JET: executes the SQLiteQuery (with whatever the factory bound) eagerly into an in-memory window. */
public class SQLiteCursor extends android.database.MatrixCursor {
    private final SQLiteCursorDriver mDriver; private final String mEditTable; private final SQLiteQuery mQuery;
    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) { super(query.jetRun()); mDriver = driver; mEditTable = editTable; mQuery = query; }
    @Deprecated public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) { this(driver, editTable, query); }
    public SQLiteDatabase getDatabase() { return mQuery.mDatabase; }
    public void setSelectionArguments(String[] args) { mDriver.setBindArguments(args); }
    public void setFillWindowForwardOnly(boolean b) {}
}
