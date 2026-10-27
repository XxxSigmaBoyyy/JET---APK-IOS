package android.util;
import java.util.TreeMap;
public class SparseLongArray implements Cloneable {
    TreeMap<Integer, Long> m = new TreeMap<>();
    public SparseLongArray() {} public SparseLongArray(int cap) {}
    public long get(int k) { return get(k, 0L); }
    public long get(int k, long d) { Long v = m.get(k); return v != null ? v : d; }
    public void put(int k, long v) { m.put(k, v); } public void append(int k, long v) { m.put(k, v); }
    public void delete(int k) { m.remove(k); } public void removeAt(int i) { m.remove(keyAt(i)); }
    public int size() { return m.size(); }
    public int keyAt(int i) { int j = 0; for (Integer k : m.keySet()) if (j++ == i) return k; throw new ArrayIndexOutOfBoundsException(i); }
    public long valueAt(int i) { return m.get(keyAt(i)); } public void setValueAt(int i, long v) { m.put(keyAt(i), v); }
    public int indexOfKey(int k) { int j = 0; for (Integer x : m.keySet()) { if (x == k) return j; j++; } return -1; }
    public int indexOfValue(long v) { int j = 0; for (Long x : m.values()) { if (x == v) return j; j++; } return -1; }
    public void clear() { m.clear(); }
    public SparseLongArray clone() { SparseLongArray c = new SparseLongArray(); c.m = new TreeMap<>(m); return c; }
}
