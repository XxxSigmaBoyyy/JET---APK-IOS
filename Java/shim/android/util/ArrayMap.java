package android.util;
public final class ArrayMap<K, V> extends java.util.LinkedHashMap<K, V> {
    public ArrayMap() {} public ArrayMap(int cap) { super(Math.max(cap, 1)); } public ArrayMap(ArrayMap<K, V> o) { super(o); }
    private java.util.Map.Entry<K, V> at(int i) { int j = 0; for (java.util.Map.Entry<K, V> e : entrySet()) if (j++ == i) return e; throw new ArrayIndexOutOfBoundsException(i); }
    public K keyAt(int i) { return at(i).getKey(); } public V valueAt(int i) { return at(i).getValue(); }
    public V setValueAt(int i, V v) { return at(i).setValue(v); } public V removeAt(int i) { return remove(keyAt(i)); }
    public int indexOfKey(Object k) { int j = 0; for (K x : keySet()) { if (java.util.Objects.equals(x, k)) return j; j++; } return -1; }
    public int indexOfValue(Object v) { int j = 0; for (V x : values()) { if (java.util.Objects.equals(x, v)) return j; j++; } return -1; }
    public void ensureCapacity(int c) {} public void putAll(ArrayMap<? extends K, ? extends V> m) { super.putAll(m); }
    public boolean containsAll(java.util.Collection<?> c) { return keySet().containsAll(c); }
}
