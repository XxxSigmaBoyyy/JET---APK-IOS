package android.graphics.drawable.shapes;
public class OvalShape extends RectShape {
    public OvalShape() {}
    @Override public void draw(android.graphics.Canvas c, android.graphics.Paint p) { c.drawOval(rect(), p); }
    @Override public OvalShape clone() throws CloneNotSupportedException { return (OvalShape) super.clone(); }
}
