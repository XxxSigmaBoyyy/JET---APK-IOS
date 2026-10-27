package android.view;
import android.graphics.Canvas;
import android.graphics.Rect;
/** Headless Surface: a handle onto a SurfaceView's frame buffer (GL frames arrive via jet.gl.FrameSink). */
public class Surface implements android.os.Parcelable {
    public static final int ROTATION_0 = 0, ROTATION_90 = 1, ROTATION_180 = 2, ROTATION_270 = 3;
    public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1, CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS = 0, CHANGE_FRAME_RATE_ALWAYS = 1;
    public static class OutOfResourcesException extends RuntimeException { public OutOfResourcesException() {} public OutOfResourcesException(String s) { super(s); } }
    private final SurfaceView view; private final android.graphics.SurfaceTexture texture; private volatile boolean valid;
    Surface(SurfaceView v) { view = v; texture = null; }
    /** Surface onto a SurfaceTexture (TextureView / camera / media consumers): GL frames go to the texture. */
    public Surface(android.graphics.SurfaceTexture st) { view = null; texture = st; valid = st != null; }
    void jetSetValid(boolean v) { valid = v; }
    public jet.gl.FrameSink jetSink() { return view != null ? view : texture; }
    public boolean isValid() { return valid; }
    public void release() {}
    public Canvas lockCanvas(Rect dirty) { if (view == null) throw new IllegalArgumentException("software lockCanvas on a SurfaceTexture surface is not supported on the JET host"); return view.lock(); }
    public Canvas lockHardwareCanvas() { return lockCanvas(null); }
    public void unlockCanvasAndPost(Canvas c) { if (view != null) view.unlock(c); }
    public void setFrameRate(float rate, int compat) {} public void setFrameRate(float rate, int compat, int strategy) {}
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
    @Override public String toString() { return "Surface(name=jet, valid=" + valid + ")"; }
}
