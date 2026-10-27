package android.util;
import java.util.TreeMap;
public class SparseIntArray implements Cloneable {
    TreeMap<Integer, Integer> m = new TreeMap<>();
    public SparseIntArray() {} public SparseIntArray(int cap) {}
    public int get(int k) { return get(k, 0); }
    public int get(int k, int d) { Integer v = m.get(k); return v != null ? v : d; }
    public void put(int k, int v) { m.put(k, v); } public void append(int k, int v) { m.put(k, v); }
    public void delete(int k) { m.remove(k); } public void removeAt(int i) { m.remove(keyAt(i)); }
    public int size() { return m.size(); }
    public int keyAt(int i) { int j = 0; for (Integer k : m.keySet()) if (j++ == i) return k; throw new ArrayIndexOutOfBoundsException(i); }
    public int valueAt(int i) { return m.get(keyAt(i)); } public void setValueAt(int i, int v) { m.put(keyAt(i), v); }
    public int indexOfKey(int k) { int j = 0; for (Integer x : m.keySet()) { if (x == k) return j; j++; } return -1; }
    public int indexOfValue(int v) { int j = 0; for (Integer x : m.values()) { if (x == v) return j; j++; } return -1; }
    public void clear() { m.clear(); }
    public SparseIntArray clone() { SparseIntArray c = new SparseIntArray(); c.m = new TreeMap<>(m); return c; }
}
