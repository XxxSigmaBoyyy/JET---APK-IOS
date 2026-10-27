package android.graphics;
public final class Rect {
    public int left, top, right, bottom;
    public Rect() {}
    public Rect(int l, int t, int r, int b) { left=l; top=t; right=r; bottom=b; }
    public Rect(Rect r) { left=r.left; top=r.top; right=r.right; bottom=r.bottom; }
    public int width() { return right - left; }
    public int height() { return bottom - top; }
    public void set(int l, int t, int r, int b) { left=l; top=t; right=r; bottom=b; }
    public boolean isEmpty() { return left >= right || top >= bottom; }
    public void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }
    public int centerX() { return (left + right) >> 1; }
    public int centerY() { return (top + bottom) >> 1; }
    public boolean contains(int x, int y) { return x >= left && x < right && y >= top && y < bottom; }
    public void offset(int dx, int dy) { left += dx; right += dx; top += dy; bottom += dy; }
    public void offsetTo(int x, int y) { offset(x - left, y - top); }
    public void inset(int dx, int dy) { left += dx; right -= dx; top += dy; bottom -= dy; }
    public boolean intersect(Rect r) { return intersect(r.left, r.top, r.right, r.bottom); }
    public boolean intersect(int l, int t, int r, int b) { if (left < r && l < right && top < b && t < bottom) { left = Math.max(left, l); top = Math.max(top, t); right = Math.min(right, r); bottom = Math.min(bottom, b); return true; } return false; }
    public void union(int l, int t, int r2, int b) { union(new Rect(l, t, r2, b)); }
    public void union(int x, int y) { if (x < left) left = x; else if (x > right) right = x; if (y < top) top = y; else if (y > bottom) bottom = y; }
    public void union(Rect r) { if (r.isEmpty()) return; if (isEmpty()) { set(r); return; } left = Math.min(left, r.left); top = Math.min(top, r.top); right = Math.max(right, r.right); bottom = Math.max(bottom, r.bottom); }
    @Override public boolean equals(Object o) { if (!(o instanceof Rect)) return false; Rect r = (Rect) o; return left == r.left && top == r.top && right == r.right && bottom == r.bottom; }
    @Override public int hashCode() { return ((left * 31 + top) * 31 + right) * 31 + bottom; }
    public void setEmpty() { left = top = right = bottom = 0; }
    @Override public String toString() { return "Rect("+left+", "+top+" - "+right+", "+bottom+")"; }
}
