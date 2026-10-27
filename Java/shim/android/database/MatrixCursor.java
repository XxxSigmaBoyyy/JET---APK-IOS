package android.database;
import java.util.*;
/** In-memory cursor; also the result type of JET's SQLiteDatabase queries. */
public class MatrixCursor extends AbstractCursor {
    private final String[] cols; private final ArrayList<Object[]> rows = new ArrayList<>(); private int pos = -1; private boolean closed;
    public MatrixCursor(String[] columnNames) { cols = columnNames; }
    /** JET: adopt another MatrixCursor's columns and rows (SQLiteCursor). */
    protected MatrixCursor(MatrixCursor src) { cols = src.cols; rows.addAll(src.rows); }
    public MatrixCursor(String[] columnNames, int cap) { this(columnNames); }
    public void addRow(Object[] r) { rows.add(Arrays.copyOf(r, cols.length)); }
    public void addRow(Iterable<?> r) { ArrayList<Object> l = new ArrayList<>(); for (Object o : r) l.add(o); addRow(l.toArray()); }
    public int getCount() { return rows.size(); }
    public int getPosition() { return pos; }
    public boolean move(int o) { return moveToPosition(pos + o); }
    public boolean moveToPosition(int p) { if (p < 0) { pos = -1; return false; } if (p >= rows.size()) { pos = rows.size(); return false; } pos = p; return true; }
    public boolean moveToFirst() { return moveToPosition(0); }
    public boolean moveToLast() { return moveToPosition(rows.size() - 1); }
    public boolean moveToNext() { return moveToPosition(pos + 1); }
    public boolean moveToPrevious() { return moveToPosition(pos - 1); }
    public boolean isFirst() { return pos == 0 && !rows.isEmpty(); }
    public boolean isLast() { return pos == rows.size() - 1 && !rows.isEmpty(); }
    public boolean isBeforeFirst() { return rows.isEmpty() || pos == -1; }
    public boolean isAfterLast() { return rows.isEmpty() || pos == rows.size(); }
    public int getColumnIndex(String n) { for (int i = 0; i < cols.length; i++) if (cols[i].equalsIgnoreCase(n)) return i; int d = n.lastIndexOf('.'); return d >= 0 ? getColumnIndex(n.substring(d + 1)) : -1; }
    public int getColumnIndexOrThrow(String n) { int i = getColumnIndex(n); if (i < 0) throw new IllegalArgumentException("column '" + n + "' does not exist"); return i; }
    public String getColumnName(int i) { return cols[i]; }
    public String[] getColumnNames() { return cols; }
    public int getColumnCount() { return cols.length; }
    private Object get(int i) { if (pos < 0 || pos >= rows.size()) throw new CursorIndexOutOfBoundsException(pos, rows.size()); return rows.get(pos)[i]; }
    public byte[] getBlob(int i) { Object o = get(i); return o instanceof byte[] ? (byte[]) o : o == null ? null : o.toString().getBytes(); }
    public String getString(int i) { Object o = get(i); return o == null ? null : o instanceof byte[] ? new String((byte[]) o) : (o instanceof Double && ((Double) o) == Math.rint((Double) o) && false) ? null : o.toString(); }
    private Number n(int i) { Object o = get(i); if (o instanceof Number) return (Number) o; if (o == null) return 0; try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; } }
    public short getShort(int i) { return n(i).shortValue(); }
    public int getInt(int i) { return n(i).intValue(); }
    public long getLong(int i) { return n(i).longValue(); }
    public float getFloat(int i) { return n(i).floatValue(); }
    public double getDouble(int i) { return n(i).doubleValue(); }
    public int getType(int i) { Object o = get(i); return o == null ? FIELD_TYPE_NULL : o instanceof byte[] ? FIELD_TYPE_BLOB : (o instanceof Double || o instanceof Float) ? FIELD_TYPE_FLOAT : o instanceof Number ? FIELD_TYPE_INTEGER : FIELD_TYPE_STRING; }
    public boolean isNull(int i) { return get(i) == null; }
    public void close() { closed = true; }
    public boolean isClosed() { return closed; }
    public void registerContentObserver(ContentObserver o) {}
    public void unregisterContentObserver(ContentObserver o) {}
    public void registerDataSetObserver(DataSetObserver o) {}
    public void unregisterDataSetObserver(DataSetObserver o) {}
    public android.os.Bundle getExtras() { return android.os.Bundle.EMPTY; }
}
