package android.graphics;
public class BitmapShader extends Shader { final Bitmap bitmap; public BitmapShader(Bitmap b, TileMode x, TileMode y) { bitmap = b; }
    public static final int FILTER_MODE_DEFAULT = 0, FILTER_MODE_NEAREST = 1, FILTER_MODE_LINEAR = 2;
    private int mFilterMode = FILTER_MODE_DEFAULT; private int mMaxAniso;
    public int getFilterMode() { return mFilterMode; }
    public void setFilterMode(int mode) { if (mode < 0 || mode > 2) throw new IllegalArgumentException("Invalid filter mode " + mode); mFilterMode = mode; mMaxAniso = 0; }
    public int getMaxAnisotropy() { return mMaxAniso; }
    public void setMaxAnisotropy(int a) { if (a < 0) throw new IllegalArgumentException("maxAnisotropy must be > 0"); mMaxAniso = a; }
}
