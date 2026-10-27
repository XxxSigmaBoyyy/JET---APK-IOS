package android.opengl;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
/** GLSurfaceView with a real render thread (AOSP semantics, simplified state machine).
 * EGL is jet.gl.JetEGL10 (Mesa surfaceless on Linux; arm64 GLES/ANGLE on iOS). */
public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback2 {
    public static final int RENDERMODE_WHEN_DIRTY = 0, RENDERMODE_CONTINUOUSLY = 1;
    public static final int DEBUG_CHECK_GL_ERROR = 1, DEBUG_LOG_GL_CALLS = 2;
    public interface Renderer {
        void onSurfaceCreated(GL10 gl, EGLConfig config);
        void onSurfaceChanged(GL10 gl, int width, int height);
        void onDrawFrame(GL10 gl);
    }
    public interface EGLConfigChooser { EGLConfig chooseConfig(EGL10 egl, EGLDisplay display); }
    public interface EGLContextFactory { EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig cfg); void destroyContext(EGL10 egl, EGLDisplay display, EGLContext ctx); }
    public interface EGLWindowSurfaceFactory { EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig cfg, Object nativeWindow); void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface s); }
    public interface GLWrapper { GL wrap(GL gl); }

    private Renderer renderer; private EGLConfigChooser chooser; private EGLContextFactory ctxFactory; private EGLWindowSurfaceFactory surfFactory; private GLWrapper wrapper;
    private int clientVersion, debugFlags; private boolean preserve; private GLThread thread; private boolean detached;

    public GLSurfaceView(Context c) { this(c, null); }
    public GLSurfaceView(Context c, AttributeSet a) { super(c, a); getHolder().addCallback(this); }
    private void checkNoThread() { if (thread != null) throw new IllegalStateException("setRenderer has already been called for this instance."); }
    public void setGLWrapper(GLWrapper w) { wrapper = w; }
    public void setDebugFlags(int f) { debugFlags = f; } public int getDebugFlags() { return debugFlags; }
    public void setPreserveEGLContextOnPause(boolean p) { preserve = p; } public boolean getPreserveEGLContextOnPause() { return preserve; }
    public void setEGLContextClientVersion(int v) { checkNoThread(); clientVersion = v; }
    public void setEGLConfigChooser(EGLConfigChooser c) { checkNoThread(); chooser = c; }
    public void setEGLConfigChooser(boolean depth) { setEGLConfigChooser(new ComponentChooser(8, 8, 8, 0, depth ? 16 : 0, 0)); }
    public void setEGLConfigChooser(int r, int g, int b, int a, int depth, int stencil) { setEGLConfigChooser(new ComponentChooser(r, g, b, a, depth, stencil)); }
    public void setEGLContextFactory(EGLContextFactory f) { checkNoThread(); ctxFactory = f; }
    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory f) { checkNoThread(); surfFactory = f; }
    public void setRenderer(Renderer r) {
        checkNoThread();
        if (chooser == null) chooser = new ComponentChooser(8, 8, 8, 0, 16, 0);
        if (ctxFactory == null) ctxFactory = new EGLContextFactory() {
            public EGLContext createContext(EGL10 egl, EGLDisplay d, EGLConfig c) {
                int[] at = clientVersion != 0 ? new int[] { 0x3098 /*EGL_CONTEXT_CLIENT_VERSION*/, clientVersion, EGL10.EGL_NONE } : null;
                return egl.eglCreateContext(d, c, EGL10.EGL_NO_CONTEXT, at);
            }
            public void destroyContext(EGL10 egl, EGLDisplay d, EGLContext x) { egl.eglDestroyContext(d, x); }
        };
        if (surfFactory == null) surfFactory = new EGLWindowSurfaceFactory() {
            public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay d, EGLConfig c, Object w) { return egl.eglCreateWindowSurface(d, c, w, null); }
            public void destroySurface(EGL10 egl, EGLDisplay d, EGLSurface s) { egl.eglDestroySurface(d, s); }
        };
        renderer = r;
        thread = new GLThread(); thread.start();
    }
    public void setRenderMode(int m) { if (thread != null) thread.setMode(m); else pendingMode = m; }
    private int pendingMode = RENDERMODE_CONTINUOUSLY;
    public int getRenderMode() { return thread != null ? thread.mode : pendingMode; }
    public void requestRender() { if (thread != null) thread.request(); }
    public void onPause() { if (thread != null) thread.setPaused(true); }
    public void onResume() { if (thread != null) thread.setPaused(false); }
    public void queueEvent(Runnable r) { if (r == null) throw new IllegalArgumentException("r must not be null"); if (thread != null) thread.queue(r); }
    public Renderer jetRenderer() { return renderer; }

    @Override public void surfaceCreated(SurfaceHolder h) { if (thread != null) thread.surface(true, 0, 0); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { if (thread != null) thread.surface(true, w, hh); }
    @Override public void surfaceDestroyed(SurfaceHolder h) { if (thread != null) thread.surface(false, 0, 0); }
    @Override public void surfaceRedrawNeeded(SurfaceHolder h) { requestRender(); }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (detached && renderer != null) { int m = thread != null ? thread.mode : RENDERMODE_CONTINUOUSLY; thread = new GLThread(); thread.mode = m; thread.start(); }
        detached = false;
    }
    @Override protected void onDetachedFromWindow() { if (thread != null) thread.exit(); detached = true; super.onDetachedFromWindow(); }

    private final class ComponentChooser implements EGLConfigChooser {
        final int[] spec;
        ComponentChooser(int r, int g, int b, int a, int depth, int stencil) {
            spec = new int[] { EGL10.EGL_RED_SIZE, r, EGL10.EGL_GREEN_SIZE, g, EGL10.EGL_BLUE_SIZE, b, EGL10.EGL_ALPHA_SIZE, a, EGL10.EGL_DEPTH_SIZE, depth, EGL10.EGL_STENCIL_SIZE, stencil, EGL10.EGL_RENDERABLE_TYPE, 0, EGL10.EGL_NONE };
            rgba = new int[] { r, g, b, a, depth, stencil };
        }
        final int[] rgba;
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay d) {
            int[] s = spec.clone(); s[13] = clientVersion >= 3 ? 0x40 : clientVersion == 2 ? 4 : 1;
            int[] n = new int[1];
            if (!egl.eglChooseConfig(d, s, null, 0, n) || n[0] <= 0) throw new IllegalArgumentException("No configs match configSpec");
            EGLConfig[] cs = new EGLConfig[n[0]]; egl.eglChooseConfig(d, s, cs, n[0], n);
            int[] v = new int[1];
            for (EGLConfig c : cs) {
                int[] got = new int[6]; int[] at = { EGL10.EGL_RED_SIZE, EGL10.EGL_GREEN_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE };
                for (int i = 0; i < 6; i++) { egl.eglGetConfigAttrib(d, c, at[i], v); got[i] = v[0]; }
                if (got[0] == rgba[0] && got[1] == rgba[1] && got[2] == rgba[2] && got[3] == rgba[3] && got[4] >= rgba[4] && got[5] >= rgba[5]) return c;
            }
            return cs[0];
        }
    }

    private final class GLThread extends Thread {
        int mode = pendingMode; private boolean paused, hasSurface, request = true, exit, sizeChanged = true; private int w, h;
        private final ArrayList<Runnable> events = new ArrayList<>();
        private EGL10 egl; private EGLDisplay dpy; private EGLConfig cfg; private EGLContext ctx; private EGLSurface surf; private GL10 gl; private boolean createdCalled;
        GLThread() { super("GLThread"); setDaemon(true); }
        synchronized void setMode(int m) { mode = m; notifyAll(); }
        synchronized void request() { request = true; notifyAll(); }
        synchronized void setPaused(boolean p) { paused = p; request = true; notifyAll(); }
        synchronized void queue(Runnable r) { events.add(r); notifyAll(); }
        synchronized void exit() { exit = true; notifyAll(); }
        synchronized void surface(boolean has, int nw, int nh) {
            if (!has && hasSurface) { hasSurface = false; destroySurfaceRequested = true; }
            else if (has) { hasSurface = true; if (nw > 0 && (nw != w || nh != h)) { w = nw; h = nh; sizeChanged = true; } }
            request = true; notifyAll();
        }
        private boolean destroySurfaceRequested;
        @Override public void run() {
            try { loop(); }
            finally { releaseSurface(); if (ctx != null) { ctxFactory.destroyContext(egl, dpy, ctx); ctx = null; } }
        }
        private void loop() {
            while (true) {
                Runnable ev = null; boolean draw = false, changed = false; int cw, ch;
                synchronized (this) {
                    while (true) {
                        if (exit) return;
                        if (!events.isEmpty()) { ev = events.remove(0); break; }
                        if (destroySurfaceRequested) { destroySurfaceRequested = false; releaseSurface(); }
                        if (paused && ctx != null && !preserve) { releaseSurface(); ctxFactory.destroyContext(egl, dpy, ctx); ctx = null; createdCalled = false; }
                        if (!paused && hasSurface && w > 0 && h > 0 && (request || mode == RENDERMODE_CONTINUOUSLY)) {
                            draw = true; request = false; changed = sizeChanged; sizeChanged = false; break;
                        }
                        try { wait(); } catch (InterruptedException e) { return; }
                    }
                    cw = w; ch = h;
                }
                if (ev != null) { ev.run(); continue; }
                if (!draw) continue;
                if (egl == null) { egl = (EGL10) EGLContext.getEGL(); dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY); if (!egl.eglInitialize(dpy, new int[2])) throw new RuntimeException("eglInitialize failed"); cfg = chooser.chooseConfig(egl, dpy); }
                if (ctx == null) { ctx = ctxFactory.createContext(egl, dpy, cfg); if (ctx == null || ctx == EGL10.EGL_NO_CONTEXT) throw new RuntimeException("createContext failed: 0x" + Integer.toHexString(egl.eglGetError())); createdCalled = false; }
                if (surf == null || changed) {
                    releaseSurface();
                    surf = surfFactory.createWindowSurface(egl, dpy, cfg, getHolder());
                    if (surf == null || surf == EGL10.EGL_NO_SURFACE) throw new RuntimeException("createWindowSurface failed: 0x" + Integer.toHexString(egl.eglGetError()));
                    if (!egl.eglMakeCurrent(dpy, surf, surf, ctx)) throw new RuntimeException("eglMakeCurrent failed: 0x" + Integer.toHexString(egl.eglGetError()));
                    GL g = ctx.getGL(); if (wrapper != null) g = wrapper.wrap(g); gl = (GL10) g;
                    if (!createdCalled) { createdCalled = true; renderer.onSurfaceCreated(gl, cfg); }
                    renderer.onSurfaceChanged(gl, cw, ch);
                }
                renderer.onDrawFrame(gl);
                egl.eglSwapBuffers(dpy, surf);
                if (mode == RENDERMODE_CONTINUOUSLY) try { Thread.sleep(16); } catch (InterruptedException e) { return; } // vsync stand-in
            }
        }
        private void releaseSurface() {
            if (surf != null && surf != EGL10.EGL_NO_SURFACE && egl != null) {
                egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                surfFactory.destroySurface(egl, dpy, surf);
            }
            surf = null;
        }
    }
}
