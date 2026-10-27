package android.util;
import java.util.TreeMap;
public class SparseArray<E> implements Cloneable {
    TreeMap<Integer, E> m = new TreeMap<>();
    public SparseArray() {} public SparseArray(int cap) {}
    public E get(int k) { return m.get(k); }
    public E get(int k, E d) { E v = m.get(k); return v != null ? v : d; }
    public void put(int k, E v) { m.put(k, v); }
    public void append(int k, E v) { m.put(k, v); }
    public void delete(int k) { m.remove(k); } public void remove(int k) { m.remove(k); }
    public void removeAt(int i) { m.remove(keyAt(i)); }
    public void removeAtRange(int i, int n) { for (int j = 0; j < n && i < size(); j++) removeAt(i); }
    public void set(int k, E v) { put(k, v); }
    public boolean contains(int k) { return m.containsKey(k); }
    public int size() { return m.size(); }
    public int keyAt(int i) { int j = 0; for (Integer k : m.keySet()) if (j++ == i) return k; throw new ArrayIndexOutOfBoundsException(i); }
    public E valueAt(int i) { return m.get(keyAt(i)); }
    public void setValueAt(int i, E v) { m.put(keyAt(i), v); }
    public int indexOfKey(int k) { int j = 0; for (Integer x : m.keySet()) { if (x == k) return j; j++; } return -1; }
    public int indexOfValue(E v) { int j = 0; for (E x : m.values()) { if (x == v) return j; j++; } return -1; }
    public void clear() { m.clear(); }
    @SuppressWarnings("unchecked") public SparseArray<E> clone() { SparseArray<E> c = new SparseArray<>(); c.m = new TreeMap<>(m); return c; }
    public void putAll(SparseArray<? extends E> o) { m.putAll(o.m); }
    public boolean contentEquals(SparseArray<?> o) { return o != null && m.equals(o.m); }
    @Override public String toString() { return m.toString(); }
}
