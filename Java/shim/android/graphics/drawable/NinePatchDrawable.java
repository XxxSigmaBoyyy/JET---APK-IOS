package android.graphics.drawable;
import android.graphics.*;
/**
 * AOSP NinePatchDrawable over a *compiled* 9-patch PNG (aapt strips the 1px border and stores Res_png_9patch in an
 * "npTc" chunk, big-endian). Stretch regions scale, fixed regions keep their size (scaled by targetDensity/density);
 * getPadding() returns the patch padding, which View applies like Android when the view sets no padding itself.
 */
public class NinePatchDrawable extends Drawable {
    private final Bitmap mBitmap; private final int[] mXDivs, mYDivs; private final Rect mPad; private final float mScale;
    private final NinePatch mNinePatch;
    public NinePatchDrawable(Bitmap bitmap, byte[] chunk, Rect padding, String srcName) { this(null, bitmap, chunk, padding, srcName); }
    public NinePatchDrawable(android.content.res.Resources res, Bitmap bitmap, byte[] chunk, Rect padding, String srcName) {
        mBitmap = bitmap; int[][] dv = parseChunk(chunk); mXDivs = dv[0]; mYDivs = dv[1];
        mPad = padding != null ? new Rect(padding) : dv[2] != null ? new Rect(dv[2][0], dv[2][2], dv[2][1], dv[2][3]) : new Rect();
        mScale = 1f; mNinePatch = new NinePatch(bitmap, chunk, srcName);
    }
    public NinePatchDrawable(NinePatch patch) { this(patch == null ? null : patch.getBitmap(), patch == null ? null : patch.jetChunk(), null, null); }
    public NinePatchDrawable(android.content.res.Resources res, NinePatch patch) { this(patch); }
    private NinePatchDrawable(Bitmap b, int[] xd, int[] yd, Rect pad, float scale) { mBitmap = b; mXDivs = xd; mYDivs = yd; mPad = pad; mScale = scale; mNinePatch = null; }

    /** Load a compiled .9.png; density = bitmap's resource density, target = display density. Falls back to a plain BitmapDrawable without npTc. */
    public static Drawable fromCompiledPng(java.io.File f, int density, int target) throws java.io.IOException {
        byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
        byte[] chunk = findChunk(data, "npTc");
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inScaled = false;
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (bm == null) return null;
        float s = density > 0 ? target / (float) density : 1f;
        if (chunk == null) { BitmapDrawable d = new BitmapDrawable(bm); d.setTargetDensity(Math.round(160 * s)); return d; }
        int[][] dv = parseChunk(chunk);
        Rect pad = dv[2] == null ? new Rect() : new Rect(Math.round(dv[2][0] * s), Math.round(dv[2][2] * s), Math.round(dv[2][1] * s), Math.round(dv[2][3] * s));
        return new NinePatchDrawable(bm, dv[0], dv[1], pad, s);
    }
    static byte[] findChunk(byte[] d, String type) {
        int p = 8;
        while (p + 8 <= d.length) {
            int len = ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
            String t = new String(d, p + 4, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (len < 0 || p + 12 + len > d.length) return null;
            if (t.equals(type)) return java.util.Arrays.copyOfRange(d, p + 8, p + 8 + len);
            if (t.equals("IEND")) return null;
            p += 12 + len;
        }
        return null;
    }
    /** Res_png_9patch: i8 wasDeserialized,numXDivs,numYDivs,numColors; u32 xOff,yOff; i32 padL,padR,padT,padB; u32 colorsOff; i32 xDivs[], yDivs[]. */
    static int[][] parseChunk(byte[] c) {
        if (c == null || c.length < 32) return new int[][] { new int[0], new int[0], null };
        java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(c).order(java.nio.ByteOrder.BIG_ENDIAN);
        b.get(); int nx = b.get() & 0xff, ny = b.get() & 0xff; b.get(); b.getInt(); b.getInt();
        int[] pad = { b.getInt(), b.getInt(), b.getInt(), b.getInt() }; b.getInt();
        int[] xd = new int[nx], yd = new int[ny];
        for (int i = 0; i < nx && b.remaining() >= 4; i++) xd[i] = b.getInt();
        for (int i = 0; i < ny && b.remaining() >= 4; i++) yd[i] = b.getInt();
        return new int[][] { xd, yd, pad };
    }
    /** Segment boundaries in source px and destination px along one axis. */
    private static int[][] segments(int[] divs, int srcLen, int dstLen, float scale) {
        java.util.ArrayList<int[]> segs = new java.util.ArrayList<>(); // {srcStart, srcEnd, stretch}
        int pos = 0;
        for (int i = 0; i + 1 < divs.length; i += 2) {
            int a = Math.max(0, Math.min(srcLen, divs[i])), e = Math.max(a, Math.min(srcLen, divs[i + 1]));
            if (a > pos) segs.add(new int[] { pos, a, 0 });
            if (e > a) segs.add(new int[] { a, e, 1 });
            pos = e;
        }
        if (pos < srcLen) segs.add(new int[] { pos, srcLen, 0 });
        float fixed = 0, stretch = 0;
        for (int[] s : segs) if (s[2] == 0) fixed += (s[1] - s[0]) * scale; else stretch += s[1] - s[0];
        float fixScale = fixed > dstLen && fixed > 0 ? dstLen / fixed : 1f;
        float avail = Math.max(0, dstLen - fixed * fixScale);
        if (stretch == 0 && fixed > 0) fixScale = dstLen / fixed;
        int[][] out = new int[segs.size()][]; float d = 0;
        for (int i = 0; i < segs.size(); i++) {
            int[] s = segs.get(i); float len = s[2] == 0 ? (s[1] - s[0]) * scale * fixScale : avail * (s[1] - s[0]) / stretch;
            int d0 = Math.round(d); d += len; int d1 = i == segs.size() - 1 ? dstLen : Math.round(d);
            out[i] = new int[] { s[0], s[1], d0, d1 };
        }
        return out;
    }
    @Override public void jetDraw(Canvas c, int l, int t, int r, int b, int state) {
        if (mBitmap == null || r <= l || b <= t) return;
        int[][] xs = segments(mXDivs, mBitmap.getWidth(), r - l, mScale), ys = segments(mYDivs, mBitmap.getHeight(), b - t, mScale);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG); p.setAlpha(Math.max(0, Math.min(255, getAlpha()))); if (mColorFilter != null) p.setColorFilter(mColorFilter);
        Rect src = new Rect(); RectF dst = new RectF();
        for (int[] y : ys) for (int[] x : xs) {
            if (x[3] <= x[2] || y[3] <= y[2] || x[1] <= x[0] || y[1] <= y[0]) continue;
            src.set(x[0], y[0], x[1], y[1]); dst.set(l + x[2], t + y[2], l + x[3], t + y[3]);
            c.drawBitmap(mBitmap, src, dst, p);
        }
    }
    @Override public int getIntrinsicWidth() { return mBitmap == null ? -1 : Math.round(mBitmap.getWidth() * mScale); }
    @Override public int getIntrinsicHeight() { return mBitmap == null ? -1 : Math.round(mBitmap.getHeight() * mScale); }
    @Override public int getMinimumWidth() { return getIntrinsicWidth(); }
    @Override public int getMinimumHeight() { return getIntrinsicHeight(); }
    @Override public boolean getPadding(Rect p) { p.set(mPad); return (mPad.left | mPad.top | mPad.right | mPad.bottom) != 0; }
    @Override public int getOpacity() { return -3; }
    public Paint getPaint() { return new Paint(); }
    public void setTargetDensity(int d) {} public void setTargetDensity(android.util.DisplayMetrics m) {} public void setTargetDensity(Canvas c) {}
    @Override public ConstantState getConstantState() {
        final NinePatchDrawable me = this;
        return new ConstantState() { public Drawable newDrawable() { return new NinePatchDrawable(me.mBitmap, me.mXDivs, me.mYDivs, new Rect(me.mPad), me.mScale); } };
    }
}
