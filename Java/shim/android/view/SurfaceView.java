package android.view;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Rect;
import java.util.concurrent.CopyOnWriteArrayList;
/** Headless SurfaceView. The surface is "created" once attached and laid out with a non-zero size; software frames come from
 * lockCanvas, GL frames from eglSwapBuffers (jet.gl.FrameSink). The latest frame is drawn as the view content. */
public class SurfaceView extends View implements jet.gl.FrameSink {
    private final CopyOnWriteArrayList<SurfaceHolder.Callback> callbacks = new CopyOnWriteArrayList<>();
    private final Surface surface = new Surface(this);
    private volatile jet.gfx.PixelBuffer frame; private jet.gfx.PixelBuffer soft; private int fixedW, fixedH, format = -1;
    private boolean created, creating; private int lastW = -1, lastH = -1;
    public static volatile int jetFramesPosted;
    private final SurfaceHolder holder = new SurfaceHolder() {
        public void addCallback(Callback c) { if (!callbacks.contains(c)) callbacks.add(c); }
        public void removeCallback(Callback c) { callbacks.remove(c); }
        public boolean isCreating() { return creating; }
        public void setType(int t) {}
        public void setFixedSize(int w, int h) { fixedW = w; fixedH = h; jetUpdateSurface(); }
        public void setSizeFromLayout() { fixedW = fixedH = 0; jetUpdateSurface(); }
        public void setFormat(int f) { format = f; }
        public void setKeepScreenOn(boolean on) { SurfaceView.this.setKeepScreenOn(on); }
        public Canvas lockCanvas() { return lock(); }
        public Canvas lockCanvas(Rect dirty) { return lock(); }
        public void unlockCanvasAndPost(Canvas c) { unlock(c); }
        public Rect getSurfaceFrame() { return new Rect(0, 0, jetSinkWidth(), jetSinkHeight()); }
        public Surface getSurface() { return surface; }
    };
    public SurfaceView(Context c) { this(c, null); }
    public SurfaceView(Context c, AttributeSet a) { super(c, a); }
    public SurfaceView(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public SurfaceView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }
    public SurfaceHolder getHolder() { return holder; }
    public void setZOrderOnTop(boolean b) {} public void setZOrderMediaOverlay(boolean b) {} public void setSecure(boolean b) {}
    public int jetSinkWidth() { return fixedW > 0 ? fixedW : Math.max(1, getWidth()); }
    public int jetSinkHeight() { return fixedH > 0 ? fixedH : Math.max(1, getHeight()); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); jetUpdateSurface(); }
    @Override protected void onDetachedFromWindow() {
        if (created) { created = false; surface.jetSetValid(false); for (SurfaceHolder.Callback c : callbacks) c.surfaceDestroyed(holder); }
        super.onDetachedFromWindow();
    }
    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); jetUpdateSurface(); }
    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) { super.onLayout(ch, l, t, r, b); jetUpdateSurface(); }
    /** surfaceCreated once attached+sized, surfaceChanged on every size change (Android order). */
    void jetUpdateSurface() {
        if (!isAttachedToWindow() || getWidth() <= 0 || getHeight() <= 0) return;
        int w = jetSinkWidth(), h = jetSinkHeight();
        if (!created) {
            created = true; creating = true; surface.jetSetValid(true);
            try { for (SurfaceHolder.Callback c : callbacks) c.surfaceCreated(holder); } finally { creating = false; }
        }
        if (w != lastW || h != lastH) {
            lastW = w; lastH = h;
            for (SurfaceHolder.Callback c : callbacks) c.surfaceChanged(holder, format == -1 ? 4 /*RGB_565*/ : format, w, h);
            for (SurfaceHolder.Callback c : callbacks) if (c instanceof SurfaceHolder.Callback2) ((SurfaceHolder.Callback2) c).surfaceRedrawNeeded(holder);
        }
    }
    @Override public void jetPostFrame(int[] argb, int w, int h) {
        jet.gfx.PixelBuffer img = new jet.gfx.PixelBuffer(w, h, argb.clone()); img.opaque = true;
        frame = img; jetFramesPosted++; postInvalidate();
    }
    public jet.gfx.PixelBuffer jetLastFrame() { return frame; }
    Canvas lock() {
        int w = jetSinkWidth(), h = jetSinkHeight();
        if (soft == null || soft.w != w || soft.h != h) soft = new jet.gfx.PixelBuffer(w, h);
        return new Canvas(android.graphics.JetGfx.forBitmap(soft));
    }
    void unlock(Canvas c) { if (c != null && c.jetGfx() != null) { c.jetGfx().flush(); c.jetGfx().dispose(); } frame = soft; jetFramesPosted++; postInvalidate(); }
    @Override public void draw(Canvas g) {
        jet.gfx.PixelBuffer f = frame;
        if (f != null && getVisibility() == VISIBLE && g != null) { java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getScaleInstance(getWidth() / (double) f.w, getHeight() / (double) f.h); g.jetDrawImage(f, at, null); }
        super.draw(g);
    }
}
