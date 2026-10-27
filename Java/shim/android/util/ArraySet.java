package android.util;
public final class ArraySet<E> extends java.util.LinkedHashSet<E> {
    public ArraySet() {} public ArraySet(int cap) { super(Math.max(cap, 1)); } public ArraySet(java.util.Collection<? extends E> c) { super(c); }
    public E valueAt(int i) { int j = 0; for (E e : this) if (j++ == i) return e; throw new ArrayIndexOutOfBoundsException(i); }
    public E removeAt(int i) { E e = valueAt(i); remove(e); return e; }
    public int indexOf(Object o) { int j = 0; for (E e : this) { if (java.util.Objects.equals(e, o)) return j; j++; } return -1; }
    public void ensureCapacity(int c) {} public void addAll(ArraySet<? extends E> s) { super.addAll(s); }
}
