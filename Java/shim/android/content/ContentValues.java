package android.content;
import java.util.*;
public final class ContentValues {
    private final LinkedHashMap<String, Object> v = new LinkedHashMap<>();
    public ContentValues() {}
    public ContentValues(int n) {}
    public ContentValues(ContentValues o) { v.putAll(o.v); }
    public void put(String k, String x) { v.put(k, x); }
    public void put(String k, Integer x) { v.put(k, x); }
    public void put(String k, Long x) { v.put(k, x); }
    public void put(String k, Short x) { v.put(k, x); }
    public void put(String k, Byte x) { v.put(k, x); }
    public void put(String k, Float x) { v.put(k, x); }
    public void put(String k, Double x) { v.put(k, x); }
    public void put(String k, Boolean x) { v.put(k, x == null ? null : (x ? 1 : 0)); }
    public void put(String k, byte[] x) { v.put(k, x); }
    public void putNull(String k) { v.put(k, null); }
    public void putAll(ContentValues o) { v.putAll(o.v); }
    public Object get(String k) { return v.get(k); }
    public String getAsString(String k) { Object o = v.get(k); return o == null ? null : o.toString(); }
    public Integer getAsInteger(String k) { Object o = v.get(k); return o instanceof Number ? ((Number) o).intValue() : o == null ? null : Integer.valueOf(o.toString()); }
    public Long getAsLong(String k) { Object o = v.get(k); return o instanceof Number ? ((Number) o).longValue() : o == null ? null : Long.valueOf(o.toString()); }
    public Boolean getAsBoolean(String k) { Object o = v.get(k); return o instanceof Number ? ((Number) o).intValue() != 0 : o == null ? null : Boolean.valueOf(o.toString()); }
    public boolean containsKey(String k) { return v.containsKey(k); }
    public void remove(String k) { v.remove(k); }
    public void clear() { v.clear(); }
    public int size() { return v.size(); }
    public boolean isEmpty() { return v.isEmpty(); }
    public Set<String> keySet() { return v.keySet(); }
    public Set<Map.Entry<String, Object>> valueSet() { return v.entrySet(); }
}
