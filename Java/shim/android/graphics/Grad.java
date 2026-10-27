package android.graphics;
final class Grad {
    static float[] fractions(float[] pos, int n) {
        float[] f = new float[n];
        for (int i = 0; i < n; i++) { f[i] = pos != null && i < pos.length ? pos[i] : (n == 1 ? 0 : i / (float) (n - 1)); if (i > 0 && f[i] <= f[i - 1]) f[i] = Math.min(1f, f[i - 1] + 1e-5f); }
        return f;
    }
    static java.awt.Color[] awt(int[] c) { java.awt.Color[] r = new java.awt.Color[c.length]; for (int i = 0; i < c.length; i++) r[i] = new java.awt.Color(c[i], true); return r; }
}
