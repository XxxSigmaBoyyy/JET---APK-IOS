package jet.gfx;

/** Backend-neutral pixel store for android.graphics.Bitmap, window frames and SurfaceView buffers:
 *  row-major, stride == width, unpremultiplied ARGB_8888 ints (Android's Bitmap.getPixel format).
 *  No AWT. Backends adapt it: Skia uploads/reads back via Pixmap bytes (iOS: the same buffer backs an SkPixmap);
 *  the Linux-only Java2D reference backend wraps the very same int[] (zero copy) inside JetJava2DGfx.
 *  {@link #gen} increments on every CPU-side or backend write so backends can cache uploads. */
public final class PixelBuffer {
    public final int w, h; public final int[] px; public boolean opaque; public int gen = 1;
    /** Backend-private cache slot (e.g. the Java2D view). Never read outside the backend that set it. */
    public Object backendView;
    public PixelBuffer(int w, int h) { this.w = Math.max(1, w); this.h = Math.max(1, h); px = new int[this.w * this.h]; }
    public PixelBuffer(int w, int h, int[] argb) { this.w = w; this.h = h; if (argb.length < w * h) throw new IllegalArgumentException("short pixel array"); px = argb; }
    public int get(int x, int y) { return px[y * w + x]; }
    public void set(int x, int y, int c) { px[y * w + x] = c; gen++; }
    public void fill(int c) { java.util.Arrays.fill(px, c); gen++; }
    public void read(int[] out, int off, int stride, int x, int y, int rw, int rh) { for (int r = 0; r < rh; r++) System.arraycopy(px, (y + r) * w + x, out, off + r * stride, rw); }
    public void write(int[] in, int off, int stride, int x, int y, int rw, int rh) { for (int r = 0; r < rh; r++) System.arraycopy(in, off + r * stride, px, (y + r) * w + x, rw); gen++; }
    public PixelBuffer copy() { PixelBuffer o = new PixelBuffer(w, h, px.clone()); o.opaque = opaque; return o; }
    public PixelBuffer sub(int x, int y, int sw, int sh) { PixelBuffer o = new PixelBuffer(sw, sh); read(o.px, 0, sw, x, y, sw, sh); o.opaque = opaque; return o; }
    public boolean sameAs(PixelBuffer o) { return o != null && o.w == w && o.h == h && java.util.Arrays.equals(px, o.px); }
    /** Little-endian BGRA bytes == ARGB ints (the Skia BGRA_8888 unpremul layout). */
    public byte[] bgraBytes() {
        byte[] b = new byte[w * h * 4];
        for (int i = 0, j = 0; i < w * h; i++, j += 4) { int c = px[i]; b[j] = (byte) c; b[j + 1] = (byte) (c >>> 8); b[j + 2] = (byte) (c >>> 16); b[j + 3] = opaque ? (byte) 0xFF : (byte) (c >>> 24); }
        return b;
    }
    public void setFromBgraBytes(byte[] b) { int n = Math.min(w * h, b.length / 4); for (int i = 0, j = 0; i < n; i++, j += 4) px[i] = (b[j] & 0xFF) | (b[j + 1] & 0xFF) << 8 | (b[j + 2] & 0xFF) << 16 | (b[j + 3] & 0xFF) << 24; gen++; }
}
