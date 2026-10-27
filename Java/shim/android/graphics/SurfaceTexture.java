package android.graphics;
/** Headless SurfaceTexture: a GL frame target. EGL window surfaces made from it render into a host pbuffer; every eglSwapBuffers
 *  posts the frame here (jet.gl.FrameSink) and, when owned by a TextureView, the view shows it. Linux-only Mesa path;
 *  on iOS this maps to a CAMetalLayer/IOSurface-backed texture. */
public class SurfaceTexture implements jet.gl.FrameSink {
    public interface OnFrameAvailableListener { void onFrameAvailable(SurfaceTexture surfaceTexture); }
    public static class OutOfResourcesException extends Exception { public OutOfResourcesException() {} public OutOfResourcesException(String s) { super(s); } }
    private int bufW = 1, bufH = 1; private boolean released; private long ts;
    private volatile OnFrameAvailableListener listener; private android.os.Handler handler;
    private volatile jet.gl.FrameSink owner;
    private volatile jet.gfx.PixelBuffer lastFrame;
    /** Most recent ST that received an EGL frame (TextureView may not own the ST the app passed to eglCreateWindowSurface). */
    public static volatile SurfaceTexture lastPosted;
    public SurfaceTexture(int texName) {}
    public SurfaceTexture(int texName, boolean singleBufferMode) {}
    public SurfaceTexture(boolean singleBufferMode) {}
    public void setOnFrameAvailableListener(OnFrameAvailableListener l) { listener = l; handler = null; }
    public void setOnFrameAvailableListener(OnFrameAvailableListener l, android.os.Handler h) { listener = l; handler = h; }
    public void setDefaultBufferSize(int w, int h) { bufW = Math.max(1, w); bufH = Math.max(1, h); }
    public void updateTexImage() {}
    public void releaseTexImage() {}
    public void attachToGLContext(int texName) {}
    public void detachFromGLContext() {}
    public void getTransformMatrix(float[] mtx) { if (mtx != null && mtx.length >= 16) { java.util.Arrays.fill(mtx, 0f); mtx[0] = mtx[5] = mtx[10] = mtx[15] = 1f; } }
    public long getTimestamp() { return ts; }
    public void release() { released = true; }
    public boolean isReleased() { return released; }
    /** JET: TextureView owning this texture receives the frames. */
    public void jetSetOwner(jet.gl.FrameSink o) { owner = o; }
    public int jetSinkWidth() { return bufW; }
    public int jetSinkHeight() { return bufH; }
    public jet.gfx.PixelBuffer jetLastFrame() { return lastFrame; }
    public void jetPostFrame(int[] argb, int w, int h) {
        ts = System.nanoTime();
        jet.gl.FrameSink o = owner;
        lastFrame = new jet.gfx.PixelBuffer(w, h, argb.clone()); lastPosted = this;
        if (Boolean.getBoolean("jet.texdebug") && (jet.gl.JetEGL10.frames < 3 || jet.gl.JetEGL10.frames % 120 == 0)) System.err.println("[JETTEX] SurfaceTexture.jetPostFrame " + w + "x" + h + " owner=" + o);
        if (o != null) o.jetPostFrame(argb, w, h);
        final OnFrameAvailableListener l = listener;
        if (l != null) { if (handler != null) handler.post(() -> l.onFrameAvailable(this)); else l.onFrameAvailable(this); }
    }
}
