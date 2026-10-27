package android.os;
import java.util.HashMap;
import java.util.Set;
public class BaseBundle {

    protected final HashMap<String, Object> m = new HashMap<>();
    public void jetPut(String k, Object v) { m.put(k, v); }
    public void putInt(String k, int v) { m.put(k, v); }
    public int getInt(String k) { return getInt(k, 0); }
    public int getInt(String k, int d) { Object v = m.get(k); return v instanceof Integer ? (Integer) v : d; }
    public void putString(String k, String v) { m.put(k, v); }
    public void putByteArray(String k, byte[] v) { m.put(k, v); }
    public byte[] getByteArray(String k) { Object v=m.get(k); return v instanceof byte[] ? (byte[])v : null; }
    public String getString(String k) { Object v = m.get(k); return v instanceof String ? (String) v : null; }
    public String getString(String k, String d) { String v = getString(k); return v != null ? v : d; }
    public void putBoolean(String k, boolean v) { m.put(k, v); }
    public boolean getBoolean(String k) { return getBoolean(k, false); }
    public boolean getBoolean(String k, boolean d) { Object v = m.get(k); return v instanceof Boolean ? (Boolean) v : d; }
    public void putLong(String k, long v) { m.put(k, v); }
    public long getLong(String k) { return getLong(k, 0L); }
    public long getLong(String k, long d) { Object v = m.get(k); return v instanceof Long ? (Long) v : d; }
    public void putFloat(String k, float v) { m.put(k, v); }
    public float getFloat(String k) { return getFloat(k, 0f); }
    public float getFloat(String k, float d) { Object v = m.get(k); return v instanceof Float ? (Float) v : d; }
    public boolean containsKey(String k) { return m.containsKey(k); }
    public void remove(String k) { m.remove(k); }
    public boolean isEmpty() { return m.isEmpty(); }
    public int size() { return m.size(); }
    public void clear() { m.clear(); }
    public Set<String> keySet() { return m.keySet(); }

    public Object get(String k) { return m.get(k); }
    public void putDouble(String k, double v) { m.put(k, v); }
    public double getDouble(String k) { return getDouble(k, 0d); }
    public double getDouble(String k, double d) { Object v = m.get(k); return v instanceof Double ? (Double) v : d; }
    public void putStringArray(String k, String[] v) { m.put(k, v); }
    public String[] getStringArray(String k) { Object v = m.get(k); return v instanceof String[] ? (String[]) v : null; }
    public void putIntArray(String k, int[] v) { m.put(k, v); }
    public int[] getIntArray(String k) { Object v = m.get(k); return v instanceof int[] ? (int[]) v : null; }
    public void putLongArray(String k, long[] v) { m.put(k, v); }
    public long[] getLongArray(String k) { Object v = m.get(k); return v instanceof long[] ? (long[]) v : null; }
    public void putBooleanArray(String k, boolean[] v) { m.put(k, v); }
    public boolean[] getBooleanArray(String k) { Object v = m.get(k); return v instanceof boolean[] ? (boolean[]) v : null; }
    public void putAll(PersistableBundle b) { if (b != null) m.putAll(b.m); }

}
