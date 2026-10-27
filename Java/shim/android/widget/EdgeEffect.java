package android.widget;
public class EdgeEffect {
    public static final int TYPE_GLOW = 0, TYPE_STRETCH = 1; public static final android.graphics.BlendMode DEFAULT_BLEND_MODE = null;
    private boolean finished = true; private int w, h, color = 0x33B5E5; private float dist;
    public EdgeEffect(android.content.Context c) {} public EdgeEffect(android.content.Context c, android.util.AttributeSet a) {}
    public void setSize(int w, int h) { this.w = w; this.h = h; } public boolean isFinished() { return finished; } public void finish() { finished = true; dist = 0; }
    public void onPull(float d) { finished = false; dist += d; } public void onPull(float d, float disp) { onPull(d); } public float onPullDistance(float d, float disp) { onPull(d); return d; }
    public float getDistance() { return dist; } public void onRelease() { finished = true; dist = 0; } public void onAbsorb(int v) { finished = false; }
    public void setColor(int c) { color = c; } public int getColor() { return color; } public int getMaxHeight() { return h; }
    public void setType(int t) {} public int getType() { return TYPE_GLOW; } public void setBlendMode(android.graphics.BlendMode m) {} public android.graphics.BlendMode getBlendMode() { return null; }
    public boolean draw(android.graphics.Canvas c) { finished = true; return false; }
}
