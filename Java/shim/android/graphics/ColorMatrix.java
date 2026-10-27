package android.graphics;
/** AOSP ColorMatrix: 4x5 row-major matrix applied to unpremultiplied RGBA (translation in 0..255 units). */
public class ColorMatrix {
    private final float[] mArray = new float[20];
    public ColorMatrix() { reset(); }
    public ColorMatrix(float[] src) { System.arraycopy(src, 0, mArray, 0, 20); }
    public ColorMatrix(ColorMatrix src) { System.arraycopy(src.mArray, 0, mArray, 0, 20); }
    public final float[] getArray() { return mArray; }
    public void reset() { java.util.Arrays.fill(mArray, 0); mArray[0] = mArray[6] = mArray[12] = mArray[18] = 1; }
    public void set(ColorMatrix src) { System.arraycopy(src.mArray, 0, mArray, 0, 20); }
    public void set(float[] src) { System.arraycopy(src, 0, mArray, 0, 20); }
    public void setScale(float r, float g, float b, float a) { java.util.Arrays.fill(mArray, 0); mArray[0] = r; mArray[6] = g; mArray[12] = b; mArray[18] = a; }
    public void setRotate(int axis, float degrees) {
        reset(); double rad = degrees * Math.PI / 180d; float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        switch (axis) {
            case 0: mArray[6] = mArray[12] = c; mArray[7] = s; mArray[11] = -s; break;
            case 1: mArray[0] = mArray[12] = c; mArray[2] = -s; mArray[10] = s; break;
            case 2: mArray[0] = mArray[6] = c; mArray[1] = s; mArray[5] = -s; break;
            default: throw new RuntimeException();
        }
    }
    public void setConcat(ColorMatrix a, ColorMatrix b) {
        float[] tmp = (a == this || b == this) ? new float[20] : mArray, x = a.mArray, y = b.mArray; int i = 0;
        for (int j = 0; j < 20; j += 5) {
            for (int k = 0; k < 4; k++) tmp[i++] = x[j] * y[k] + x[j + 1] * y[k + 5] + x[j + 2] * y[k + 10] + x[j + 3] * y[k + 15];
            tmp[i++] = x[j] * y[4] + x[j + 1] * y[9] + x[j + 2] * y[14] + x[j + 3] * y[19] + x[j + 4];
        }
        if (tmp != mArray) System.arraycopy(tmp, 0, mArray, 0, 20);
    }
    public void preConcat(ColorMatrix pre) { setConcat(this, pre); }
    public void postConcat(ColorMatrix post) { setConcat(post, this); }
    public void setSaturation(float sat) {
        reset(); float[] m = mArray; float invSat = 1 - sat, R = 0.213f * invSat, G = 0.715f * invSat, B = 0.072f * invSat;
        m[0] = R + sat; m[1] = G; m[2] = B; m[5] = R; m[6] = G + sat; m[7] = B; m[10] = R; m[11] = G; m[12] = B + sat;
    }
    public void setRGB2YUV() { reset(); float[] m = mArray; m[0] = .299f; m[1] = .587f; m[2] = .114f; m[5] = -.16874f; m[6] = -.33126f; m[7] = .5f; m[10] = .5f; m[11] = -.41869f; m[12] = -.08131f; }
    public void setYUV2RGB() { reset(); float[] m = mArray; m[2] = 1.402f; m[5] = 1; m[6] = -.34414f; m[7] = -.71414f; m[10] = 1; m[11] = 1.772f; m[12] = 0; }
    @Override public boolean equals(Object o) { return o instanceof ColorMatrix && java.util.Arrays.equals(mArray, ((ColorMatrix) o).mArray); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(mArray); }
    /** Apply to one unpremultiplied ARGB colour. */
    static int apply(float[] m, int c) {
        float r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255, a = c >>> 24;
        int R = cl(m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4]), G = cl(m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9]);
        int B = cl(m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14]), A = cl(m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19]);
        return (A << 24) | (R << 16) | (G << 8) | B;
    }
    private static int cl(float v) { return v <= 0 ? 0 : v >= 255 ? 255 : (int) (v + 0.5f); }
}
