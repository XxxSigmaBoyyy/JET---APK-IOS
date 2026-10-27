package android.content;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/** Simple XML-less prefs stored as java.util.Properties. */
public final class SharedPreferencesImpl implements SharedPreferences {
    private final File file;
    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private final List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();
    public SharedPreferencesImpl(File f) {
        file = f;
        if (f.exists()) try (InputStream in = new FileInputStream(f)) {
            Properties p = new Properties(); p.load(in);
            for (String k : p.stringPropertyNames()) map.put(k, p.getProperty(k));
        } catch (IOException ignored) {}
    }
    public Map<String, ?> getAll() { return new HashMap<>(map); }
    public String getString(String k, String d) { Object v = map.get(k); return v != null ? String.valueOf(v) : d; }
    public int getInt(String k, int d) { try { return Integer.parseInt(getString(k, null)); } catch (Exception e) { return d; } }
    public long getLong(String k, long d) { try { return Long.parseLong(getString(k, null)); } catch (Exception e) { return d; } }
    public float getFloat(String k, float d) { try { return Float.parseFloat(getString(k, null)); } catch (Exception e) { return d; } }
    @SuppressWarnings("unchecked") public Set<String> getStringSet(String k, Set<String> d) { Object v = map.get(k); return v instanceof Set ? new HashSet<>((Set<String>) v) : d; }
    public boolean getBoolean(String k, boolean d) { String s = getString(k, null); return s == null ? d : Boolean.parseBoolean(s); }
    public boolean contains(String k) { return map.containsKey(k); }
    public Editor edit() { return new Ed(); }
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { listeners.add(l); }
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { listeners.remove(l); }
    private final class Ed implements Editor {
        private final Map<String, Object> pending = new HashMap<>();
        private boolean clear;
        public Editor putString(String k, String v) { pending.put(k, v); return this; }
        public Editor putStringSet(String k, Set<String> v) { if (v == null) return remove(k); pending.put(k, new HashSet<>(v)); return this; }
        public Editor putInt(String k, int v) { pending.put(k, Integer.toString(v)); return this; }
        public Editor putLong(String k, long v) { pending.put(k, Long.toString(v)); return this; }
        public Editor putFloat(String k, float v) { pending.put(k, Float.toString(v)); return this; }
        public Editor putBoolean(String k, boolean v) { pending.put(k, Boolean.toString(v)); return this; }
        public Editor remove(String k) { pending.put(k, null); return this; }
        public Editor clear() { clear = true; return this; }
        public void apply() { commit(); }
        public boolean commit() {
            if (clear) map.clear();
            for (Map.Entry<String, Object> e : pending.entrySet()) {
                if (e.getValue() == null) map.remove(e.getKey()); else map.put(e.getKey(), e.getValue());
            }
            try {
                file.getParentFile().mkdirs();
                Properties p = new Properties();
                for (Map.Entry<String, Object> e : map.entrySet()) p.setProperty(e.getKey(), String.valueOf(e.getValue()));
                try (OutputStream out = new FileOutputStream(file)) { p.store(out, "jet-prefs"); }
            } catch (IOException ex) { return false; }
            return true;
        }
    }
}
