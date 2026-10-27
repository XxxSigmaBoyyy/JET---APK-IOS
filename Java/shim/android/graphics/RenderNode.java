package android.graphics;
/** android.graphics.RenderNode (API 29): display list + RenderProperties, replayed through the active jet.gfx backend. */
public final class RenderNode {
    private final String name; private static long ids = 1; private final long id = ids++;
    private JetRecordingGfx list; private RecordingCanvas recording;
    private int l, t, r, b; private float tx, ty, tz, sx = 1, sy = 1, rot, rotX, rotY, px, py, alpha = 1, elev, cam = 8;
    private boolean pivotSet, clipToBounds = true, clipToOutline, overlapping = true, forceDark, compositing, projectBackwards, projectionReceiver;
    private Rect clipRect; private Outline outline; private int ambient = 0xFF000000, spot = 0xFF000000; private Paint layerPaint; private RenderEffect effect;
    public RenderNode(String name) { this.name = name; }
    public RecordingCanvas beginRecording(int width, int height) { recording = new RecordingCanvas(width, height); return recording; }
    public RecordingCanvas beginRecording() { return beginRecording(getWidth(), getHeight()); }
    public void endRecording() { if (recording != null) { list = recording.rec; recording = null; } }
    public boolean hasDisplayList() { return list != null; }
    public void discardDisplayList() { list = null; }
    public long getUniqueId() { return id; }
    public boolean setPosition(int l, int t, int r, int b) { boolean ch = l != this.l || t != this.t || r != this.r || b != this.b; this.l = l; this.t = t; this.r = r; this.b = b; return ch; }
    public boolean setPosition(Rect p) { return setPosition(p.left, p.top, p.right, p.bottom); }
    public boolean offsetLeftAndRight(int o) { l += o; r += o; return o != 0; }
    public boolean offsetTopAndBottom(int o) { t += o; b += o; return o != 0; }
    public int getLeft() { return l; } public int getTop() { return t; } public int getRight() { return r; } public int getBottom() { return b; }
    public int getWidth() { return r - l; } public int getHeight() { return b - t; }
    public boolean setTranslationX(float v) { tx = v; return true; } public float getTranslationX() { return tx; }
    public boolean setTranslationY(float v) { ty = v; return true; } public float getTranslationY() { return ty; }
    public boolean setTranslationZ(float v) { tz = v; return true; } public float getTranslationZ() { return tz; }
    public boolean setScaleX(float v) { sx = v; return true; } public float getScaleX() { return sx; }
    public boolean setScaleY(float v) { sy = v; return true; } public float getScaleY() { return sy; }
    public boolean setRotationZ(float v) { rot = v; return true; } public float getRotationZ() { return rot; }
    public boolean setRotationX(float v) { rotX = v; return true; } public float getRotationX() { return rotX; }
    public boolean setRotationY(float v) { rotY = v; return true; } public float getRotationY() { return rotY; }
    public boolean setPivotX(float v) { px = v; pivotSet = true; return true; } public float getPivotX() { return pivotSet ? px : getWidth() / 2f; }
    public boolean setPivotY(float v) { py = v; pivotSet = true; return true; } public float getPivotY() { return pivotSet ? py : getHeight() / 2f; }
    public boolean isPivotExplicitlySet() { return pivotSet; } public boolean resetPivot() { pivotSet = false; return true; }
    public boolean setAlpha(float a) { alpha = a; return true; } public float getAlpha() { return alpha; }
    public boolean setElevation(float e) { elev = e; return true; } public float getElevation() { return elev; }
    public boolean setCameraDistance(float d) { cam = d; return true; } public float getCameraDistance() { return cam; }
    public boolean setClipToBounds(boolean c) { clipToBounds = c; return true; } public boolean getClipToBounds() { return clipToBounds; }
    public boolean setClipToOutline(boolean c) { clipToOutline = c; return true; } public boolean getClipToOutline() { return clipToOutline; }
    public boolean setClipRect(Rect c) { clipRect = c == null ? null : new Rect(c); return true; }
    public boolean setOutline(Outline o) { outline = o; return true; }
    public boolean hasShadow() { return outline != null && elev + tz > 0; }
    public boolean setAmbientShadowColor(int c) { ambient = c; return true; } public int getAmbientShadowColor() { return ambient; }
    public boolean setSpotShadowColor(int c) { spot = c; return true; } public int getSpotShadowColor() { return spot; }
    public boolean setHasOverlappingRendering(boolean o) { overlapping = o; return true; } public boolean hasOverlappingRendering() { return overlapping; }
    public boolean setUseCompositingLayer(boolean f, Paint p) { compositing = f; layerPaint = p; return true; } public boolean getUseCompositingLayer() { return compositing; }
    public boolean setRenderEffect(RenderEffect e) { effect = e; return true; }
    public boolean setForceDarkAllowed(boolean a) { forceDark = a; return true; } public boolean isForceDarkAllowed() { return forceDark; }
    public boolean setProjectBackwards(boolean p) { projectBackwards = p; return true; }
    public boolean setProjectionReceiver(boolean p) { projectionReceiver = p; return true; }
    public boolean hasIdentityMatrix() { return tx == 0 && ty == 0 && sx == 1 && sy == 1 && rot == 0 && rotX == 0 && rotY == 0; }
    public boolean setAnimationMatrix(Matrix m) { return true; } public Matrix getAnimationMatrix() { return null; }
    public boolean setStaticMatrix(Matrix m) { return true; }
    public long computeApproximateMemoryUsage() { return list == null ? 0 : list.ops.size() * 64L; }
    public long computeApproximateMemoryAllocated() { return computeApproximateMemoryUsage(); }
    public boolean isForceDarkAllowedJet() { return forceDark; }
    /** Local transform (excluding left/top), AOSP RenderProperties order: translate · pivot(rotate · scale). */
    java.awt.geom.AffineTransform jetLocal() {
        java.awt.geom.AffineTransform a = java.awt.geom.AffineTransform.getTranslateInstance(tx, ty);
        if (sx != 1 || sy != 1 || rot != 0) { float x = getPivotX(), y = getPivotY(); a.translate(x, y); if (rot != 0) a.rotate(Math.toRadians(rot)); a.scale(sx, sy); a.translate(-x, -y); }
        return a;
    }
    public void getMatrix(Matrix out) { if (out != null) out.a = jetLocal(); }
    public void getInverseMatrix(Matrix out) { if (out != null) { try { out.a = jetLocal().createInverse(); } catch (Exception e) { out.a = new java.awt.geom.AffineTransform(); } } }
    /** Draw this node into c (Canvas.drawRenderNode). */
    public void jetReplay(Canvas c) {
        if (list == null || alpha <= 0) return;
        jet.gfx.Gfx g = c.jetGfx(); if (g == null) return;
        int d0 = g.depth(); g.save();
        try {
            g.concat(java.awt.geom.AffineTransform.getTranslateInstance(l, t)); g.concat(jetLocal());
            // Soft ambient shadow when elevation/outline set (Compose Modifier.shadow). Without this,
            // some hosts draw a hard dark rect; we approximate a translucent outline fill instead.
            if (hasShadow()) {
                Path op = outline.jetPath();
                if (op != null) {
                    // BlurMaskFilter often no-op on host Skia → hard dark box. Soft multi-layer approx.
                    Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    int rgb = (spot != 0 ? spot : ambient) & 0x00FFFFFF;
                    sp.setStyle(Paint.Style.FILL);
                    float dy = Math.min(6f, Math.max(1f, elev * 0.25f));
                    java.awt.Rectangle b = op.p.getBounds();
                    for (int i = 3; i >= 1; i--) {
                        int a = Math.min(24, Math.max(5, (int) ((5 + elev) * i / 3)));
                        sp.setColor((a << 24) | rgb);
                        g.save();
                        double grow = i * Math.max(1.0, elev * 0.12);
                        if (b.width > 0 && b.height > 0) {
                            double cx = b.getCenterX(), cy = b.getCenterY();
                            double sx = (b.width + grow * 2) / b.width, sy = (b.height + grow * 2) / b.height;
                            g.concat(java.awt.geom.AffineTransform.getTranslateInstance(cx, cy + dy));
                            g.concat(java.awt.geom.AffineTransform.getScaleInstance(sx, sy));
                            g.concat(java.awt.geom.AffineTransform.getTranslateInstance(-cx, -cy));
                        } else {
                            g.concat(java.awt.geom.AffineTransform.getTranslateInstance(0, dy));
                        }
                        g.drawShape(op.p, sp, true, false);
                        g.restore();
                    }
                }
            }
            if (clipToBounds) g.clip(new java.awt.geom.Rectangle2D.Float(0, 0, getWidth(), getHeight()));
            if (clipRect != null) g.clip(new java.awt.geom.Rectangle2D.Float(clipRect.left, clipRect.top, clipRect.width(), clipRect.height()));
            if (clipToOutline && outline != null) { Path p = outline.jetPath(); if (p != null) g.clip(p.p); }
            int la = layerPaint != null ? layerPaint.getAlpha() : 255;
            if (alpha < 1f || la < 255) g.saveLayer(Math.round(255 * Math.max(0, Math.min(1, alpha)) * la / 255f));
            list.replay(g);
        } finally { while (g.depth() > d0) g.restore(); }
    }
}
