package android.graphics;
public class SweepGradient extends Shader {
    final int[] colors;
    public SweepGradient(float cx, float cy, int[] colors, float[] pos) { this.colors = colors; }
    public SweepGradient(float cx, float cy, int c0, int c1) { this(cx, cy, new int[] { c0, c1 }, null); }
}
