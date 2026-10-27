package android.graphics;
public class RectF implements android.os.Parcelable {
    public float left, top, right, bottom;
    public RectF() {}
    public RectF(float l, float t, float r, float b) { left = l; top = t; right = r; bottom = b; }
    public RectF(RectF r) { if (r != null) set(r); }
    public RectF(Rect r) { if (r != null) set(r); }
    public final float width() { return right - left; }
    public final float height() { return bottom - top; }
    public final float centerX() { return (left + right) * 0.5f; }
    public final float centerY() { return (top + bottom) * 0.5f; }
    public final boolean isEmpty() { return left >= right || top >= bottom; }
    public void setEmpty() { left = top = right = bottom = 0; }
    public void set(float l, float t, float r, float b) { left = l; top = t; right = r; bottom = b; }
    public void set(RectF r) { set(r.left, r.top, r.right, r.bottom); }
    public void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }
    public void offset(float dx, float dy) { left += dx; right += dx; top += dy; bottom += dy; }
    public void offsetTo(float x, float y) { right += x - left; bottom += y - top; left = x; top = y; }
    public void inset(float dx, float dy) { left += dx; top += dy; right -= dx; bottom -= dy; }
    public boolean contains(float x, float y) { return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom; }
    public boolean contains(float l, float t, float r, float b) { return left < right && top < bottom && left <= l && top <= t && right >= r && bottom >= b; }
    public boolean contains(RectF r) { return contains(r.left, r.top, r.right, r.bottom); }
    public boolean intersect(float l, float t, float r, float b) { if (left < r && l < right && top < b && t < bottom) { left = Math.max(left, l); top = Math.max(top, t); right = Math.min(right, r); bottom = Math.min(bottom, b); return true; } return false; }
    public boolean intersect(RectF r) { return intersect(r.left, r.top, r.right, r.bottom); }
    public boolean intersects(float l, float t, float r, float b) { return left < r && l < right && top < b && t < bottom; }
    public static boolean intersects(RectF a, RectF b) { return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom; }
    public boolean setIntersect(RectF a, RectF b) { if (intersects(a, b)) { set(Math.max(a.left, b.left), Math.max(a.top, b.top), Math.min(a.right, b.right), Math.min(a.bottom, b.bottom)); return true; } return false; }
    public void union(float l, float t, float r, float b) { if (l < r && t < b) { if (left < right && top < bottom) { left = Math.min(left, l); top = Math.min(top, t); right = Math.max(right, r); bottom = Math.max(bottom, b); } else set(l, t, r, b); } }
    public void union(RectF r) { union(r.left, r.top, r.right, r.bottom); }
    public void union(float x, float y) { left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y); }
    public void sort() { if (left > right) { float t = left; left = right; right = t; } if (top > bottom) { float t = top; top = bottom; bottom = t; } }
    public void round(Rect d) { d.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom)); }
    public void roundOut(Rect d) { d.set((int) Math.floor(left), (int) Math.floor(top), (int) Math.ceil(right), (int) Math.ceil(bottom)); }
    @Override public boolean equals(Object o) { if (!(o instanceof RectF)) return false; RectF r = (RectF) o; return left == r.left && top == r.top && right == r.right && bottom == r.bottom; }
    @Override public int hashCode() { return java.util.Objects.hash(left, top, right, bottom); }
    @Override public String toString() { return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")"; }
    public String toShortString() { return "[" + left + "," + top + "][" + right + "," + bottom + "]"; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
}
