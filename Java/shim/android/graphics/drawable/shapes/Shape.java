package android.graphics.drawable.shapes;
public abstract class Shape implements Cloneable {
    private float mWidth, mHeight;
    public final float getWidth() { return mWidth; } public final float getHeight() { return mHeight; }
    public abstract void draw(android.graphics.Canvas c, android.graphics.Paint p);
    public final void resize(float w, float h) { if (w < 0) w = 0; if (h < 0) h = 0; if (mWidth != w || mHeight != h) { mWidth = w; mHeight = h; onResize(w, h); } }
    public boolean hasAlpha() { return true; }
    protected void onResize(float w, float h) {}
    public void getOutline(android.graphics.Outline o) {}
    @Override public Shape clone() throws CloneNotSupportedException { return (Shape) super.clone(); }
}
