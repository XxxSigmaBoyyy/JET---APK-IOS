package android.view;
import android.graphics.Canvas;
import android.graphics.Rect;
public interface SurfaceHolder {
    int SURFACE_TYPE_NORMAL = 0, SURFACE_TYPE_HARDWARE = 1, SURFACE_TYPE_GPU = 2, SURFACE_TYPE_PUSH_BUFFERS = 3;
    interface Callback {
        void surfaceCreated(SurfaceHolder h);
        void surfaceChanged(SurfaceHolder h, int format, int width, int height);
        void surfaceDestroyed(SurfaceHolder h);
    }
    interface Callback2 extends Callback {
        void surfaceRedrawNeeded(SurfaceHolder h);
        default void surfaceRedrawNeededAsync(SurfaceHolder h, Runnable drawingFinished) { surfaceRedrawNeeded(h); drawingFinished.run(); }
    }
    class BadSurfaceTypeException extends RuntimeException { public BadSurfaceTypeException() {} public BadSurfaceTypeException(String s) { super(s); } }
    void addCallback(Callback c);
    void removeCallback(Callback c);
    boolean isCreating();
    void setType(int type);
    void setFixedSize(int w, int h);
    void setSizeFromLayout();
    void setFormat(int format);
    void setKeepScreenOn(boolean on);
    Canvas lockCanvas();
    Canvas lockCanvas(Rect dirty);
    default Canvas lockHardwareCanvas() { return lockCanvas(); }
    void unlockCanvasAndPost(Canvas c);
    Rect getSurfaceFrame();
    Surface getSurface();
}
