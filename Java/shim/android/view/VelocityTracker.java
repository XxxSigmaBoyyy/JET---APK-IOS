package android.view;
/** Least-squares-free velocity: (last - first) over the window of retained samples (<=100 ms), like the impulse estimator in spirit. */
public final class VelocityTracker {
    private final float[] xs = new float[20], ys = new float[20]; private final long[] ts = new long[20]; private int n, head; private float vx, vy;
    private VelocityTracker() {}
    public static VelocityTracker obtain() { return new VelocityTracker(); }
    public void recycle() { clear(); }
    public void clear() { n = 0; head = 0; vx = vy = 0; }
    public void addMovement(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) clear();
        xs[head] = e.getRawX(); ys[head] = e.getRawY(); ts[head] = e.getEventTime(); head = (head + 1) % xs.length; if (n < xs.length) n++;
    }
    public void computeCurrentVelocity(int units) { computeCurrentVelocity(units, Float.MAX_VALUE); }
    public void computeCurrentVelocity(int units, float max) {
        if (n < 2) { vx = vy = 0; return; }
        int last = (head - 1 + xs.length) % xs.length, first = last;
        for (int i = 1; i < n; i++) { int k = (last - i + xs.length) % xs.length; if (ts[last] - ts[k] > 100) break; first = k; }
        long dt = Math.max(1, ts[last] - ts[first]);
        vx = clamp((xs[last] - xs[first]) * units / dt, max); vy = clamp((ys[last] - ys[first]) * units / dt, max);
    }
    private static float clamp(float v, float m) { return Math.max(-m, Math.min(m, v)); }
    public float getXVelocity() { return vx; } public float getYVelocity() { return vy; }
    public float getXVelocity(int id) { return vx; } public float getYVelocity(int id) { return vy; }
    public boolean isAxisSupported(int axis) { return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y; }
    public float getAxisVelocity(int axis) { return axis == MotionEvent.AXIS_X ? vx : axis == MotionEvent.AXIS_Y ? vy : 0; }
    public float getAxisVelocity(int axis, int id) { return getAxisVelocity(axis); }
}
