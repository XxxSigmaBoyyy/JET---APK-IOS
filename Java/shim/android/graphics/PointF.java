package android.graphics;
/** Host shim for android.graphics.PointF (API 1+; equals(float,float) since API 30). */
public class PointF {
    public float x, y;
    public PointF() {}
    public PointF(float x, float y) { this.x = x; this.y = y; }
    public PointF(Point p) { this.x = p.x; this.y = p.y; }
    public PointF(PointF p) { this.x = p.x; this.y = p.y; }
    public void set(float x, float y) { this.x = x; this.y = y; }
    public void set(PointF p) { this.x = p.x; this.y = p.y; }
    public final void negate() { x = -x; y = -y; }
    public final void offset(float dx, float dy) { x += dx; y += dy; }
    public final boolean equals(float x, float y) { return this.x == x && this.y == y; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PointF)) return false;
        PointF p = (PointF) o;
        return x == p.x && y == p.y;
    }
    @Override public int hashCode() { return Float.floatToIntBits(x) * 31 + Float.floatToIntBits(y); }
    @Override public String toString() { return "PointF(" + x + ", " + y + ")"; }
    public final float length() { return (float) Math.hypot(x, y); }
    public static float length(float x, float y) { return (float) Math.hypot(x, y); }
}
