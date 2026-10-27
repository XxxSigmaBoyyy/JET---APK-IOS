package jet.gfx;
/** Backend-provided font metrics (integer, like java.awt.FontMetrics) so layout matches the rasteriser. */
public abstract class TextMetrics {
    public abstract int getAscent(); public abstract int getDescent(); public abstract int getLeading();
    public int getHeight() { return getAscent() + getDescent() + getLeading(); }
    public abstract int stringWidth(String s);
    /** Fractional advance (Paint.measureText). */
    public abstract float advance(String s);
    public abstract boolean canDisplay(String s);
    public int getMaxAscent() { return getAscent(); } public int getMaxDescent() { return getDescent(); }
}
