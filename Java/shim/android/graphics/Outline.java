package android.graphics;
public final class Outline {
    public final Rect mRect = new Rect(); public float mRadius; private float alpha = 1f; private boolean empty = true;
    public Outline() {} public Outline(Outline o) { set(o); }
    public void set(Outline o) { mRect.set(o.mRect); mRadius = o.mRadius; alpha = o.alpha; empty = o.empty; }
    public void setEmpty() { empty = true; mRect.set(0, 0, 0, 0); } public boolean isEmpty() { return empty; }
    public void setRect(int l, int t, int r, int b) { mRect.set(l, t, r, b); mRadius = 0; empty = false; mPath = null; } public void setRect(Rect r) { setRect(r.left, r.top, r.right, r.bottom); }
    public void setRoundRect(int l, int t, int r, int b, float rad) { setRect(l, t, r, b); mRadius = rad; } public void setRoundRect(Rect r, float rad) { setRoundRect(r.left, r.top, r.right, r.bottom, rad); }
    public void setOval(int l, int t, int r, int b) { setRect(l, t, r, b); mRadius = Math.min(r - l, b - t) / 2f; } public void setOval(Rect r) { setOval(r.left, r.top, r.right, r.bottom); }
    Path mPath; public void setPath(Path p) { empty = false; mPath = p; } public void setConvexPath(Path p) { setPath(p); }
    /** Clip shape for RenderNode.setClipToOutline. */
    public Path jetPath() { if (empty) return null; if (mPath != null) return mPath; Path p = new Path(); p.addRoundRect(new RectF(mRect), mRadius, mRadius, Path.Direction.CW); return p; }
    public void setAlpha(float a) { alpha = a; } public float getAlpha() { return alpha; } public float getRadius() { return mRadius; }
    public boolean getRect(Rect out) { out.set(mRect); return !empty; } public boolean canClip() { return true; } public void offset(int dx, int dy) { mRect.offset(dx, dy); }
}
