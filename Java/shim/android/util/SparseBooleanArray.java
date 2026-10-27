package android.util;
import java.util.TreeMap;
public class SparseBooleanArray implements Cloneable {
    TreeMap<Integer, Boolean> m = new TreeMap<>();
    public SparseBooleanArray() {} public SparseBooleanArray(int cap) {}
    public boolean get(int k) { return get(k, false); }
    public boolean get(int k, boolean d) { Boolean v = m.get(k); return v != null ? v : d; }
    public void put(int k, boolean v) { m.put(k, v); } public void append(int k, boolean v) { m.put(k, v); }
    public void delete(int k) { m.remove(k); } public void removeAt(int i) { m.remove(keyAt(i)); }
    public int size() { return m.size(); }
    public int keyAt(int i) { int j = 0; for (Integer k : m.keySet()) if (j++ == i) return k; throw new ArrayIndexOutOfBoundsException(i); }
    public boolean valueAt(int i) { return m.get(keyAt(i)); } public void setValueAt(int i, boolean v) { m.put(keyAt(i), v); }
    public int indexOfKey(int k) { int j = 0; for (Integer x : m.keySet()) { if (x == k) return j; j++; } return -1; }
    public int indexOfValue(boolean v) { int j = 0; for (Boolean x : m.values()) { if (x == v) return j; j++; } return -1; }
    public void clear() { m.clear(); }
    public SparseBooleanArray clone() { SparseBooleanArray c = new SparseBooleanArray(); c.m = new TreeMap<>(m); return c; }
}
