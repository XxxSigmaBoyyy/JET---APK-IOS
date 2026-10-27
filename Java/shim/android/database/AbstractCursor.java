package android.database;
/** android.database.AbstractCursor: position bookkeeping, observers, extras and notification URI (AOSP semantics, no CursorWindow). */
public abstract class AbstractCursor implements Cursor {
    protected int mPos = -1;
    protected boolean mClosed;
    protected android.content.ContentResolver mContentResolver;
    private android.net.Uri mNotifyUri; private java.util.List<android.net.Uri> mNotifyUris;
    private android.os.Bundle mExtras = android.os.Bundle.EMPTY;
    private final java.util.List<ContentObserver> mContentObservers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<DataSetObserver> mDataSetObservers = new java.util.concurrent.CopyOnWriteArrayList<>();
    public AbstractCursor() {}
    public abstract int getCount();
    public abstract String[] getColumnNames();
    public abstract String getString(int column);
    public abstract short getShort(int column);
    public abstract int getInt(int column);
    public abstract long getLong(int column);
    public abstract float getFloat(int column);
    public abstract double getDouble(int column);
    public abstract boolean isNull(int column);
    public int getType(int column) { return FIELD_TYPE_STRING; }
    public byte[] getBlob(int column) { throw new UnsupportedOperationException("getBlob is not supported"); }
    public int getColumnCount() { return getColumnNames().length; }
    public void deactivate() { for (DataSetObserver o : mDataSetObservers) o.onInvalidated(); }
    public boolean requery() { for (DataSetObserver o : mDataSetObservers) o.onChanged(); return true; }
    public boolean isClosed() { return mClosed; }
    public void close() { mClosed = true; mContentObservers.clear(); deactivate(); }
    public boolean onMove(int oldPosition, int newPosition) { return true; }
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) { String r = getString(columnIndex); if (r != null) { char[] d = r.toCharArray(); buffer.data = d; buffer.sizeCopied = d.length; } else buffer.sizeCopied = 0; }
    public int getPosition() { return mPos; }
    public boolean moveToPosition(int position) {
        final int count = getCount();
        if (position >= count) { mPos = count; return false; }
        if (position < 0) { mPos = -1; return false; }
        if (position == mPos) return true;
        boolean result = onMove(mPos, position);
        if (!result) mPos = -1; else mPos = position;
        return result;
    }
    public boolean move(int offset) { return moveToPosition(mPos + offset); }
    public boolean moveToFirst() { return moveToPosition(0); }
    public boolean moveToLast() { return moveToPosition(getCount() - 1); }
    public boolean moveToNext() { return moveToPosition(mPos + 1); }
    public boolean moveToPrevious() { return moveToPosition(mPos - 1); }
    public boolean isFirst() { return mPos == 0 && getCount() != 0; }
    public boolean isLast() { int c = getCount(); return mPos == (c - 1) && c != 0; }
    public boolean isBeforeFirst() { return getCount() == 0 || mPos == -1; }
    public boolean isAfterLast() { return getCount() == 0 || mPos == getCount(); }
    public int getColumnIndex(String columnName) {
        int periodIndex = columnName.lastIndexOf('.'); if (periodIndex != -1) columnName = columnName.substring(periodIndex + 1);
        String[] cols = getColumnNames(); for (int i = 0; i < cols.length; i++) if (cols[i].equalsIgnoreCase(columnName)) return i;
        return -1;
    }
    public int getColumnIndexOrThrow(String columnName) { int i = getColumnIndex(columnName); if (i < 0) throw new IllegalArgumentException("column '" + columnName + "' does not exist. Available columns: " + java.util.Arrays.toString(getColumnNames())); return i; }
    public String getColumnName(int columnIndex) { return getColumnNames()[columnIndex]; }
    public void registerContentObserver(ContentObserver observer) { mContentObservers.add(observer); }
    public void unregisterContentObserver(ContentObserver observer) { mContentObservers.remove(observer); }
    public void registerDataSetObserver(DataSetObserver observer) { mDataSetObservers.add(observer); }
    public void unregisterDataSetObserver(DataSetObserver observer) { mDataSetObservers.remove(observer); }
    protected void onChange(boolean selfChange) { for (ContentObserver o : mContentObservers) o.dispatchChange(selfChange); }
    public void setNotificationUri(android.content.ContentResolver cr, android.net.Uri notifyUri) { mContentResolver = cr; mNotifyUri = notifyUri; mNotifyUris = notifyUri == null ? null : java.util.Collections.singletonList(notifyUri); }
    public void setNotificationUris(android.content.ContentResolver cr, java.util.List<android.net.Uri> notifyUris) { mContentResolver = cr; mNotifyUris = notifyUris; mNotifyUri = notifyUris == null || notifyUris.isEmpty() ? null : notifyUris.get(0); }
    public android.net.Uri getNotificationUri() { return mNotifyUri; }
    public java.util.List<android.net.Uri> getNotificationUris() { return mNotifyUris; }
    public boolean getWantsAllOnMoveCalls() { return false; }
    public void setExtras(android.os.Bundle extras) { mExtras = extras == null ? android.os.Bundle.EMPTY : extras; }
    public android.os.Bundle getExtras() { return mExtras; }
    public android.os.Bundle respond(android.os.Bundle extras) { return android.os.Bundle.EMPTY; }
    protected void checkPosition() { if (-1 == mPos || getCount() == mPos) throw new CursorIndexOutOfBoundsException(mPos, getCount()); }
    @Override protected void finalize() {}
}
