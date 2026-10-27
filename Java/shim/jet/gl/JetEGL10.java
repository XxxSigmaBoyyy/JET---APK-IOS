package jet.gl;
import javax.microedition.khronos.egl.*;
import java.util.concurrent.ConcurrentHashMap;
/** EGL10/EGL11 over Mesa surfaceless EGL (Linux-only host bridge).
 * Window surfaces become pbuffers sized from the SurfaceView; eglSwapBuffers reads the frame back into the view. */
public final class JetEGL10 implements EGL11 {
    private static final JetEGL10 INSTANCE = new JetEGL10();
    public static JetEGL10 get() { return INSTANCE; }
    public static volatile int frames, texFrames; public static volatile String renderer; public static volatile long firstFrameNanos;

    static final class Dpy extends EGLDisplay { final long h; Dpy(long h) { this.h = h; } }
    static final class Cfg extends EGLConfig { final long h; Cfg(long h) { this.h = h; } }
    static final class Ctx extends EGLContext { final long h; private javax.microedition.khronos.opengles.GL gl;
        Ctx(long h) { this.h = h; }
        @Override public synchronized javax.microedition.khronos.opengles.GL getGL() { if (gl == null) gl = JetGL10.create(); return gl; } }
    static final class Surf extends EGLSurface { final long h; final FrameSink sink; final int w, hh; int[] buf;
        Surf(long h, FrameSink s, int w, int hh) { this.h = h; sink = s; this.w = w; this.hh = hh; } }
    private final ConcurrentHashMap<Long, Object> objs = new ConcurrentHashMap<>();
    private volatile int lastError = EGL_SUCCESS;
    @SuppressWarnings("unchecked") private <T> T obj(long h, java.util.function.LongFunction<T> mk) { return h == 0 ? null : (T) objs.computeIfAbsent(h, k -> mk.apply(k)); }
    private static long h(EGLDisplay d) { return d instanceof Dpy ? ((Dpy) d).h : 0; }
    private static long h(EGLConfig c) { return c instanceof Cfg ? ((Cfg) c).h : 0; }
    private static long h(EGLContext c) { return c instanceof Ctx ? ((Ctx) c).h : 0; }
    private static long h(EGLSurface s) { return s instanceof Surf ? ((Surf) s).h : 0; }
    private boolean ok(boolean b) { lastError = b ? EGL_SUCCESS : JetEGLNative.getError(); return b; }

    @Override public EGLDisplay eglGetDisplay(Object nativeDisplay) { return obj(JetEGLNative.getDisplay(), Dpy::new); }
    @Override public boolean eglInitialize(EGLDisplay d, int[] ver) {
        boolean r = ok(JetEGLNative.initialize(h(d), new int[2]));
        if (r && ver != null && ver.length >= 2) { ver[0] = 1; ver[1] = 4; } // Android reports EGL 1.4
        return r;
    }
    @Override public boolean eglTerminate(EGLDisplay d) { return true; /* keep the shared host display alive */ }
    @Override public int eglGetError() { int e = lastError; lastError = EGL_SUCCESS; return e; }
    @Override public String eglQueryString(EGLDisplay d, int n) { String s = JetEGLNative.queryString(h(d), n); return s == null ? "" : s; }

    /** Window-surface requests are served by pbuffers: rewrite EGL_SURFACE_TYPE to PBUFFER_BIT. */
    private static boolean msaaLogged;
    static int[] hostAttribs(int[] a) {
        java.util.ArrayList<Integer> o = new java.util.ArrayList<>(); boolean st = false;
        if (a != null) for (int i = 0; i + 1 < a.length && a[i] != EGL_NONE; i += 2) {
            int k = a[i], v = a[i + 1];
            if (k == EGL_SURFACE_TYPE) { v = EGL_PBUFFER_BIT; st = true; }
            // Mesa surfaceless MSAA pbuffers can't be read back (glReadPixels -> INVALID_OPERATION, blit-resolve gives
            // zeros; reproduced in a 30-line C test), which showed as noise/black GL views (Telegram intro asks 4x MSAA).
            // Request a single-sample config instead (no antialiasing on this host). -Djet.gl.msaa=1 keeps the request.
            if ((k == 0x3031 /*EGL_SAMPLES*/ || k == 0x3032 /*EGL_SAMPLE_BUFFERS*/) && v > 0 && !Boolean.getBoolean("jet.gl.msaa")) {
                if (!msaaLogged) { msaaLogged = true; System.err.println("[JETGL] EGL_SAMPLES/SAMPLE_BUFFERS request dropped (host MSAA pbuffer readback unsupported)"); }
                continue;
            }
            o.add(k); o.add(v);
        }
        if (!st) { o.add(EGL_SURFACE_TYPE); o.add(EGL_PBUFFER_BIT); }
        o.add(EGL_NONE);
        int[] r = new int[o.size()]; for (int i = 0; i < r.length; i++) r[i] = o.get(i); return r;
    }
    @Override public boolean eglChooseConfig(EGLDisplay d, int[] attrs, EGLConfig[] cfgs, int size, int[] num) {
        long[] out = cfgs == null ? null : new long[Math.min(size, cfgs.length)];
        int n = JetEGLNative.chooseConfig(h(d), hostAttribs(attrs), out);
        if (!ok(n >= 0)) return false;
        if (num != null && num.length > 0) num[0] = n;
        if (out != null) for (int i = 0; i < n && i < out.length; i++) cfgs[i] = obj(out[i], Cfg::new);
        return true;
    }
    @Override public boolean eglGetConfigs(EGLDisplay d, EGLConfig[] cfgs, int size, int[] num) {
        return eglChooseConfig(d, new int[] { EGL_NONE }, cfgs, size, num);
    }
    @Override public boolean eglGetConfigAttrib(EGLDisplay d, EGLConfig c, int attr, int[] v) {
        int r = JetEGLNative.getConfigAttrib(h(d), h(c), attr);
        if (!ok(r != -1 || attr == EGL_TRANSPARENT_RED_VALUE)) return false;
        if (attr == EGL_SURFACE_TYPE) r |= EGL_WINDOW_BIT; // windows are emulated
        if (v != null && v.length > 0) v[0] = r; return true;
    }
    @Override public EGLContext eglCreateContext(EGLDisplay d, EGLConfig c, EGLContext share, int[] attrs) {
        long x = JetEGLNative.createContext(h(d), h(c), h(share), attrs);
        ok(x != 0); return x == 0 ? EGL_NO_CONTEXT : obj(x, Ctx::new);
    }
    @Override public boolean eglDestroyContext(EGLDisplay d, EGLContext c) { objs.remove(h(c)); return ok(JetEGLNative.destroyContext(h(d), h(c))); }
    private EGLSurface pbuf(EGLDisplay d, EGLConfig c, FrameSink sink, int w, int hh) {
        long s = JetEGLNative.createPbuffer(h(d), h(c), Math.max(1, w), Math.max(1, hh));
        if (!ok(s != 0)) return EGL_NO_SURFACE;
        Surf o = new Surf(s, sink, Math.max(1, w), Math.max(1, hh)); objs.put(s, o); return o;
    }
    static FrameSink sinkOf(Object win) {
        if (win instanceof FrameSink) return (FrameSink) win;
        if (win instanceof android.view.SurfaceHolder) { Object s = ((android.view.SurfaceHolder) win).getSurface(); if (s instanceof android.view.Surface) return ((android.view.Surface) s).jetSink(); }
        if (win instanceof android.view.Surface) return ((android.view.Surface) win).jetSink();
        if (win instanceof android.graphics.SurfaceTexture) return (android.graphics.SurfaceTexture) win;
        if (win instanceof android.view.TextureView) { android.graphics.SurfaceTexture t = ((android.view.TextureView) win).getSurfaceTexture(); if (t != null) return t; }
        return null;
    }
    @Override public EGLSurface eglCreateWindowSurface(EGLDisplay d, EGLConfig c, Object win, int[] attrs) {
        FrameSink s = sinkOf(win); if (Boolean.getBoolean("jet.texdebug")) System.err.println("[JETTEX] eglCreateWindowSurface win=" + (win == null ? null : win.getClass().getName()) + " sink=" + s);
        return pbuf(d, c, s, s == null ? 1 : s.jetSinkWidth(), s == null ? 1 : s.jetSinkHeight());
    }
    @Override public EGLSurface eglCreatePbufferSurface(EGLDisplay d, EGLConfig c, int[] attrs) {
        int w = 1, hh = 1;
        if (attrs != null) for (int i = 0; i + 1 < attrs.length && attrs[i] != EGL_NONE; i += 2) { if (attrs[i] == EGL_WIDTH) w = attrs[i + 1]; if (attrs[i] == EGL_HEIGHT) hh = attrs[i + 1]; }
        return pbuf(d, c, null, w, hh);
    }
    @Override public EGLSurface eglCreatePixmapSurface(EGLDisplay d, EGLConfig c, Object p, int[] a) { lastError = EGL_BAD_MATCH; return EGL_NO_SURFACE; }
    @Override public boolean eglDestroySurface(EGLDisplay d, EGLSurface s) { objs.remove(h(s)); return ok(JetEGLNative.destroySurface(h(d), h(s))); }
    private final java.util.Set<Long> viewportInit = java.util.concurrent.ConcurrentHashMap.newKeySet();
    @Override public boolean eglMakeCurrent(EGLDisplay d, EGLSurface draw, EGLSurface read, EGLContext c) {
        boolean r = ok(JetEGLNative.makeCurrent(h(d), h(draw), h(read), h(c)));
        // EGL spec: the first time a context is bound to a draw surface, viewport+scissor = surface size.
        // Mesa had already latched 0,0,0,0 (surfaceless first bind), so Telegram's Intro (which never calls
        // glViewport) rasterised into a degenerate viewport -> sparse striped noise.
        if (r && c != null && draw instanceof Surf && viewportInit.add(h(c))) {
            Surf o = (Surf) draw;
            try { android.opengl.GLES20.glViewport(0, 0, o.w, o.hh); android.opengl.GLES20.glScissor(0, 0, o.w, o.hh); } catch (Throwable ignore) {}
        }
        if (r && c != null && renderer == null) renderer = JetEGLNative.glString(0x1F01) + " | " + JetEGLNative.glString(0x1F02);
        return r;
    }
    @Override public boolean eglSwapBuffers(EGLDisplay d, EGLSurface s) {
        if (s instanceof Surf) {
            Surf o = (Surf) s;
            if (o.sink != null) {
                if (o.buf == null) o.buf = new int[o.w * o.hh];
                boolean okRead = JetEGLNative.readFrame(o.w, o.hh, o.buf);
                if (okRead) { o.sink.jetPostFrame(o.buf, o.w, o.hh); frames++; if (frames == 1) { firstFrameNanos = System.nanoTime(); System.err.println("[JETGL] first frame presented " + o.w + "x" + o.hh + " sink=" + o.sink.getClass().getName() + " renderer=" + renderer + " (Linux-only Mesa)"); } }
                else if (frames == 0 && Boolean.getBoolean("jet.texdebug")) System.err.println("[JETGL] readFrame failed " + o.w + "x" + o.hh);
            } else if (frames == 0 && Boolean.getBoolean("jet.texdebug")) System.err.println("[JETGL] swap with null sink");
        }
        return ok(JetEGLNative.swapBuffers(h(d), h(s)));
    }
    @Override public boolean eglCopyBuffers(EGLDisplay d, EGLSurface s, Object p) { return false; }
    @Override public boolean eglQuerySurface(EGLDisplay d, EGLSurface s, int attr, int[] v) {
        int r = JetEGLNative.querySurface(h(d), h(s), attr); if (!ok(r != -1)) return false; if (v != null && v.length > 0) v[0] = r; return true;
    }
    @Override public boolean eglQueryContext(EGLDisplay d, EGLContext c, int attr, int[] v) {
        int r = JetEGLNative.queryContext(h(d), h(c), attr); if (!ok(r != -1)) return false; if (v != null && v.length > 0) v[0] = r; return true;
    }
    @Override public EGLContext eglGetCurrentContext() { long x = JetEGLNative.currentContext(); return x == 0 ? EGL_NO_CONTEXT : obj(x, Ctx::new); }
    @Override public EGLDisplay eglGetCurrentDisplay() { long x = JetEGLNative.currentDisplay(); return x == 0 ? EGL_NO_DISPLAY : obj(x, Dpy::new); }
    @Override public EGLSurface eglGetCurrentSurface(int which) { long x = JetEGLNative.currentSurface(which); Object o = x == 0 ? null : objs.get(x); return o instanceof EGLSurface ? (EGLSurface) o : EGL_NO_SURFACE; }
    @Override public boolean eglWaitGL() { return JetEGLNative.waitGL(); }
    @Override public boolean eglWaitNative(int engine, Object bindTarget) { return true; }
}
