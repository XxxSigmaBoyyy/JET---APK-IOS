package jet.gl;
/** JNI surface of native/jetgl/jetgl.c (Mesa surfaceless EGL). Linux-only. */
public final class JetEGLNative {
    static { JetGL.load(); }
    public static native long getDisplay();
    public static native boolean initialize(long d, int[] ver);
    public static native boolean terminate(long d);
    public static native int getError();
    public static native String queryString(long d, int name);
    public static native int chooseConfig(long d, int[] attrs, long[] out);
    public static native int getConfigAttrib(long d, long cfg, int attr);
    public static native long createContext(long d, long cfg, long share, int[] attrs);
    public static native boolean destroyContext(long d, long ctx);
    public static native long createPbuffer(long d, long cfg, int w, int h);
    public static native boolean destroySurface(long d, long s);
    public static native boolean makeCurrent(long d, long draw, long read, long ctx);
    public static native boolean swapBuffers(long d, long s);
    public static native int querySurface(long d, long s, int attr);
    public static native int queryContext(long d, long ctx, int attr);
    public static native long currentContext();
    public static native long currentSurface(int which);
    public static native long currentDisplay();
    public static native boolean waitGL();
    public static native boolean readFrame(int w, int h, int[] out);
    public static native String glString(int name);
}
