package android.graphics;
/** Blur mask filter (API 1). Host applies via Skia MaskFilter when jet.gfx=skia. */
public class BlurMaskFilter extends MaskFilter {
    public enum Blur { NORMAL, SOLID, OUTER, INNER }
    public final float radius;
    public final Blur style;
    public BlurMaskFilter(float radius, Blur style) {
        this.radius = Math.max(0f, radius);
        this.style = style == null ? Blur.NORMAL : style;
    }
}
