package android.graphics;
public class LinearGradient extends Shader {
    final float x0, y0, x1, y1; final int[] colors; final float[] pos; final TileMode tile;
    public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, TileMode t) { this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; this.colors = colors; this.pos = positions; tile = t; }
    public LinearGradient(float x0, float y0, float x1, float y1, int c0, int c1, TileMode t) { this(x0, y0, x1, y1, new int[] { c0, c1 }, null, t); }
    @Override public java.awt.Paint jetAwt() {
        if (colors == null || colors.length < 2 || (x0 == x1 && y0 == y1)) return null;
        float[] f = Grad.fractions(pos, colors.length); java.awt.Color[] c = Grad.awt(colors);
        return new java.awt.LinearGradientPaint(x0, y0, x1, y1, f, c, tile == TileMode.REPEAT ? java.awt.MultipleGradientPaint.CycleMethod.REPEAT : tile == TileMode.MIRROR ? java.awt.MultipleGradientPaint.CycleMethod.REFLECT : java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}
