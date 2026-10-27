package android.graphics;
public class ColorMatrixColorFilter extends ColorFilter {
    private final ColorMatrix mMatrix = new ColorMatrix();
    public ColorMatrixColorFilter(ColorMatrix matrix) { mMatrix.set(matrix); }
    public ColorMatrixColorFilter(float[] array) { if (array.length < 20) throw new ArrayIndexOutOfBoundsException(); mMatrix.set(array); }
    public void getColorMatrix(ColorMatrix out) { out.set(mMatrix); }
    float[] jetArray() { return mMatrix.getArray(); }
}
