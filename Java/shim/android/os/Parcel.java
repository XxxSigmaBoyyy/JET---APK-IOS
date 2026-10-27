package android.os;

import java.util.*;

/** In-process Parcel with AOSP semantics: a little-endian, 4-byte aligned data buffer with position/size/capacity
 *  plus an object table for live binders and file descriptors (which, as on Android, cannot be marshalled).
 *  Parcelables are written as class name + writeToParcel and read back through their CREATOR. */
public final class Parcel {
    private byte[] d = new byte[64]; private int size, pos;
    private final ArrayList<Object> objs = new ArrayList<>();
    private static final Object NULL_OBJ = new Object();

    public static Parcel obtain() { return new Parcel(); }
    public static Parcel obtain(IBinder b) { return new Parcel(); }
    public void recycle() { size = pos = 0; objs.clear(); }
    public int dataSize() { return size; }
    public int dataAvail() { return Math.max(0, size - pos); }
    public int dataPosition() { return pos; }
    public int dataCapacity() { return d.length; }
    public void setDataSize(int s) { ensure(s); size = s; if (pos > s) pos = s; }
    public void setDataPosition(int p) { if (p < 0) throw new IllegalArgumentException(); pos = p; }
    public void setDataCapacity(int c) { if (c > d.length) d = Arrays.copyOf(d, c); }
    public boolean hasFileDescriptors() { for (Object o : objs) if (o instanceof java.io.FileDescriptor) return true; return false; }
    public byte[] marshall() {
        for (Object o : objs) if (o instanceof IBinder || o instanceof java.io.FileDescriptor) throw new RuntimeException("Tried to marshall a Parcel that contained Binder objects.");
        return Arrays.copyOf(d, size);
    }
    public void unmarshall(byte[] data, int off, int len) { ensure(len); System.arraycopy(data, off, d, 0, len); size = len; pos = 0; }
    public void appendFrom(Parcel p, int off, int len) { ensure(pos + len); System.arraycopy(p.d, off, d, pos, len); pos += len; if (pos > size) size = pos; }

    private void ensure(int n) { if (n > d.length) d = Arrays.copyOf(d, Math.max(n, d.length * 2)); }
    private void put(int nbytes, long v) { int al = (nbytes + 3) & ~3; ensure(pos + al); for (int i = 0; i < al; i++) d[pos + i] = i < nbytes ? (byte) (v >>> (8 * i)) : 0; pos += al; if (pos > size) size = pos; }
    private long get(int nbytes) { int al = (nbytes + 3) & ~3; if (pos + al > size) { pos = size; return 0; } long v = 0; for (int i = 0; i < nbytes; i++) v |= (d[pos + i] & 0xFFL) << (8 * i); pos += al; return v; }

    public void writeInt(int v) { put(4, v & 0xFFFFFFFFL); }
    public int readInt() { return (int) get(4); }
    public void writeLong(long v) { put(8, v); }
    public long readLong() { return get(8); }
    public void writeFloat(float v) { writeInt(Float.floatToRawIntBits(v)); }
    public float readFloat() { return Float.intBitsToFloat(readInt()); }
    public void writeDouble(double v) { writeLong(Double.doubleToRawLongBits(v)); }
    public double readDouble() { return Double.longBitsToDouble(readLong()); }
    public void writeByte(byte v) { writeInt(v); }
    public byte readByte() { return (byte) readInt(); }
    public void writeBoolean(boolean v) { writeInt(v ? 1 : 0); }
    public boolean readBoolean() { return readInt() != 0; }
    public void writeString(String s) { if (s == null) { writeInt(-1); return; } writeInt(s.length()); int n = s.length() * 2 + 2; int al = (n + 3) & ~3; ensure(pos + al);
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); d[pos + 2 * i] = (byte) c; d[pos + 2 * i + 1] = (byte) (c >>> 8); }
        for (int i = s.length() * 2; i < al; i++) d[pos + i] = 0; pos += al; if (pos > size) size = pos; }
    public String readString() { int n = readInt(); if (n < 0) return null; int al = (n * 2 + 2 + 3) & ~3; if (pos + al > size) { pos = size; return null; }
        char[] c = new char[n]; for (int i = 0; i < n; i++) c[i] = (char) ((d[pos + 2 * i] & 0xFF) | ((d[pos + 2 * i + 1] & 0xFF) << 8)); pos += al; return new String(c); }
    public void writeString8(String s) { writeString(s); } public String readString8() { return readString(); }
    public void writeString16(String s) { writeString(s); } public String readString16() { return readString(); }
    public void writeCharSequence(CharSequence cs) { writeString(cs == null ? null : cs.toString()); }
    public CharSequence readCharSequence() { return readString(); }

    public void writeByteArray(byte[] b) { writeByteArray(b, 0, b == null ? 0 : b.length); }
    public void writeByteArray(byte[] b, int off, int len) { if (b == null) { writeInt(-1); return; } if (off < 0 || len < 0 || off + len > b.length) throw new ArrayIndexOutOfBoundsException();
        writeInt(len); int al = (len + 3) & ~3; ensure(pos + al); System.arraycopy(b, off, d, pos, len); for (int i = len; i < al; i++) d[pos + i] = 0; pos += al; if (pos > size) size = pos; }
    public void writeBlob(byte[] b) { writeByteArray(b); } public void writeBlob(byte[] b, int off, int len) { writeByteArray(b, off, len); }
    public byte[] createByteArray() { int n = readInt(); if (n < 0) return null; int al = (n + 3) & ~3; if (pos + al > size) { pos = size; return null; } byte[] b = Arrays.copyOfRange(d, pos, pos + n); pos += al; return b; }
    public byte[] readBlob() { return createByteArray(); }
    public void readByteArray(byte[] out) { byte[] b = createByteArray(); if (b == null || b.length != out.length) throw new RuntimeException("bad array lengths"); System.arraycopy(b, 0, out, 0, b.length); }
    public byte[] marshallBytes() { return marshall(); }

    public void writeIntArray(int[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (int v : a) writeInt(v); }
    public int[] createIntArray() { int n = readInt(); if (n < 0) return null; int[] a = new int[n]; for (int i = 0; i < n; i++) a[i] = readInt(); return a; }
    public void readIntArray(int[] o) { int[] a = createIntArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeLongArray(long[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (long v : a) writeLong(v); }
    public long[] createLongArray() { int n = readInt(); if (n < 0) return null; long[] a = new long[n]; for (int i = 0; i < n; i++) a[i] = readLong(); return a; }
    public void readLongArray(long[] o) { long[] a = createLongArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeFloatArray(float[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (float v : a) writeFloat(v); }
    public float[] createFloatArray() { int n = readInt(); if (n < 0) return null; float[] a = new float[n]; for (int i = 0; i < n; i++) a[i] = readFloat(); return a; }
    public void readFloatArray(float[] o) { float[] a = createFloatArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeDoubleArray(double[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (double v : a) writeDouble(v); }
    public double[] createDoubleArray() { int n = readInt(); if (n < 0) return null; double[] a = new double[n]; for (int i = 0; i < n; i++) a[i] = readDouble(); return a; }
    public void readDoubleArray(double[] o) { double[] a = createDoubleArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeBooleanArray(boolean[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (boolean v : a) writeInt(v ? 1 : 0); }
    public boolean[] createBooleanArray() { int n = readInt(); if (n < 0) return null; boolean[] a = new boolean[n]; for (int i = 0; i < n; i++) a[i] = readInt() != 0; return a; }
    public void readBooleanArray(boolean[] o) { boolean[] a = createBooleanArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeCharArray(char[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (char v : a) writeInt(v); }
    public char[] createCharArray() { int n = readInt(); if (n < 0) return null; char[] a = new char[n]; for (int i = 0; i < n; i++) a[i] = (char) readInt(); return a; }
    public void readCharArray(char[] o) { char[] a = createCharArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeStringArray(String[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (String v : a) writeString(v); }
    public String[] createStringArray() { int n = readInt(); if (n < 0) return null; String[] a = new String[n]; for (int i = 0; i < n; i++) a[i] = readString(); return a; }
    public void readStringArray(String[] o) { String[] a = createStringArray(); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public void writeStringList(List<String> l) { if (l == null) { writeInt(-1); return; } writeInt(l.size()); for (String s : l) writeString(s); }
    public ArrayList<String> createStringArrayList() { int n = readInt(); if (n < 0) return null; ArrayList<String> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(readString()); return l; }
    public void readStringList(List<String> l) { l.clear(); ArrayList<String> r = createStringArrayList(); if (r != null) l.addAll(r); }

    // ---- objects (binders, fds): kept live in the object table, the data stream holds the index
    private void writeObj(Object o) { writeInt(o == null ? -1 : objs.size()); if (o != null) objs.add(o); }
    private Object readObj() { int i = readInt(); return i < 0 || i >= objs.size() ? null : objs.get(i); }
    public void writeStrongBinder(IBinder b) { writeObj(b); }
    public void writeStrongInterface(IInterface i) { writeStrongBinder(i == null ? null : i.asBinder()); }
    public IBinder readStrongBinder() { Object o = readObj(); return o instanceof IBinder ? (IBinder) o : null; }
    public void writeFileDescriptor(java.io.FileDescriptor fd) { writeObj(fd); }
    public void writeRawFileDescriptor(java.io.FileDescriptor fd) { writeObj(fd); }
    public java.io.FileDescriptor readRawFileDescriptor() { Object o = readObj(); return o instanceof java.io.FileDescriptor ? (java.io.FileDescriptor) o : null; }
    public void writeInterfaceToken(String s) { writeInt(0); writeString(s); }
    public void enforceInterface(String s) { readInt(); String t = readString(); if (!Objects.equals(s, t)) throw new SecurityException("Binder invocation to an incorrect interface"); }
    public void writeNoException() { writeInt(0); }
    public void readException() { int code = readInt(); if (code != 0) { String msg = readString(); readException(code, msg); } }
    public void readException(int code, String msg) { switch (code) { case -1: throw new SecurityException(msg); case -3: throw new IllegalArgumentException(msg); case -4: throw new NullPointerException(msg); case -5: throw new IllegalStateException(msg); case -9: throw new UnsupportedOperationException(msg); default: throw new RuntimeException("Unknown exception code: " + code + " msg " + msg); } }
    public void writeException(Exception e) { int code = e instanceof SecurityException ? -1 : e instanceof IllegalArgumentException ? -3 : e instanceof NullPointerException ? -4 : e instanceof IllegalStateException ? -5 : e instanceof UnsupportedOperationException ? -9 : 0;
        if (code == 0) { if (e instanceof RuntimeException) throw (RuntimeException) e; throw new RuntimeException(e); } writeInt(code); writeString(e.getMessage()); }

    // ---- Parcelables
    public void writeParcelable(Parcelable p, int flags) { if (p == null) { writeString(null); return; } writeString(p.getClass().getName()); p.writeToParcel(this, flags); }
    public final void writeParcelableCreator(Parcelable p) { writeString(p.getClass().getName()); }
    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T readParcelable(ClassLoader loader) { String name = readString(); if (name == null) return null; Parcelable.Creator<?> c = creator(name, loader);
        Object o = c instanceof Parcelable.ClassLoaderCreator ? ((Parcelable.ClassLoaderCreator<?>) c).createFromParcel(this, loader) : c.createFromParcel(this); return (T) o; }
    public <T> T readParcelable(ClassLoader loader, Class<T> clazz) { Object o = readParcelable(loader); return clazz.cast(o); }
    public Parcelable.Creator<?> readParcelableCreator(ClassLoader loader) { String n = readString(); return n == null ? null : creator(n, loader); }
    static final Map<String, Parcelable.Creator<?>> CREATORS = new java.util.concurrent.ConcurrentHashMap<>();
    static Parcelable.Creator<?> creator(String name, ClassLoader loader) {
        Parcelable.Creator<?> c = CREATORS.get(name); if (c != null) return c;
        try {
            ClassLoader l = loader != null ? loader : Thread.currentThread().getContextClassLoader(); if (l == null) l = Parcel.class.getClassLoader();
            Class<?> k = Class.forName(name, true, l); java.lang.reflect.Field f = k.getField("CREATOR"); f.setAccessible(true);
            c = (Parcelable.Creator<?>) f.get(null); if (c == null) throw new BadParcelableException("Parcelable protocol requires a non-null static Parcelable.Creator object called CREATOR on class " + name);
            CREATORS.put(name, c); return c;
        } catch (ReflectiveOperationException | ClassCastException e) { throw new BadParcelableException("ClassNotFoundException when unmarshalling: " + name); }
    }
    public <T extends Parcelable> void writeTypedObject(T v, int flags) { if (v != null) { writeInt(1); v.writeToParcel(this, flags); } else writeInt(0); }
    public <T> T readTypedObject(Parcelable.Creator<T> c) { return readInt() != 0 ? c.createFromParcel(this) : null; }
    public <T extends Parcelable> void writeTypedList(List<T> l) { writeTypedList(l, 0); }
    public <T extends Parcelable> void writeTypedList(List<T> l, int flags) { if (l == null) { writeInt(-1); return; } writeInt(l.size()); for (T t : l) writeTypedObject(t, flags); }
    public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c) { int n = readInt(); if (n < 0) return null; ArrayList<T> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(readTypedObject(c)); return l; }
    public <T> void readTypedList(List<T> l, Parcelable.Creator<T> c) { l.clear(); ArrayList<T> r = createTypedArrayList(c); if (r != null) l.addAll(r); }
    public <T extends Parcelable> void writeTypedArray(T[] a, int flags) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (T t : a) writeTypedObject(t, flags); }
    public <T> T[] createTypedArray(Parcelable.Creator<T> c) { int n = readInt(); if (n < 0) return null; T[] a = c.newArray(n); for (int i = 0; i < n; i++) a[i] = readTypedObject(c); return a; }
    public <T> void readTypedArray(T[] o, Parcelable.Creator<T> c) { T[] a = createTypedArray(c); System.arraycopy(a, 0, o, 0, Math.min(a.length, o.length)); }
    public <T extends Parcelable> void writeParcelableArray(T[] a, int flags) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (T t : a) writeParcelable(t, flags); }
    public Parcelable[] readParcelableArray(ClassLoader l) { int n = readInt(); if (n < 0) return null; Parcelable[] a = new Parcelable[n]; for (int i = 0; i < n; i++) a[i] = readParcelable(l); return a; }
    @SuppressWarnings("unchecked") public <T> T[] readParcelableArray(ClassLoader l, Class<T> k) { Parcelable[] a = readParcelableArray(l); if (a == null) return null; T[] o = (T[]) java.lang.reflect.Array.newInstance(k, a.length); for (int i = 0; i < a.length; i++) o[i] = k.cast(a[i]); return o; }
    public <T extends Parcelable> void writeParcelableList(List<T> l, int flags) { if (l == null) { writeInt(-1); return; } writeInt(l.size()); for (T t : l) writeParcelable(t, flags); }
    @SuppressWarnings("unchecked") public <T extends Parcelable> List<T> readParcelableList(List<T> l, ClassLoader cl) { int n = readInt(); l.clear(); for (int i = 0; i < n; i++) l.add((T) readParcelable(cl)); return l; }

    // ---- generic values (AOSP VAL_* tags)
    public void writeValue(Object v) {
        if (v == null) writeInt(-1);
        else if (v instanceof String) { writeInt(0); writeString((String) v); }
        else if (v instanceof Integer) { writeInt(1); writeInt((Integer) v); }
        else if (v instanceof Map) { writeInt(2); writeMap((Map<?, ?>) v); }
        else if (v instanceof Bundle) { writeInt(3); writeBundle((Bundle) v); }
        else if (v instanceof PersistableBundle) { writeInt(25); writePersistableBundle((PersistableBundle) v); }
        else if (v instanceof Parcelable) { writeInt(4); writeParcelable((Parcelable) v, 0); }
        else if (v instanceof Short) { writeInt(5); writeInt((Short) v); }
        else if (v instanceof Long) { writeInt(6); writeLong((Long) v); }
        else if (v instanceof Float) { writeInt(7); writeFloat((Float) v); }
        else if (v instanceof Double) { writeInt(8); writeDouble((Double) v); }
        else if (v instanceof Boolean) { writeInt(9); writeInt((Boolean) v ? 1 : 0); }
        else if (v instanceof CharSequence) { writeInt(10); writeCharSequence((CharSequence) v); }
        else if (v instanceof List) { writeInt(11); writeList((List<?>) v); }
        else if (v instanceof android.util.SparseArray) { writeInt(12); writeSparseArray((android.util.SparseArray<?>) v); }
        else if (v instanceof boolean[]) { writeInt(23); writeBooleanArray((boolean[]) v); }
        else if (v instanceof byte[]) { writeInt(13); writeByteArray((byte[]) v); }
        else if (v instanceof String[]) { writeInt(14); writeStringArray((String[]) v); }
        else if (v instanceof IBinder) { writeInt(15); writeStrongBinder((IBinder) v); }
        else if (v instanceof Parcelable[]) { writeInt(16); writeParcelableArray((Parcelable[]) v, 0); }
        else if (v instanceof int[]) { writeInt(18); writeIntArray((int[]) v); }
        else if (v instanceof long[]) { writeInt(19); writeLongArray((long[]) v); }
        else if (v instanceof Byte) { writeInt(20); writeInt((Byte) v); }
        else if (v instanceof Character) { writeInt(29); writeInt((Character) v); }
        else if (v instanceof float[]) { writeInt(31); writeFloatArray((float[]) v); }
        else if (v instanceof double[]) { writeInt(28); writeDoubleArray((double[]) v); }
        else if (v instanceof char[]) { writeInt(30); writeCharArray((char[]) v); }
        else if (v instanceof Object[]) { Object[] a = (Object[]) v; writeInt(17); writeInt(a.length); for (Object o : a) writeValue(o); }
        else if (v instanceof java.io.Serializable) { writeInt(21); writeSerializable((java.io.Serializable) v); }
        else throw new RuntimeException("Parcel: unable to marshal value " + v);
    }
    public Object readValue(ClassLoader l) {
        int t = readInt();
        switch (t) {
            case -1: return null; case 0: return readString(); case 1: return readInt(); case 2: return readHashMap(l); case 3: return readBundle(l);
            case 25: return readPersistableBundle(l); case 4: return readParcelable(l); case 5: return (short) readInt(); case 6: return readLong(); case 7: return readFloat();
            case 8: return readDouble(); case 9: return readInt() != 0; case 10: return readCharSequence(); case 11: return readArrayList(l); case 12: return readSparseArray(l);
            case 23: return createBooleanArray(); case 13: return createByteArray(); case 14: return createStringArray(); case 15: return readStrongBinder(); case 16: return readParcelableArray(l);
            case 18: return createIntArray(); case 19: return createLongArray(); case 20: return (byte) readInt(); case 29: return (char) readInt(); case 31: return createFloatArray();
            case 28: return createDoubleArray(); case 30: return createCharArray(); case 17: { int n = readInt(); Object[] a = new Object[n]; for (int i = 0; i < n; i++) a[i] = readValue(l); return a; }
            case 21: return readSerializable(l);
            default: throw new BadParcelableException("Parcel: unmarshalling unknown type code " + t + " at offset " + (pos - 4));
        }
    }
    @SuppressWarnings("unchecked") public <T> T readValue(ClassLoader l, Class<T> clazz, Class<?>... itemTypes) { return (T) readValue(l); }
    public void writeList(List<?> l) { if (l == null) { writeInt(-1); return; } writeInt(l.size()); for (Object o : l) writeValue(o); }
    public ArrayList readArrayList(ClassLoader cl) { int n = readInt(); if (n < 0) return null; ArrayList<Object> l = new ArrayList<>(n); for (int i = 0; i < n; i++) l.add(readValue(cl)); return l; }
    @SuppressWarnings("unchecked") public void readList(List l, ClassLoader cl) { int n = readInt(); for (int i = 0; i < n; i++) l.add(readValue(cl)); }
    public <T> void readList(List<? super T> l, ClassLoader cl, Class<T> k) { readList((List) l, cl); }
    public void writeArray(Object[] a) { if (a == null) { writeInt(-1); return; } writeInt(a.length); for (Object o : a) writeValue(o); }
    public Object[] readArray(ClassLoader cl) { int n = readInt(); if (n < 0) return null; Object[] a = new Object[n]; for (int i = 0; i < n; i++) a[i] = readValue(cl); return a; }
    public void writeMap(Map<?, ?> m) { if (m == null) { writeInt(-1); return; } writeInt(m.size()); for (Map.Entry<?, ?> e : m.entrySet()) { writeValue(e.getKey()); writeValue(e.getValue()); } }
    @SuppressWarnings("unchecked") public void readMap(Map m, ClassLoader cl) { int n = readInt(); for (int i = 0; i < n; i++) { Object k = readValue(cl); m.put(k, readValue(cl)); } }
    public <K, V> void readMap(Map<? super K, ? super V> m, ClassLoader cl, Class<K> kc, Class<V> vc) { readMap((Map) m, cl); }
    public HashMap readHashMap(ClassLoader cl) { int n = readInt(); if (n < 0) return null; HashMap<Object, Object> m = new HashMap<>(); for (int i = 0; i < n; i++) { Object k = readValue(cl); m.put(k, readValue(cl)); } return m; }
    public void writeSparseArray(android.util.SparseArray<?> a) { if (a == null) { writeInt(-1); return; } writeInt(a.size()); for (int i = 0; i < a.size(); i++) { writeInt(a.keyAt(i)); writeValue(a.valueAt(i)); } }
    public <T> android.util.SparseArray<T> readSparseArray(ClassLoader cl) { int n = readInt(); if (n < 0) return null; android.util.SparseArray<Object> a = new android.util.SparseArray<>(); for (int i = 0; i < n; i++) { int k = readInt(); a.put(k, readValue(cl)); } @SuppressWarnings("unchecked") android.util.SparseArray<T> r = (android.util.SparseArray<T>) a; return r; }
    public void writeSparseBooleanArray(android.util.SparseBooleanArray a) { if (a == null) { writeInt(-1); return; } writeInt(a.size()); for (int i = 0; i < a.size(); i++) { writeInt(a.keyAt(i)); writeInt(a.valueAt(i) ? 1 : 0); } }
    public android.util.SparseBooleanArray readSparseBooleanArray() { int n = readInt(); if (n < 0) return null; android.util.SparseBooleanArray a = new android.util.SparseBooleanArray(); for (int i = 0; i < n; i++) { int k = readInt(); a.put(k, readInt() != 0); } return a; }
    public void writeBundle(Bundle b) { if (b == null) { writeInt(-1); return; } writeInt(b.size()); for (String k : b.keySet()) { writeString(k); writeValue(b.get(k)); } }
    public Bundle readBundle() { return readBundle(null); }
    public Bundle readBundle(ClassLoader cl) { int n = readInt(); if (n < 0) return null; Bundle b = new Bundle(); for (int i = 0; i < n; i++) { String k = readString(); b.jetPut(k, readValue(cl)); } return b; }
    public void writePersistableBundle(PersistableBundle b) { if (b == null) { writeInt(-1); return; } writeInt(b.size()); for (String k : b.keySet()) { writeString(k); writeValue(b.get(k)); } }
    public PersistableBundle readPersistableBundle() { return readPersistableBundle(null); }
    public PersistableBundle readPersistableBundle(ClassLoader cl) { int n = readInt(); if (n < 0) return null; PersistableBundle b = new PersistableBundle(); for (int i = 0; i < n; i++) { String k = readString(); b.jetPut(k, readValue(cl)); } return b; }
    public void writeSerializable(java.io.Serializable s) { if (s == null) { writeString(null); return; } writeString(s.getClass().getName());
        try { java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); java.io.ObjectOutputStream oo = new java.io.ObjectOutputStream(bo); oo.writeObject(s); oo.close(); writeByteArray(bo.toByteArray()); }
        catch (java.io.IOException e) { throw new BadParcelableException("Parcelable encountered IOException writing serializable object (name = " + s.getClass().getName() + ")"); } }
    public java.io.Serializable readSerializable() { return readSerializable(null); }
    private java.io.Serializable readSerializable(final ClassLoader cl) { String name = readString(); if (name == null) return null; byte[] b = createByteArray();
        try (java.io.ObjectInputStream oi = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(b)) {
            @Override protected Class<?> resolveClass(java.io.ObjectStreamClass c) throws java.io.IOException, ClassNotFoundException {
                ClassLoader l = cl != null ? cl : Thread.currentThread().getContextClassLoader(); if (l != null) try { return Class.forName(c.getName(), false, l); } catch (ClassNotFoundException e) { } return super.resolveClass(c); } }) {
            return (java.io.Serializable) oi.readObject();
        } catch (java.io.IOException | ClassNotFoundException e) { throw new BadParcelableException("Parcelable encountered " + e.getClass().getSimpleName() + " reading a Serializable object (name = " + name + ")"); } }
    public <T> T readSerializable(ClassLoader cl, Class<T> k) { return k.cast(readSerializable(cl)); }
}
