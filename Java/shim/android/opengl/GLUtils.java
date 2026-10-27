package android.opengl;
import android.graphics.Bitmap;
/** GLUtils: Bitmap -> GL texture upload (RGBA8888) through the GLES20 bridge (Linux host: Mesa). */
public final class GLUtils {
    private GLUtils() {}
    /** Pack Bitmap pixels as RGBA8888 for GL_RGBA / GL_UNSIGNED_BYTE.
     *  Source Bitmap stores unpremultiplied ARGB ints (see Bitmap). Do NOT premultiply by default —
     *  double-premult darkens AA edges and Telegram Intro samples look like striped noise.
     *  Opt in with -Djet.tex.premult=true. Swap R/B with -Djet.tex.swaprb=true (upload only). */
    static java.nio.ByteBuffer rgba(Bitmap b) {
        if (b.getConfig() != Bitmap.Config.ARGB_8888 && b.getConfig() != null) {
            Bitmap c = b.copy(Bitmap.Config.ARGB_8888, false);
            if (c != null) b = c;
        }
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        // Native byte order for the element type does not matter: we put() bytes one at a time.
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(w * h * 4);
        boolean swapRB = Boolean.getBoolean("jet.tex.swaprb");
        boolean premult = Boolean.getBoolean("jet.tex.premult"); // default off
        for (int p : px) {
            int a = (p >>> 24) & 255;
            int r = (p >> 16) & 255, g = (p >> 8) & 255, bl = p & 255;
            if (swapRB) { int tmp = r; r = bl; bl = tmp; }
            if (premult && a != 255) {
                r = r * a / 255; g = g * a / 255; bl = bl * a / 255;
            }
            bb.put((byte) r).put((byte) g).put((byte) bl).put((byte) a);
        }
        bb.position(0);
        bb.limit(w * h * 4);
        return bb;
    }
    public static int getInternalFormat(Bitmap b) { return GLES20.GL_RGBA; }
    public static int getType(Bitmap b) { return GLES20.GL_UNSIGNED_BYTE; }
    public static void texImage2D(int target, int level, int internalformat, Bitmap b, int type, int border) {
        if (b == null) throw new NullPointerException("texImage2D can't be used with a null Bitmap");
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        // Clear any leftover UNPACK_ROW_LENGTH from app code (GLES3).
        try { GLES20.glPixelStorei(0x0CF2 /* GL_UNPACK_ROW_LENGTH */, 0); } catch (Throwable ignore) {}
        java.nio.ByteBuffer rb = rgba(b);
        if (Boolean.getBoolean("jet.texdebug")) {
            int nz = 0; java.nio.ByteBuffer peek = rb.duplicate();
            while (peek.remaining() >= 4) {
                int r = peek.get() & 255, g = peek.get() & 255, bl = peek.get() & 255, a = peek.get() & 255;
                if ((r | g | bl) != 0 && a != 0) nz++;
            }
            System.err.println("[JETTEX] texImage2D " + b.getWidth() + "x" + b.getHeight()
                    + " nzPx~" + nz + " target=" + target + " swapRB=" + Boolean.getBoolean("jet.tex.swaprb")
                    + " premult=" + Boolean.getBoolean("jet.tex.premult"));
        }
        GLES20.glTexImage2D(target, level, GLES20.GL_RGBA, b.getWidth(), b.getHeight(), border,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rb);
    }
    public static void texImage2D(int target, int level, int internalformat, Bitmap b, int border) {
        texImage2D(target, level, internalformat, b, GLES20.GL_UNSIGNED_BYTE, border);
    }
    public static void texImage2D(int target, int level, Bitmap b, int border) {
        texImage2D(target, level, GLES20.GL_RGBA, b, GLES20.GL_UNSIGNED_BYTE, border);
    }
    public static void texSubImage2D(int target, int level, int x, int y, Bitmap b) {
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        try { GLES20.glPixelStorei(0x0CF2, 0); } catch (Throwable ignore) {}
        GLES20.glTexSubImage2D(target, level, x, y, b.getWidth(), b.getHeight(),
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgba(b));
    }
    public static void texSubImage2D(int target, int level, int x, int y, Bitmap b, int format, int type) {
        texSubImage2D(target, level, x, y, b);
    }
    public static String getEGLErrorString(int e) { return "EGL error 0x" + Integer.toHexString(e); }
}
