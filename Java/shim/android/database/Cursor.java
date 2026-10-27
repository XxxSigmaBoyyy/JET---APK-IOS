package android.database;
public interface Cursor extends java.io.Closeable {
    int FIELD_TYPE_NULL = 0, FIELD_TYPE_INTEGER = 1, FIELD_TYPE_FLOAT = 2, FIELD_TYPE_STRING = 3, FIELD_TYPE_BLOB = 4;
    int getCount(); int getPosition();
    boolean move(int offset); boolean moveToPosition(int p); boolean moveToFirst(); boolean moveToLast(); boolean moveToNext(); boolean moveToPrevious();
    boolean isFirst(); boolean isLast(); boolean isBeforeFirst(); boolean isAfterLast();
    int getColumnIndex(String n); int getColumnIndexOrThrow(String n); String getColumnName(int i); String[] getColumnNames(); int getColumnCount();
    byte[] getBlob(int i); String getString(int i); short getShort(int i); int getInt(int i); long getLong(int i); float getFloat(int i); double getDouble(int i);
    int getType(int i); boolean isNull(int i);
    void close(); boolean isClosed();
    void registerContentObserver(ContentObserver o); void unregisterContentObserver(ContentObserver o);
    void registerDataSetObserver(DataSetObserver o); void unregisterDataSetObserver(DataSetObserver o);
    android.os.Bundle getExtras();
}
