package android.graphics;
public class Shader {
    public enum TileMode { CLAMP, REPEAT, MIRROR, DECAL }
    protected Matrix mLocal; public Shader() {}
    public void setLocalMatrix(Matrix m) { mLocal = m; } public boolean getLocalMatrix(Matrix m) { if (mLocal == null) return false; if (m != null) m.set(mLocal); return true; }
    /** Java2D paint for this shader, or null to fall back to the paint colour. */
    public java.awt.Paint jetAwt() { return null; }
}
