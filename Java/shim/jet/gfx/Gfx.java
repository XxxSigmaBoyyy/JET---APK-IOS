package jet.gfx;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
/**
 * THE single raster backend interface. Every pixel the shim produces goes through one of these
 * (android.graphics.Canvas is a facade over it). Geometry is java.awt.geom (pure-Java math, no rasteriser);
 * pixels interchange as jet.gfx.PixelBuffer (unpremultiplied ARGB ints; no AWT imaging). Implementations: Java2D (reference),
 * Skia (Skija on Linux; Skia/Metal on iOS), Recording (RenderNode / Picture display lists).
 */
public interface Gfx {
    /** drawText style bits (numerically equal to the java.awt.Font constants the Java2D reference uses). */
    int STYLE_BOLD = 1, STYLE_ITALIC = 2;
    int width(); int height();
    void save(); void saveLayer(int alpha); void restore(); int depth();
    AffineTransform getTransform(); void setTransform(AffineTransform t); void concat(AffineTransform t);
    void clip(Shape userSpace); void clipOut(Shape userSpace);
    /** Clip bounds in user space; null = unbounded. */
    Rectangle getClipBounds();
    void drawShape(Shape s, android.graphics.Paint p, boolean fill, boolean stroke);
    void drawPaint(android.graphics.Paint p);
    /** Text at baseline (x,y); x already alignment-adjusted. style = STYLE_BOLD|STYLE_ITALIC bits, size in px. */
    void drawText(String s, float x, float y, int style, int size, android.graphics.Paint p);
    void drawImage(PixelBuffer im, AffineTransform at, android.graphics.Paint p);
    /** mode: 0 SRC_OVER, 1 SRC, 2 CLEAR — fills the current clip. */
    void drawColor(int argb, int mode);
    /** Make pixels visible in the target image (no-op for Java2D). */
    void flush();
    void dispose();
}
