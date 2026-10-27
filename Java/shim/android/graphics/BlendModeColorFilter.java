package android.graphics;
/** API 29+ colour filter used by Compose ColorFilter.tint on SDK_INT >= 29. */
public class BlendModeColorFilter extends ColorFilter {
    private final int mColor;
    private final BlendMode mMode;
    public BlendModeColorFilter(int color, BlendMode mode) {
        mColor = color;
        mMode = mode == null ? BlendMode.SRC_IN : mode;
    }
    public int getColor() { return mColor; }
    public BlendMode getMode() { return mMode; }
    /** Map blend mode to the PorterDuff equivalent JetGfx already understands for tint. */
    public PorterDuff.Mode jetPorterDuffMode() {
        if (mMode == null) return PorterDuff.Mode.SRC_IN;
        switch (mMode) {
            case CLEAR: return PorterDuff.Mode.CLEAR;
            case SRC: return PorterDuff.Mode.SRC;
            case DST: return PorterDuff.Mode.DST;
            case SRC_OVER: return PorterDuff.Mode.SRC_OVER;
            case DST_OVER: return PorterDuff.Mode.DST_OVER;
            case SRC_IN: return PorterDuff.Mode.SRC_IN;
            case DST_IN: return PorterDuff.Mode.DST_IN;
            case SRC_OUT: return PorterDuff.Mode.SRC_OUT;
            case DST_OUT: return PorterDuff.Mode.DST_OUT;
            case SRC_ATOP: return PorterDuff.Mode.SRC_ATOP;
            case DST_ATOP: return PorterDuff.Mode.DST_ATOP;
            case XOR: return PorterDuff.Mode.XOR;
            case PLUS: return PorterDuff.Mode.ADD;
            case MODULATE:
            case MULTIPLY: return PorterDuff.Mode.MULTIPLY;
            case SCREEN: return PorterDuff.Mode.SCREEN;
            case OVERLAY: return PorterDuff.Mode.OVERLAY;
            case DARKEN: return PorterDuff.Mode.DARKEN;
            case LIGHTEN: return PorterDuff.Mode.LIGHTEN;
            default: return PorterDuff.Mode.SRC_IN;
        }
    }
}
