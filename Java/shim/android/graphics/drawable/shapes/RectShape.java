package android.graphics.drawable.shapes;
public class RectShape extends Shape {
    private final android.graphics.RectF mRect = new android.graphics.RectF();
    public RectShape() {}
    @Override public void draw(android.graphics.Canvas c, android.graphics.Paint p) { c.drawRect(mRect, p); }
    @Override protected void onResize(float w, float h) { mRect.set(0, 0, w, h); }
    protected final android.graphics.RectF rect() { return mRect; }
    @Override public RectShape clone() throws CloneNotSupportedException { return (RectShape) super.clone(); }
}
