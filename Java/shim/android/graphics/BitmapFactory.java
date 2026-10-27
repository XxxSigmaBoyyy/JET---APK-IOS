package android.graphics;
import java.io.*;
/** JET: BitmapFactory over javax.imageio (PNG/JPEG/GIF/BMP). Honors inJustDecodeBounds, inSampleSize, density scaling. */
public class BitmapFactory {
    public static class Options {
        public Bitmap inBitmap; public boolean inMutable, inJustDecodeBounds, inPreferQualityOverSpeed, inDither, inPurgeable, inInputShareable;
        public int inSampleSize = 1, inDensity, inTargetDensity, inScreenDensity;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inPremultiplied = true, inScaled = true;
        public int outWidth, outHeight; public String outMimeType; public Bitmap.Config outConfig;
        public byte[] inTempStorage; public boolean mCancel;
        /** API 26+: colour management (decodes are sRGB; outColorSpace stays null = sRGB-equivalent on this host). */
        public android.graphics.ColorSpace inPreferredColorSpace, outColorSpace;
        public void requestCancelDecode() { mCancel = true; }
    }
    static String mime(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "image/jpeg";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "image/gif";
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') return "image/bmp";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }
    public static Bitmap decodeByteArray(byte[] data, int off, int len) { return decodeByteArray(data, off, len, null); }
    public static Bitmap decodeByteArray(byte[] data, int off, int len, Options o) {
        byte[] b = off == 0 && len == data.length ? data : java.util.Arrays.copyOfRange(data, off, off + len);
        return decode(b, o, 0);
    }
    public static Bitmap decodeStream(InputStream in) { return decodeStream(in, null, null); }
    public static Bitmap decodeStream(InputStream in, Rect outPadding, Options o) {
        if (in == null) return null;
        try { return decode(in.readAllBytes(), o, 0); } catch (IOException e) { return null; }
    }
    public static Bitmap decodeFile(String path) { return decodeFile(path, null); }
    public static Bitmap decodeFile(String path, Options o) {
        try (InputStream in = new FileInputStream(path)) { return decode(in.readAllBytes(), o, 0); } catch (IOException e) { return null; }
    }
    public static Bitmap decodeFileDescriptor(FileDescriptor fd) { return decodeFileDescriptor(fd, null, null); }
    public static Bitmap decodeFileDescriptor(FileDescriptor fd, Rect pad, Options o) {
        try (InputStream in = new FileInputStream(fd)) { return decode(in.readAllBytes(), o, 0); } catch (IOException e) { return null; }
    }
    public static Bitmap decodeResource(android.content.res.Resources r, int id) { return decodeResource(r, id, null); }
    public static Bitmap decodeResource(android.content.res.Resources r, int id, Options o) {
        try (InputStream in = r.openRawResource(id)) {
            int dens = r.jetResourceDensity(id);
            if (o == null) o = new Options();
            if (o.inDensity == 0) o.inDensity = dens;
            if (o.inTargetDensity == 0) o.inTargetDensity = r.getDisplayMetrics().densityDpi;
            return decode(in.readAllBytes(), o, 0);
        } catch (Exception e) { return null; }
    }
    public static Bitmap decodeResourceStream(android.content.res.Resources r, android.util.TypedValue v, InputStream in, Rect pad, Options o) {
        if (o == null) o = new Options();
        if (o.inDensity == 0 && v != null) o.inDensity = v.density == 0 ? 160 : v.density == 0xFFFF ? 0 : v.density;
        if (o.inTargetDensity == 0 && r != null) o.inTargetDensity = r.getDisplayMetrics().densityDpi;
        return decodeStream(in, pad, o);
    }
    static Bitmap decode(byte[] b, Options o, int unused) {
        String mime = mime(b);
        if (o != null) { o.outMimeType = mime; o.outWidth = -1; o.outHeight = -1; }
        try {
            int[] sb = JetImageIOCodec.bounds(b);
            if (sb != null) {   // Skia codec path (PNG/JPEG/WebP/GIF/BMP...): no AWT/ImageIO involved in decoding
                int s = o == null ? 1 : Math.max(1, Integer.highestOneBit(Math.max(1, o.inSampleSize)));
                int w = Math.max(1, sb[0] / s), h = Math.max(1, sb[1] / s);
                int sw = o == null ? w : scale(w, o), sh = o == null ? h : scale(h, o);
                if (o != null && o.inJustDecodeBounds) { o.outWidth = sw; o.outHeight = sh; o.outConfig = Bitmap.Config.ARGB_8888; return null; }
                Object[] r = JetImageIOCodec.decode(b, sw, sh);
                if (r != null) {
                    Bitmap bm = Bitmap.jetFromArgb((int[]) r[0], sw, sh, o != null && o.inMutable);
                    bm.setHasAlpha((Boolean) r[1]);
                    if (o != null) { o.outWidth = sw; o.outHeight = sh; o.outConfig = Bitmap.Config.ARGB_8888;
                        if (o.inScaled && o.inDensity != 0 && o.inTargetDensity != 0) bm.setDensity(o.inTargetDensity); else if (o.inDensity != 0) bm.setDensity(o.inDensity); }
                    return bm;
                }
            }
            { StringBuilder hx = new StringBuilder(); for (int i = 0; i < Math.min(16, b.length); i++) hx.append(String.format("%02x", b[i] & 0xff));
              String txt = new String(b, 0, Math.min(40, b.length), java.nio.charset.StandardCharsets.ISO_8859_1).replaceAll("[^\\x20-\\x7e]", ".");
              System.err.println("[jet-bitmap] no Skia codec for " + mime + " (" + b.length + " bytes) head=" + hx + " '" + txt + "'"); }
            if (o != null) { o.outWidth = -1; o.outHeight = -1; }
            return null;
        } catch (RuntimeException e) { System.err.println("[jet-bitmap] decode failed: " + e); return null; }
    }
    /** {w, h} via the Skia codec header (no full decode), or null. */
    public static int[] jetBounds(byte[] b) { int[] r = JetImageIOCodec.bounds(b); return r == null ? null : new int[]{ r[0], r[1] }; }
    static int scale(int v, Options o) {
        if (!o.inScaled || o.inDensity == 0 || o.inTargetDensity == 0 || o.inDensity == o.inTargetDensity) return v;
        return Math.max(1, (int) (v * (float) o.inTargetDensity / o.inDensity + 0.5f));
    }
}
