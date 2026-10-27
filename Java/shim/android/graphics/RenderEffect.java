package android.graphics;
/** android.graphics.RenderEffect (API 31): recorded, not yet rasterised (blur/colour-filter effects are no-ops). */
public final class RenderEffect {
    private RenderEffect() {}
    public static RenderEffect createOffsetEffect(float x, float y) { return new RenderEffect(); }
    public static RenderEffect createOffsetEffect(float x, float y, RenderEffect in) { return new RenderEffect(); }
    public static RenderEffect createBlurEffect(float rx, float ry, Shader.TileMode m) { return new RenderEffect(); }
    public static RenderEffect createBlurEffect(float rx, float ry, RenderEffect in, Shader.TileMode m) { return new RenderEffect(); }
    public static RenderEffect createBitmapEffect(Bitmap b) { return new RenderEffect(); }
    public static RenderEffect createBitmapEffect(Bitmap b, Rect s, Rect d) { return new RenderEffect(); }
    public static RenderEffect createColorFilterEffect(ColorFilter f) { return new RenderEffect(); }
    public static RenderEffect createColorFilterEffect(ColorFilter f, RenderEffect in) { return new RenderEffect(); }
    public static RenderEffect createBlendModeEffect(RenderEffect d, RenderEffect s, BlendMode m) { return new RenderEffect(); }
    public static RenderEffect createChainEffect(RenderEffect o, RenderEffect i) { return new RenderEffect(); }
    public static RenderEffect createShaderEffect(Shader s) { return new RenderEffect(); }
}
