package android.graphics;
public final class Bitmap {
    public enum Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE, RGBA_1010102 }
    public enum CompressFormat { JPEG, PNG, WEBP, WEBP_LOSSY, WEBP_LOSSLESS }
    public static final int DENSITY_NONE = 0;
    /** Encode through Skia's encoders (PNG / JPEG / WebP) — no ImageIO. */
    public boolean compress(CompressFormat f, int quality, java.io.OutputStream out) {
        try { byte[] b = JetImageIOCodec.encode(pb, f.name(), quality); if (b == null) return false; out.write(b); return true; } catch (java.io.IOException e) { return false; }
    }
    /** Backend-neutral pixel store (unpremultiplied ARGB ints). Backends adapt it; no AWT here. */
    /** Not final: reconfigure() swaps in a new store (AOSP keeps the allocation; contents are undefined after a resize). */
    public jet.gfx.PixelBuffer pb;
    private int mAllocBytes = -1;
    private boolean mutable;
    private int mDensity = 320; private boolean mHasAlpha = true, mPremul = true, mHasMipMap;
    Bitmap(jet.gfx.PixelBuffer pb, boolean mutable) { this.pb = pb; this.mutable = mutable; }
    /** Shim-internal: wrap an existing pixel buffer. */
    public static Bitmap jetWrap(jet.gfx.PixelBuffer pb, boolean mutable) { return new Bitmap(pb, mutable); }
    /** Backend-neutral construction from unpremultiplied ARGB pixels (decoder output). */
    public static Bitmap jetFromArgb(int[] argb, int w, int h, boolean mutable) { return new Bitmap(new jet.gfx.PixelBuffer(w, h, argb), mutable); }
    public static Bitmap createBitmap(android.util.DisplayMetrics dm, int w, int h, Config c) { Bitmap b = createBitmap(w, h, c); if (dm != null) b.mDensity = dm.densityDpi; return b; }
    public static Bitmap createBitmap(int w, int h, Config c, boolean hasAlpha) { Bitmap b = createBitmap(w, h, c); b.mHasAlpha = hasAlpha; return b; }
    /** API 26+ overloads. Host bitmaps are sRGB; the ColorSpace argument is accepted and not dereferenced (type stays auto-stubbed). */
    public static Bitmap createBitmap(android.util.DisplayMetrics dm, int w, int h, Config c, boolean hasAlpha) { Bitmap b = createBitmap(dm, w, h, c); b.mHasAlpha = hasAlpha; return b; }
    public static Bitmap createBitmap(android.util.DisplayMetrics dm, int w, int h, Config c, boolean hasAlpha, android.graphics.ColorSpace cs) { return createBitmap(dm, w, h, c, hasAlpha); }
    public static Bitmap createBitmap(int w, int h, Config c, boolean hasAlpha, android.graphics.ColorSpace cs) { return createBitmap(w, h, c, hasAlpha); }
    public static Bitmap createBitmap(int[] colors, int w, int h, Config c) { return createBitmap(colors, 0, w, w, h, c); }
    public static Bitmap createBitmap(int[] colors, int off, int stride, int w, int h, Config c) { Bitmap b = createBitmap(w, h, c); b.pb.write(colors, off, stride, 0, 0, w, h); b.mutable = false; return b; }
    public static Bitmap createBitmap(Bitmap src, int x, int y, int w, int h, android.graphics.Matrix m, boolean filter) {
        Bitmap sub = createBitmap(src, x, y, w, h);
        if (m == null || m.isIdentity()) return sub;
        RectF r = new RectF(0, 0, w, h); m.mapRect(r);
        int nw = Math.max(1, Math.round(r.width())), nh = Math.max(1, Math.round(r.height()));
        Bitmap out = createBitmap(nw, nh, Config.ARGB_8888);
        Canvas c = new Canvas(out); c.translate(-r.left, -r.top); c.drawBitmap(sub, m, null);
        return out;
    }
    public int getDensity() { return mDensity; }
    public void setDensity(int d) { mDensity = d; }
    public int getScaledWidth(int target) { return mDensity == 0 || target == 0 ? getWidth() : (getWidth() * target + mDensity / 2) / mDensity; }
    public int getScaledHeight(int target) { return mDensity == 0 || target == 0 ? getHeight() : (getHeight() * target + mDensity / 2) / mDensity; }
    public int getScaledWidth(android.util.DisplayMetrics m) { return getScaledWidth(m.densityDpi); }
    public int getScaledHeight(android.util.DisplayMetrics m) { return getScaledHeight(m.densityDpi); }
    public int getScaledWidth(Canvas c) { return getWidth(); }
    public int getScaledHeight(Canvas c) { return getHeight(); }
    public int getRowBytes() { return getWidth() * 4; }
    public int getByteCount() { return getRowBytes() * getHeight(); }
    public int getAllocationByteCount() { return Math.max(mAllocBytes, getByteCount()); }
    public boolean hasAlpha() { return mHasAlpha; }
    public void setHasAlpha(boolean a) { mHasAlpha = a; pb.opaque = !a && pb.opaque; }
    public boolean isPremultiplied() { return mPremul; }
    public void setPremultiplied(boolean p) { mPremul = p; }
    public boolean hasMipMap() { return mHasMipMap; }
    public void setHasMipMap(boolean m) { mHasMipMap = m; }
    public int getGenerationId() { return pb.gen; }
    public void prepareToDraw() {}
    public void setConfig(Config c) {}
    /** AOSP: legal when w*h*4 <= getAllocationByteCount() and the bitmap is mutable (Glide/Fresco bitmap pools reuse larger bitmaps). */
    public void reconfigure(int w, int h, Config c) {
        if (w == getWidth() && h == getHeight()) return;
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be > 0");
        if (!mutable) throw new IllegalStateException("only mutable bitmaps may be reconfigured");
        if ((long) w * h * 4 > getAllocationByteCount()) throw new IllegalArgumentException("Bitmap not large enough to support new configuration");
        int alloc = getAllocationByteCount();
        pb = new jet.gfx.PixelBuffer(w, h); mAllocBytes = alloc;
    }
    public Bitmap copy(Config c, boolean isMutable) { Bitmap b = new Bitmap(pb.copy(), isMutable); b.mDensity = mDensity; b.mHasAlpha = mHasAlpha; return b; }
    public Bitmap extractAlpha() { jet.gfx.PixelBuffer o = pb.copy(); for (int i = 0; i < o.px.length; i++) o.px[i] &= 0xFF000000; return new Bitmap(o, true); }
    public void getPixels(int[] px, int off, int stride, int x, int y, int w, int h) { pb.read(px, off, stride, x, y, w, h); }
    public void setPixels(int[] px, int off, int stride, int x, int y, int w, int h) { pb.write(px, off, stride, x, y, w, h); }
    public void copyPixelsToBuffer(java.nio.Buffer dst) { if (dst instanceof java.nio.IntBuffer) ((java.nio.IntBuffer) dst).put(pb.px); else if (dst instanceof java.nio.ByteBuffer) { java.nio.ByteBuffer bb = (java.nio.ByteBuffer) dst; for (int c : pb.px) { bb.put((byte) (c >>> 16)); bb.put((byte) (c >>> 8)); bb.put((byte) c); bb.put((byte) (c >>> 24)); } } }
    public void copyPixelsFromBuffer(java.nio.Buffer src) { if (src instanceof java.nio.IntBuffer) ((java.nio.IntBuffer) src).get(pb.px); else if (src instanceof java.nio.ByteBuffer) { java.nio.ByteBuffer bb = (java.nio.ByteBuffer) src; for (int i = 0; i < pb.px.length && bb.remaining() >= 4; i++) { int r = bb.get() & 0xFF, g = bb.get() & 0xFF, b = bb.get() & 0xFF, a = bb.get() & 0xFF; pb.px[i] = a << 24 | r << 16 | g << 8 | b; } } pb.gen++; }
    public boolean sameAs(Bitmap o) { return o != null && pb.sameAs(o.pb); }
    public void eraseColor(long c) { eraseColor((int) c); }
    public static Bitmap createBitmap(int w, int h, Config c) { return new Bitmap(new jet.gfx.PixelBuffer(w, h), true); }
    public static Bitmap createBitmap(Bitmap src) { return createBitmap(src, 0, 0, src.getWidth(), src.getHeight()); }
    public static Bitmap createBitmap(Bitmap src, int x, int y, int w, int h) { Bitmap b = new Bitmap(src.pb.sub(x, y, w, h), false); b.mDensity = src.mDensity; return b; }
    /** Resampled through the active Gfx backend (Skia linear / Java2D bilinear), not AWT image ops. */
    public static Bitmap createScaledBitmap(Bitmap src, int w, int h, boolean filter) {
        Bitmap out = new Bitmap(new jet.gfx.PixelBuffer(w, h), true); out.mDensity = src.mDensity;
        jet.gfx.Gfx g = JetGfx.forBitmap(out.pb);
        Paint p = new Paint(); p.setFilterBitmap(filter);
        g.drawImage(src.pb, java.awt.geom.AffineTransform.getScaleInstance(w / (double) src.getWidth(), h / (double) src.getHeight()), p);
        g.flush(); g.dispose(); out.mutable = false;
        return out;
    }
    public int getWidth() { return pb.w; }
    public int getHeight() { return pb.h; }
    public boolean isMutable() { return mutable; }
    public void recycle() {}
    public boolean isRecycled() { return false; }
    public int getPixel(int x, int y) { return pb.get(x, y); }
    public void setPixel(int x, int y, int color) { pb.set(x, y, color); }
    public void eraseColor(int c) { pb.fill(c); }
    public Config getConfig() { return Config.ARGB_8888; }
    @Override public String toString() { return "Bitmap(" + getWidth() + "x" + getHeight() + ")"; }
    private ColorSpace jetColorSpace;
    public ColorSpace getColorSpace() { return jetColorSpace != null ? jetColorSpace : ColorSpace.get(ColorSpace.Named.SRGB); }
    public void setColorSpace(ColorSpace cs) { if (cs == null) throw new IllegalArgumentException("The colorSpace cannot be set to null"); if (cs.getModel() != ColorSpace.Model.RGB) throw new IllegalArgumentException("A Bitmap can only be associated with an RGB color space"); jetColorSpace = cs; }
}
