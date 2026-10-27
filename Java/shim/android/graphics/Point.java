package android.graphics;
public class Point {
    public int x, y;
    public Point() {} public Point(int x, int y) { this.x = x; this.y = y; } public Point(Point p) { x = p.x; y = p.y; }
    public void set(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) { return o instanceof Point && ((Point) o).x == x && ((Point) o).y == y; }
    @Override public int hashCode() { return x * 31 + y; }
}
