package android.graphics;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;

/** Image decoding through Skia's codecs (PNG/JPEG/WebP/GIF/BMP/ICO/WBMP), the same decoder family Android itself uses and the one
 *  the iOS backend links. Pure decode + resample; returns unpremultiplied ARGB ints so the result is independent of the pixel store.
 *  Returns null when Skia cannot decode (no other decoder is used). */
public final class JetSkiaCodec {
    private JetSkiaCodec() {}
    private static volatile boolean sBroken;
    static { jet.gfx.SkiaInit.ensure(); }
    /** {width, height, hasAlpha(0/1)} or null. */
    static synchronized int[] bounds(byte[] b) {
        if (sBroken || b == null) return null;
        try (Data d = Data.makeFromBytes(b); Codec c = Codec.makeFromData(d)) {
            ImageInfo ii = c.getImageInfo();
            return new int[]{ ii.getWidth(), ii.getHeight(), ii.getColorAlphaType() == ColorAlphaType.OPAQUE ? 0 : 1 };
        } catch (Throwable t) { if (t instanceof LinkageError) sBroken = true; return null; }
    }
    /** Decode and resample to dw x dh. out[0] = ARGB pixels (row-major); out[1] = {hasAlpha}. */
    static synchronized Object[] decode(byte[] b, int dw, int dh) {
        if (sBroken || b == null) return null;
        try (Data d = Data.makeFromBytes(b); Codec c = Codec.makeFromData(d)) {
            ImageInfo ci = c.getImageInfo();
            int w = ci.getWidth(), h = ci.getHeight();
            boolean alpha = ci.getColorAlphaType() != ColorAlphaType.OPAQUE;
            ImageInfo full = new ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL);
            try (io.github.humbleui.skija.Bitmap src = new io.github.humbleui.skija.Bitmap()) {
                if (!src.allocPixels(full)) return null;
                c.readPixels(src);
                byte[] px;
                if (dw == w && dh == h) px = src.readPixels();
                else {
                    ImageInfo di = new ImageInfo(dw, dh, ColorType.BGRA_8888, ColorAlphaType.PREMUL);
                    try (io.github.humbleui.skija.Bitmap dst = new io.github.humbleui.skija.Bitmap();
                         Image im = Image.makeRasterFromBitmap(src)) {
                        dst.allocPixels(di);
                        try (io.github.humbleui.skija.Canvas cv = new io.github.humbleui.skija.Canvas(dst)) {
                            cv.drawImageRect(im, Rect.makeWH(w, h), Rect.makeWH(dw, dh), SamplingMode.LINEAR, null, true);
                        }
                        try (io.github.humbleui.skija.Bitmap un = new io.github.humbleui.skija.Bitmap()) {
                            un.allocPixels(new ImageInfo(dw, dh, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL));
                            try (Image di2 = Image.makeRasterFromBitmap(dst)) { di2.readPixels(un, 0, 0); } px = un.readPixels();
                        }
                    }
                }
                if (px == null) return null;
                int n = dw * dh; int[] argb = new int[n];
                for (int i = 0, j = 0; i < n; i++, j += 4)
                    argb[i] = (px[j] & 0xFF) | (px[j + 1] & 0xFF) << 8 | (px[j + 2] & 0xFF) << 16 | (px[j + 3] & 0xFF) << 24;
                return new Object[]{ argb, alpha };
            }
        } catch (Throwable t) { if (t instanceof LinkageError) sBroken = true; return null; }
    }

    /** Skia encoders: PNG (lossless), JPEG/WebP-lossy (quality), WebP-lossless. */
    static synchronized byte[] encode(jet.gfx.PixelBuffer pb, Bitmap.CompressFormat f, int quality) {
        if (sBroken) return null;
        try (Image im = Image.makeRasterFromBytes(new ImageInfo(pb.w, pb.h, ColorType.BGRA_8888, pb.opaque ? ColorAlphaType.OPAQUE : ColorAlphaType.UNPREMUL), pb.bgraBytes(), pb.w * 4L)) {
            EncodedImageFormat fmt = f == Bitmap.CompressFormat.JPEG ? EncodedImageFormat.JPEG : f == Bitmap.CompressFormat.PNG ? EncodedImageFormat.PNG : EncodedImageFormat.WEBP;
            int q = f == Bitmap.CompressFormat.WEBP_LOSSLESS ? 100 : Math.max(0, Math.min(100, quality));
            try (Data d = im.encodeToData(fmt, q)) { return d == null ? null : d.getBytes(); }
        } catch (Throwable t) { if (t instanceof LinkageError) sBroken = true; return null; }
    }
}
