package android.view;
/** Multi-pointer MotionEvent with history (Android touchscreen semantics for games/SDL/Arc). */
public final class MotionEvent extends InputEvent {
    public static final int ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2, ACTION_CANCEL = 3, ACTION_OUTSIDE = 4, ACTION_POINTER_DOWN = 5, ACTION_POINTER_UP = 6,
        ACTION_HOVER_MOVE = 7, ACTION_SCROLL = 8, ACTION_HOVER_ENTER = 9, ACTION_HOVER_EXIT = 10, ACTION_BUTTON_PRESS = 11, ACTION_BUTTON_RELEASE = 12,
        ACTION_MASK = 0xff, ACTION_POINTER_INDEX_MASK = 0xff00, ACTION_POINTER_INDEX_SHIFT = 8;
    public static final int TOOL_TYPE_UNKNOWN = 0, TOOL_TYPE_FINGER = 1, TOOL_TYPE_STYLUS = 2, TOOL_TYPE_MOUSE = 3, INVALID_POINTER_ID = -1, EDGE_TOP = 1, EDGE_BOTTOM = 2, EDGE_LEFT = 4, EDGE_RIGHT = 8;
    public static final int CLASSIFICATION_NONE = 0, CLASSIFICATION_AMBIGUOUS_GESTURE = 1, CLASSIFICATION_DEEP_PRESS = 2, CLASSIFICATION_TWO_FINGER_SWIPE = 3, CLASSIFICATION_MULTI_FINGER_SWIPE = 4, CLASSIFICATION_PINCH = 5;
    public static final int AXIS_X = 0, AXIS_Y = 1, AXIS_PRESSURE = 2, AXIS_SIZE = 3, AXIS_VSCROLL = 9, AXIS_HSCROLL = 10, BUTTON_PRIMARY = 1, BUTTON_SECONDARY = 2, BUTTON_TERTIARY = 4;
    private static final int MAX_POINTERS = 10, MAX_HISTORY = 32;

    private int action, meta, source = 0x1002 /* SOURCE_TOUCHSCREEN */, edge, deviceId;
    private int pointerCount = 1;
    private final int[] pointerIds = new int[MAX_POINTERS];
    private final float[] xs = new float[MAX_POINTERS], ys = new float[MAX_POINTERS];
    private final float[] pressures = new float[MAX_POINTERS], sizes = new float[MAX_POINTERS];
    private float rawDx, rawDy;
    private long down, time;
    private int historySize;
    private final long[] histTimes = new long[MAX_HISTORY];
    private final float[] histX = new float[MAX_HISTORY * MAX_POINTERS], histY = new float[MAX_HISTORY * MAX_POINTERS];

    private MotionEvent() { for (int i = 0; i < MAX_POINTERS; i++) { pointerIds[i] = i; pressures[i] = 1f; sizes[i] = 0.1f; } }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int meta) {
        MotionEvent e = new MotionEvent();
        e.down = downTime; e.time = eventTime; e.action = action; e.meta = meta;
        e.pointerCount = 1; e.pointerIds[0] = 0; e.xs[0] = x; e.ys[0] = y;
        return e;
    }
    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, float pressure, float size, int meta, float xPrec, float yPrec, int deviceId, int edgeFlags) {
        MotionEvent e = obtain(downTime, eventTime, action, x, y, meta);
        e.pressures[0] = pressure; e.sizes[0] = size; e.deviceId = deviceId; e.edge = edgeFlags;
        return e;
    }
    /** Multi-pointer obtain used by harness scripts (pointerIds 0..n-1, interleaved xy). */
    public static MotionEvent obtainPointers(long downTime, long eventTime, int action, int pointerCount, int[] ids, float[] x, float[] y, int meta) {
        MotionEvent e = new MotionEvent();
        e.down = downTime; e.time = eventTime; e.action = action; e.meta = meta;
        e.pointerCount = Math.max(1, Math.min(pointerCount, MAX_POINTERS));
        for (int i = 0; i < e.pointerCount; i++) {
            e.pointerIds[i] = ids != null && i < ids.length ? ids[i] : i;
            e.xs[i] = x[i]; e.ys[i] = y[i];
        }
        return e;
    }
    public static MotionEvent obtain(MotionEvent o) {
        MotionEvent e = new MotionEvent();
        e.action = o.action; e.meta = o.meta; e.source = o.source; e.edge = o.edge; e.deviceId = o.deviceId;
        e.rawDx = o.rawDx; e.rawDy = o.rawDy; e.down = o.down; e.time = o.time;
        e.pointerCount = o.pointerCount;
        System.arraycopy(o.pointerIds, 0, e.pointerIds, 0, MAX_POINTERS);
        System.arraycopy(o.xs, 0, e.xs, 0, MAX_POINTERS);
        System.arraycopy(o.ys, 0, e.ys, 0, MAX_POINTERS);
        System.arraycopy(o.pressures, 0, e.pressures, 0, MAX_POINTERS);
        System.arraycopy(o.sizes, 0, e.sizes, 0, MAX_POINTERS);
        e.historySize = o.historySize;
        System.arraycopy(o.histTimes, 0, e.histTimes, 0, MAX_HISTORY);
        System.arraycopy(o.histX, 0, e.histX, 0, o.histX.length);
        System.arraycopy(o.histY, 0, e.histY, 0, o.histY.length);
        return e;
    }
    public static MotionEvent obtainNoHistory(MotionEvent o) {
        MotionEvent e = obtain(o); e.historySize = 0; return e;
    }
    public MotionEvent copy() { return obtain(this); }

    /** Append one history sample (current pointers) before updating coords — for MOVE batches. */
    public void jetAddHistory(long eventTime) {
        if (historySize >= MAX_HISTORY) {
            System.arraycopy(histTimes, 1, histTimes, 0, MAX_HISTORY - 1);
            System.arraycopy(histX, pointerCount, histX, 0, (MAX_HISTORY - 1) * pointerCount);
            System.arraycopy(histY, pointerCount, histY, 0, (MAX_HISTORY - 1) * pointerCount);
            historySize = MAX_HISTORY - 1;
        }
        histTimes[historySize] = eventTime;
        for (int i = 0; i < pointerCount; i++) {
            histX[historySize * pointerCount + i] = xs[i];
            histY[historySize * pointerCount + i] = ys[i];
        }
        historySize++;
    }
    public void jetSetPointer(int index, float x, float y) {
        if (index >= 0 && index < pointerCount) { xs[index] = x; ys[index] = y; }
    }
    public void jetSetPointerCount(int n) { pointerCount = Math.max(1, Math.min(n, MAX_POINTERS)); }

    public int getAction() { return action; } public void setAction(int a) { action = a; }
    public int getActionMasked() { return action & ACTION_MASK; }
    public int getActionIndex() { return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT; }
    public float getX() { return xs[0]; } public float getY() { return ys[0]; }
    public float getX(int i) { return i >= 0 && i < pointerCount ? xs[i] : xs[0]; }
    public float getY(int i) { return i >= 0 && i < pointerCount ? ys[i] : ys[0]; }
    public float getRawX() { return xs[0] + rawDx; } public float getRawY() { return ys[0] + rawDy; }
    public float getRawX(int i) { return getX(i) + rawDx; } public float getRawY(int i) { return getY(i) + rawDy; }
    public void offsetLocation(float dx, float dy) {
        for (int i = 0; i < pointerCount; i++) { xs[i] += dx; ys[i] += dy; }
        rawDx -= dx; rawDy -= dy;
    }
    public void setLocation(float nx, float ny) { offsetLocation(nx - xs[0], ny - ys[0]); }
    public int getPointerCount() { return pointerCount; }
    public int getPointerId(int i) { return i >= 0 && i < pointerCount ? pointerIds[i] : 0; }
    public int findPointerIndex(int id) { for (int i = 0; i < pointerCount; i++) if (pointerIds[i] == id) return i; return -1; }
    public int getToolType(int i) { return TOOL_TYPE_FINGER; }
    public int getClassification() { return CLASSIFICATION_NONE; }
    public float getPressure() { return pressures[0]; } public float getPressure(int i) { return i >= 0 && i < pointerCount ? pressures[i] : 1f; }
    public float getSize() { return sizes[0]; } public float getSize(int i) { return i >= 0 && i < pointerCount ? sizes[i] : 0.1f; }
    public float getTouchMajor() { return 10f; } public float getTouchMinor() { return 10f; } public float getTouchMajor(int i) { return 10f; } public float getOrientation() { return 0f; }
    public float getAxisValue(int axis) { return axis == AXIS_X ? xs[0] : axis == AXIS_Y ? ys[0] : axis == AXIS_PRESSURE ? pressures[0] : 0f; }
    public float getAxisValue(int axis, int i) {
        if (axis == AXIS_X) return getX(i); if (axis == AXIS_Y) return getY(i); if (axis == AXIS_PRESSURE) return getPressure(i); return 0f;
    }
    public int getHistorySize() { return historySize; }
    public float getHistoricalX(int pos) { return getHistoricalX(0, pos); }
    public float getHistoricalY(int pos) { return getHistoricalY(0, pos); }
    public float getHistoricalX(int pointerIndex, int pos) {
        if (pos < 0 || pos >= historySize || pointerIndex < 0 || pointerIndex >= pointerCount) return getX(pointerIndex);
        return histX[pos * pointerCount + pointerIndex];
    }
    public float getHistoricalY(int pointerIndex, int pos) {
        if (pos < 0 || pos >= historySize || pointerIndex < 0 || pointerIndex >= pointerCount) return getY(pointerIndex);
        return histY[pos * pointerCount + pointerIndex];
    }
    public long getHistoricalEventTime(int pos) { return pos >= 0 && pos < historySize ? histTimes[pos] : time; }
    public long getDownTime() { return down; } public long getEventTime() { return time; } public long getEventTimeNanos() { return time * 1_000_000L; }
    public int getMetaState() { return meta; } public int getButtonState() { return 0; } public int getEdgeFlags() { return edge; } public void setEdgeFlags(int f) { edge = f; }
    public int getSource() { return source; } public void setSource(int s) { source = s; } public boolean isFromSource(int s) { return (source & s) == s; }
    public int getDeviceId() { return deviceId; } public int getFlags() { return 0; }
    public float getXPrecision() { return 1f; } public float getYPrecision() { return 1f; }
    public void transform(android.graphics.Matrix m) {}
    public void recycle() {}
    public static String actionToString(int a) {
        int masked = a & ACTION_MASK;
        switch (masked) {
            case 0: return "ACTION_DOWN"; case 1: return "ACTION_UP"; case 2: return "ACTION_MOVE"; case 3: return "ACTION_CANCEL";
            case 5: return "ACTION_POINTER_DOWN(" + ((a & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT) + ")";
            case 6: return "ACTION_POINTER_UP(" + ((a & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT) + ")";
            default: return "ACTION_" + a;
        }
    }
    @Override public String toString() { return "MotionEvent { action=" + actionToString(action) + ", pointers=" + pointerCount + ", x=" + xs[0] + ", y=" + ys[0] + ", hist=" + historySize + " }"; }
}
