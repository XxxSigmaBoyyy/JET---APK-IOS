package android.widget;
/** Virtual-time scroller (uses SystemClock.uptimeMillis, which the harness drives). */
public class Scroller {
    private int sx, sy, fx, fy, cx, cy, dur = 250, mode; private long start; private boolean finished = true; private float vel;
    private final android.view.animation.Interpolator interp;
    public Scroller(android.content.Context c) { this(c, null); }
    public Scroller(android.content.Context c, android.view.animation.Interpolator i) { interp = i; }
    public Scroller(android.content.Context c, android.view.animation.Interpolator i, boolean flywheel) { this(c, i); }
    public Scroller(android.content.Context c, android.view.animation.Interpolator i, float bx, float by) { this(c, i); }
    public Scroller(android.content.Context c, android.view.animation.Interpolator i, float bx, float by, boolean fw) { this(c, i); }
    private static long now() { return android.os.SystemClock.uptimeMillis(); }
    public final boolean isFinished() { return finished; } public final void forceFinished(boolean f) { finished = f; }
    public final int getDuration() { return dur; } public final int getCurrX() { return cx; } public final int getCurrY() { return cy; }
    public final int getStartX() { return sx; } public final int getStartY() { return sy; } public final int getFinalX() { return fx; } public final int getFinalY() { return fy; }
    public float getCurrVelocity() { return finished ? 0 : vel; } public final void setFriction(float f) {} public void setFinalX(int x) { fx = x; } public void setFinalY(int y) { fy = y; }
    public void extendDuration(int e) { dur += e; } public int timePassed() { return (int) (now() - start); } public boolean isScrollingInDirection(float x, float y) { return !finished; }
    public void startScroll(int x, int y, int dx, int dy) { startScroll(x, y, dx, dy, 250); }
    public void startScroll(int x, int y, int dx, int dy, int d) { sx = cx = x; sy = cy = y; fx = x + dx; fy = y + dy; dur = Math.max(1, d); start = now(); finished = false; mode = 0; }
    public void fling(int x, int y, int vx, int vy, int minX, int maxX, int minY, int maxY) {
        vel = (float) Math.hypot(vx, vy); int d = 600; int tx = x + vx * d / 2000, ty = y + vy * d / 2000;
        startScroll(x, y, Math.max(minX, Math.min(maxX, tx)) - x, Math.max(minY, Math.min(maxY, ty)) - y, d); mode = 1; }
    public void fling(int x, int y, int vx, int vy, int minX, int maxX, int minY, int maxY, int ox, int oy) { fling(x, y, vx, vy, minX, maxX, minY, maxY); }
    public boolean springBack(int x, int y, int minX, int maxX, int minY, int maxY) { int tx = Math.max(minX, Math.min(maxX, x)), ty = Math.max(minY, Math.min(maxY, y)); if (tx == x && ty == y) return false; startScroll(x, y, tx - x, ty - y); return true; }
    public void notifyHorizontalEdgeReached(int a, int b, int o) {} public void notifyVerticalEdgeReached(int a, int b, int o) {} public boolean isOverScrolled() { return false; }
    public void abortAnimation() { cx = fx; cy = fy; finished = true; }
    public boolean computeScrollOffset() {
        if (finished) return false; long t = now() - start;
        if (t >= dur) { cx = fx; cy = fy; finished = true; return true; }
        float f = t / (float) dur; f = interp != null ? interp.getInterpolation(f) : (mode == 1 ? 1 - (1 - f) * (1 - f) : (float) (Math.cos((f + 1) * Math.PI) / 2 + 0.5));
        cx = sx + Math.round(f * (fx - sx)); cy = sy + Math.round(f * (fy - sy)); return true; }
}
