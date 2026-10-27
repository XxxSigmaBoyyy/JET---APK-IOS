package javax.microedition.khronos.egl;
public abstract class EGLContext {
    public EGLContext() {}
    public static EGL getEGL() { return jet.gl.JetEGL10.get(); }
    public abstract javax.microedition.khronos.opengles.GL getGL();
}
