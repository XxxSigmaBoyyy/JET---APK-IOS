package android.graphics;
import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
/** ImageIO-backed codec for iOS Zero stage (no Skija). */
public final class JetImageIOCodec {
    private JetImageIOCodec() {}
    public static int[] bounds(byte[] data) {
        try {
            BufferedImage im = ImageIO.read(new ByteArrayInputStream(data));
            if (im == null) return null;
            return new int[]{ im.getWidth(), im.getHeight() };
        } catch (Throwable t) { return null; }
    }
    /** returns {int[] argb (sw x sh), Boolean hasAlpha} or null; BitmapFactory.decode reads r[1] as hasAlpha. */
    public static Object[] decode(byte[] data, int sw, int sh) {
        try {
            BufferedImage im = ImageIO.read(new ByteArrayInputStream(data));
            if (im == null) return null;
            int w = im.getWidth(), h = im.getHeight();
            boolean alpha = im.getColorModel().hasAlpha();
            if (sw > 0 && sh > 0 && (sw != w || sh != h)) {
                BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.drawImage(im, 0, 0, sw, sh, null); g.dispose();
                im = scaled; w = sw; h = sh;
            }
            int[] px = new int[w * h];
            im.getRGB(0, 0, w, h, px, 0, w);
            return new Object[]{ px, Boolean.valueOf(alpha) };
        } catch (Throwable t) { return null; }
    }
    public static byte[] encode(jet.gfx.PixelBuffer pb, String format, int quality) {
        try {
            BufferedImage im = new BufferedImage(pb.w, pb.h, BufferedImage.TYPE_INT_ARGB);
            im.setRGB(0, 0, pb.w, pb.h, pb.px, 0, pb.w);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String fmt = format == null ? "png" : format.toLowerCase();
            if (fmt.contains("jpeg") || fmt.equals("jpg")) fmt = "jpg";
            else fmt = "png";
            if (!ImageIO.write(im, fmt, bos)) return null;
            return bos.toByteArray();
        } catch (Throwable t) { return null; }
    }
}
