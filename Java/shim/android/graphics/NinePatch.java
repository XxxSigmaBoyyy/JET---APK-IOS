package android.graphics;
/** AOSP NinePatch: bitmap + serialized Res_png_9patch chunk (drawing is done by NinePatchDrawable). */
public class NinePatch {
    private final Bitmap mBitmap; private final byte[] mChunk; private final String mName; private Paint mPaint;
    public NinePatch(Bitmap bitmap, byte[] chunk) { this(bitmap, chunk, null); }
    public NinePatch(Bitmap bitmap, byte[] chunk, String srcName) { mBitmap = bitmap; mChunk = chunk; mName = srcName; }
    public static boolean isNinePatchChunk(byte[] chunk) { return chunk != null && chunk.length >= 32 && chunk[0] != -1; }
    public Bitmap getBitmap() { return mBitmap; }
    public String getName() { return mName; }
    public Paint getPaint() { return mPaint; } public void setPaint(Paint p) { mPaint = p; }
    public int getWidth() { return mBitmap == null ? 0 : mBitmap.getWidth(); }
    public int getHeight() { return mBitmap == null ? 0 : mBitmap.getHeight(); }
    public int getDensity() { return mBitmap == null ? 160 : mBitmap.getDensity(); }
    public boolean hasAlpha() { return mBitmap == null || mBitmap.hasAlpha(); }
    public final Region getTransparentRegion(Rect bounds) { return null; }
    public byte[] jetChunk() { return mChunk; }
    public void draw(Canvas c, RectF loc) { draw(c, loc, mPaint); }
    public void draw(Canvas c, Rect loc) { draw(c, new RectF(loc), mPaint); }
    public void draw(Canvas c, Rect loc, Paint p) { draw(c, new RectF(loc), p); }
    private void draw(Canvas c, RectF loc, Paint p) {
        android.graphics.drawable.NinePatchDrawable d = new android.graphics.drawable.NinePatchDrawable(this);
        d.jetDraw(c, Math.round(loc.left), Math.round(loc.top), Math.round(loc.right), Math.round(loc.bottom), 0);
    }
}
