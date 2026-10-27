package android.os;
import java.io.Serializable;
public class Bundle extends BaseBundle implements Cloneable, Parcelable {
    // AOSP: Bundle is Parcelable (Compose SaveableStateRegistry / SavedStateHandle only accept Parcelable/Serializable
    // values). Marshalled through the in-process Parcel (writeBundle/readBundle).
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) { dest.writeBundle(this); }
    public static final Parcelable.Creator<Bundle> CREATOR = new Parcelable.Creator<Bundle>() {
        public Bundle createFromParcel(Parcel s) { Bundle b = s.readBundle(); return b == null ? new Bundle() : b; } public Bundle[] newArray(int n) { return new Bundle[n]; } };
    /** AOSP: shallow copy (androidx ActivityResultRegistry.onSaveInstanceState clones its pending-results Bundle). */
    @Override public Object clone() { return new Bundle(this); }
    public Bundle deepCopy() { return new Bundle(this); }
    public void putSerializable(String k, Serializable v) { m.put(k, v); }
    public Serializable getSerializable(String k) { Object v = m.get(k); return v instanceof Serializable ? (Serializable) v : null; }
    public void putParcelable(String k, Parcelable v) { m.put(k, v); }
    public void putParcelable(String k, Object v) { m.put(k, v); }  // jet-internal callers with non-Parcelable values
    public Parcelable getParcelable(String k) { Object v = m.get(k); return v instanceof Parcelable ? (Parcelable) v : null; }
    public void putAll(Bundle b) { if (b != null) m.putAll(b.m); }
    @Override public String toString() { return "Bundle" + m; }
    public Bundle() {}
    public Bundle(Bundle b) { putAll(b); }
    public Bundle(ClassLoader cl) {}
    public Bundle(int cap) {}
    public static final Bundle EMPTY = new Bundle();
    public void putBundle(String k, Bundle v) { m.put(k, v); }
    public Bundle getBundle(String k) { Object v = m.get(k); return v instanceof Bundle ? (Bundle) v : null; }
    public void putCharSequence(String k, CharSequence v) { m.put(k, v); }
    public CharSequence getCharSequence(String k) { Object v = m.get(k); return v instanceof CharSequence ? (CharSequence) v : null; }
    public void putStringArrayList(String k, java.util.ArrayList<String> v) { m.put(k, v); }
    @SuppressWarnings("unchecked") public java.util.ArrayList<String> getStringArrayList(String k) { Object v = m.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<String>) v : null; }
    public void putIntegerArrayList(String k, java.util.ArrayList<Integer> v) { m.put(k, v); }
    public void putParcelableArrayList(String k, java.util.ArrayList<? extends Parcelable> v) { m.put(k, v); }
    public void putParcelableArray(String k, Parcelable[] v) { m.put(k, v); }
    // API 33 type-safe getters (and the pre-33 untyped list/array variants)
    public <T> T getParcelable(String k, Class<T> c) { Object v = m.get(k); return c.isInstance(v) ? c.cast(v) : null; }
    public <T extends Serializable> T getSerializable(String k, Class<T> c) { Object v = m.get(k); return c.isInstance(v) ? c.cast(v) : null; }
    @SuppressWarnings("unchecked") public <T extends Parcelable> java.util.ArrayList<T> getParcelableArrayList(String k) { Object v = m.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<T>) v : null; }
    @SuppressWarnings("unchecked") public <T> java.util.ArrayList<T> getParcelableArrayList(String k, Class<? extends T> c) { Object v = m.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<T>) v : null; }
    public Parcelable[] getParcelableArray(String k) { Object v = m.get(k); return v instanceof Parcelable[] ? (Parcelable[]) v : null; }
    @SuppressWarnings("unchecked") public <T> T[] getParcelableArray(String k, Class<T> c) { Object v = m.get(k); if (!(v instanceof Object[])) return null; Object[] a = (Object[]) v; T[] r = (T[]) java.lang.reflect.Array.newInstance(c, a.length); System.arraycopy(a, 0, r, 0, a.length); return r; }
    public void putSparseParcelableArray(String k, android.util.SparseArray<? extends Parcelable> v) { m.put(k, v); }
    @SuppressWarnings("unchecked") public <T extends Parcelable> android.util.SparseArray<T> getSparseParcelableArray(String k) { Object v = m.get(k); return v instanceof android.util.SparseArray ? (android.util.SparseArray<T>) v : null; }
    @SuppressWarnings("unchecked") public <T> android.util.SparseArray<T> getSparseParcelableArray(String k, Class<? extends T> c) { Object v = m.get(k); return v instanceof android.util.SparseArray ? (android.util.SparseArray<T>) v : null; }
    public java.util.ArrayList<Integer> getIntegerArrayList(String k) { Object v = m.get(k); @SuppressWarnings("unchecked") java.util.ArrayList<Integer> r = v instanceof java.util.ArrayList ? (java.util.ArrayList<Integer>) v : null; return r; }
    public void setClassLoader(ClassLoader cl) {}
    public ClassLoader getClassLoader() { return Bundle.class.getClassLoader(); }

}
