package android.graphics;
/**
 * Backend selection: -Djet.gfx=java2d (default on iOS Zero).
 * Skia/Skija path lives in JetSkiaGfx but is not compiled into the iOS stage
 * until a Metal/CoreGraphics Skia build is wired; use JetJava2DGfx (java.desktop).
 */
public final class JetGfx {
    private JetGfx() {}
    public static final String BACKEND;
    static {
        String b = System.getProperty("jet.gfx", "java2d");
        if (!"java2d".equals(b) && !"skia".equals(b)) b = "java2d";
        // Skia deferred: keep property for future, but never resolve JetSkiaGfx here.
        if ("skia".equals(b)) {
            System.err.println("[jet-gfx] skia not packaged on iOS stage yet; using java2d");
            b = "java2d";
        }
        BACKEND = b;
    }
    static boolean skia() { return false; }
    public static jet.gfx.Gfx forFrame(jet.gfx.PixelBuffer img) { return new JetJava2DGfx(img, false); }
    public static jet.gfx.Gfx forBitmap(jet.gfx.PixelBuffer img) { return new JetJava2DGfx(img, true); }
    public static jet.gfx.TextMetrics metrics(int style, int size) { return JetJava2DGfx.metrics(style, size); }
    static int style(Paint p) {
        int s = 0; if (p.isFakeBoldText() || (p.getTypeface() != null && p.getTypeface().isBold())) s |= jet.gfx.Gfx.STYLE_BOLD;
        if (p.getTypeface() != null && p.getTypeface().isItalic()) s |= jet.gfx.Gfx.STYLE_ITALIC; return s;
    }
    static int size(Paint p) { return Math.max(1, Math.round(p.getTextSize())); }
    static PorterDuffColorFilter asTintFilter(ColorFilter cf) {
        if (cf instanceof PorterDuffColorFilter) return (PorterDuffColorFilter) cf;
        if (cf instanceof BlendModeColorFilter) {
            BlendModeColorFilter b = (BlendModeColorFilter) cf;
            return new PorterDuffColorFilter(b.getColor(), b.jetPorterDuffMode());
        }
        return null;
    }
    static int effectiveColor(Paint p) {
        int c = p.getColor(); float[] cm = colorMatrixOf(p.getColorFilter());
        if (cm != null) return ColorMatrix.apply(cm, c);
        PorterDuffColorFilter f = asTintFilter(p.getColorFilter());
        if (f == null) return c;
        PorterDuff.Mode m = f.getMode();
        if (m == PorterDuff.Mode.SRC_IN || m == PorterDuff.Mode.SRC_ATOP || m == PorterDuff.Mode.SRC) c = (f.getColor() & 0xFFFFFF) | ((((f.getColor() >>> 24) * (c >>> 24)) / 255) << 24);
        else if (m == PorterDuff.Mode.MULTIPLY) c = Color.argb(Color.alpha(c) * Color.alpha(f.getColor()) / 255, Color.red(c) * Color.red(f.getColor()) / 255, Color.green(c) * Color.green(f.getColor()) / 255, Color.blue(c) * Color.blue(f.getColor()) / 255);
        return c;
    }
    static float[] colorMatrixOf(ColorFilter cf) {
        if (cf instanceof ColorMatrixColorFilter) return ((ColorMatrixColorFilter) cf).jetArray();
        if (cf instanceof LightingColorFilter) return ((LightingColorFilter) cf).jetArray();
        return null;
    }
    static jet.gfx.PixelBuffer tint(jet.gfx.PixelBuffer src, ColorFilter cf) {
        float[] cm = colorMatrixOf(cf);
        if (cm != null) {
            jet.gfx.PixelBuffer o = new jet.gfx.PixelBuffer(src.w, src.h);
            for (int i = 0; i < src.px.length; i++) o.px[i] = ColorMatrix.apply(cm, src.opaque ? src.px[i] | 0xFF000000 : src.px[i]);
            return o;
        }
        PorterDuffColorFilter f = asTintFilter(cf);
        if (f == null) return src;
        PorterDuff.Mode m = f.getMode(); if (m != PorterDuff.Mode.SRC_IN && m != PorterDuff.Mode.SRC_ATOP) return src;
        jet.gfx.PixelBuffer o = new jet.gfx.PixelBuffer(src.w, src.h); int c = f.getColor(), ca = c >>> 24;
        for (int i = 0; i < src.px.length; i++) { int a = src.opaque ? 255 : src.px[i] >>> 24; o.px[i] = ((a * ca / 255) << 24) | (c & 0xFFFFFF); }
        return o;
    }
    static jet.gfx.PixelBuffer tint(jet.gfx.PixelBuffer src, PorterDuffColorFilter f) { return tint(src, (ColorFilter) f); }
}
