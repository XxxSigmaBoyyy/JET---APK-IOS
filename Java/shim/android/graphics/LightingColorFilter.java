package android.graphics;
public class LightingColorFilter extends ColorFilter {
    private final int mMul, mAdd;
    public LightingColorFilter(int mul, int add) { mMul = mul; mAdd = add; }
    public int getColorMultiply() { return mMul; }
    public int getColorAdd() { return mAdd; }
    float[] jetArray() { float[] m = new float[20]; m[0] = ((mMul >> 16) & 255) / 255f; m[6] = ((mMul >> 8) & 255) / 255f; m[12] = (mMul & 255) / 255f; m[18] = 1;
        m[4] = (mAdd >> 16) & 255; m[9] = (mAdd >> 8) & 255; m[14] = mAdd & 255; return m; }
}
