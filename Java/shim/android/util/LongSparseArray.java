package android.util;
import java.util.TreeMap;
public class LongSparseArray<E> implements Cloneable {
    TreeMap<Long, E> m = new TreeMap<>();
    public LongSparseArray() {} public LongSparseArray(int cap) {}
    public E get(long k) { return m.get(k); } public E get(long k, E d) { E v = m.get(k); return v != null ? v : d; }
    public void put(long k, E v) { m.put(k, v); } public void append(long k, E v) { m.put(k, v); }
    public void delete(long k) { m.remove(k); } public void remove(long k) { m.remove(k); }
    public void removeAt(int i) { m.remove(keyAt(i)); }
    public int size() { return m.size(); }
    public long keyAt(int i) { int j = 0; for (Long k : m.keySet()) if (j++ == i) return k; throw new ArrayIndexOutOfBoundsException(i); }
    public E valueAt(int i) { return m.get(keyAt(i)); } public void setValueAt(int i, E v) { m.put(keyAt(i), v); }
    public int indexOfKey(long k) { int j = 0; for (Long x : m.keySet()) { if (x == k) return j; j++; } return -1; }
    public void clear() { m.clear(); }
    public LongSparseArray<E> clone() { LongSparseArray<E> c = new LongSparseArray<>(); c.m = new TreeMap<>(m); return c; }
}
