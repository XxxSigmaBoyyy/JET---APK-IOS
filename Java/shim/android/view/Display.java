package android.view;
import android.util.DisplayMetrics;
/** The single host display; matches the harness window and DisplayMetrics defaults (750x1334 @ 320dpi, 60 Hz). */
public final class Display {
    public static final int DEFAULT_DISPLAY = 0, INVALID_DISPLAY = -1;
    public static final int STATE_UNKNOWN = 0, STATE_OFF = 1, STATE_ON = 2, STATE_DOZE = 3, STATE_DOZE_SUSPEND = 4, STATE_VR = 5, STATE_ON_SUSPEND = 6;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 1, FLAG_SECURE = 2, FLAG_PRIVATE = 4, FLAG_PRESENTATION = 8, FLAG_ROUND = 16;
    public static final class Mode {
        private final int id, w, h; private final float rate;
        Mode(int id, int w, int h, float rate) { this.id = id; this.w = w; this.h = h; this.rate = rate; }
        public int getModeId() { return id; } public int getPhysicalWidth() { return w; } public int getPhysicalHeight() { return h; }
        public float getRefreshRate() { return rate; } public float[] getAlternativeRefreshRates() { return new float[0]; }
    }
    private static final Display DEFAULT = new Display();
    public static Display jetDefault() { return DEFAULT; }
    public Display() {}
    private static DisplayMetrics dm() { return new DisplayMetrics(); }
    public int getDisplayId() { return 0; }
    public String getName() { return "Built-in Screen"; }
    public boolean isValid() { return true; }
    public int getFlags() { return FLAG_SUPPORTS_PROTECTED_BUFFERS | FLAG_SECURE; }
    public int getState() { return STATE_ON; }
    public int getWidth() { return dm().widthPixels; }
    public float jetDensity() { return dm().density; }
    public int getHeight() { return dm().heightPixels; }
    public void getSize(android.graphics.Point p) { DisplayMetrics m = dm(); p.x = m.widthPixels; p.y = m.heightPixels; }
    public void getRealSize(android.graphics.Point p) { getSize(p); }
    public void getRectSize(android.graphics.Rect r) { DisplayMetrics m = dm(); r.set(0, 0, m.widthPixels, m.heightPixels); }
    public void getCurrentSizeRange(android.graphics.Point min, android.graphics.Point max) { DisplayMetrics m = dm(); int a = Math.min(m.widthPixels, m.heightPixels), b = Math.max(m.widthPixels, m.heightPixels); min.x = min.y = a; max.x = max.y = b; }
    public void getMetrics(DisplayMetrics out) { out.setTo(dm()); }
    public void getRealMetrics(DisplayMetrics out) { out.setTo(dm()); }
    public int getRotation() { return DisplayMetrics.jetLandscape() ? Surface.ROTATION_90 : Surface.ROTATION_0; }
    @Deprecated public int getOrientation() { return 0; }
    public float getRefreshRate() { return 60f; }
    public float[] getSupportedRefreshRates() { return new float[] { 60f }; }
    public Mode getMode() { DisplayMetrics m = dm(); return new Mode(1, m.widthPixels, m.heightPixels, 60f); }
    public Mode[] getSupportedModes() { return new Mode[] { getMode() }; }
    public int getPixelFormat() { return 1; /* RGBA_8888 */ }
    public long getAppVsyncOffsetNanos() { return 0; } public long getPresentationDeadlineNanos() { return 16_666_666L; }
    public boolean isHdr() { return false; } public boolean isWideColorGamut() { return false; }
    @Override public String toString() { return "Display id 0: Built-in Screen " + getWidth() + "x" + getHeight(); }
}
