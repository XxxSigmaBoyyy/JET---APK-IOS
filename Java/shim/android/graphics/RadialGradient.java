package android.graphics;
public class RadialGradient extends Shader {
    final float cx, cy, r; final int[] colors; final float[] pos;
    public RadialGradient(float cx, float cy, float r, int[] colors, float[] stops, TileMode t) { this.cx = cx; this.cy = cy; this.r = r; this.colors = colors; this.pos = stops; }
    public RadialGradient(float cx, float cy, float r, int c0, int c1, TileMode t) { this(cx, cy, r, new int[] { c0, c1 }, null, t); }
    @Override public java.awt.Paint jetAwt() { if (colors == null || colors.length < 2 || r <= 0) return null; return new java.awt.RadialGradientPaint(cx, cy, r, Grad.fractions(pos, colors.length), Grad.awt(colors)); }
}
