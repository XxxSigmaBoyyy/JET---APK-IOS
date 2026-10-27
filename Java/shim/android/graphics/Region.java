package android.graphics;
public class Region {
    public enum Op { DIFFERENCE, INTERSECT, UNION, XOR, REVERSE_DIFFERENCE, REPLACE }
    private final Rect b = new Rect();
    public Region() {} public Region(Rect r) { b.set(r); } public Region(int l, int t, int r, int bb) { b.set(l, t, r, bb); }
    public boolean set(Rect r) { b.set(r); return !b.isEmpty(); } public boolean set(int l, int t, int r, int bb) { b.set(l, t, r, bb); return !b.isEmpty(); }
    public void setEmpty() { b.set(0, 0, 0, 0); } public boolean isEmpty() { return b.isEmpty(); } public boolean isRect() { return true; } public boolean isComplex() { return false; }
    public Rect getBounds() { return new Rect(b); } public boolean getBounds(Rect r) { r.set(b); return !b.isEmpty(); }
    public boolean contains(int x, int y) { return b.contains(x, y); }
    public boolean op(Rect r, Op op) { if (op == Op.UNION) b.union(r); else if (op == Op.INTERSECT) { if (!b.intersect(r)) b.set(0, 0, 0, 0); } else if (op == Op.REPLACE) b.set(r); return !b.isEmpty(); }
    public boolean op(int l, int t, int r, int bb, Op op) { return op(new Rect(l, t, r, bb), op); }
    public boolean union(Rect r) { return op(r, Op.UNION); }
    public void translate(int dx, int dy) { b.offset(dx, dy); }
}
