package android.util;
public abstract class Property<T, V> {
    private final String name; private final Class<V> type;
    public Property(Class<V> type, String name) { this.name = name; this.type = type; }
    public boolean isReadOnly() { return false; } public void set(T o, V v) { throw new UnsupportedOperationException("Property " + name + " is read-only"); }
    public abstract V get(T o); public String getName() { return name; } public Class<V> getType() { return type; }
    public static <T, V> Property<T, V> of(Class<T> h, Class<V> t, String n) { throw new UnsupportedOperationException(); }
}
