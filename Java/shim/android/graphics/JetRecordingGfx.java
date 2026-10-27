package android.graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.function.Consumer;
/** Display-list backend: records Gfx ops (RenderNode / RecordingCanvas / Picture) and replays them onto any backend. */
final class JetRecordingGfx implements jet.gfx.Gfx {
    final int w, h;
    final ArrayList<Consumer<Replay>> ops = new ArrayList<>();
    private AffineTransform tx = new AffineTransform();
    private final java.util.ArrayDeque<Object[]> st = new java.util.ArrayDeque<>();
    private Rectangle2D clipB; // user-space-at-record-time approximation, device(local origin) coords
    JetRecordingGfx(int w, int h) { this.w = w; this.h = h; }
    static final class Replay { final jet.gfx.Gfx t; final AffineTransform base; Replay(jet.gfx.Gfx t) { this.t = t; base = t.getTransform(); } }
    private static Paint cp(Paint p) { return p == null ? null : new Paint(p); }
    private static Shape cs(Shape s) { return s instanceof Path2D ? (Shape) ((Path2D) s).clone() : s; }
    public int width() { return w; } public int height() { return h; }
    public void save() { st.push(new Object[] { new AffineTransform(tx), clipB }); ops.add(r -> r.t.save()); }
    public void saveLayer(int a) { st.push(new Object[] { new AffineTransform(tx), clipB }); ops.add(r -> r.t.saveLayer(a)); }
    public void restore() { if (st.isEmpty()) return; Object[] s = st.pop(); tx = (AffineTransform) s[0]; clipB = (Rectangle2D) s[1]; ops.add(r -> r.t.restore()); }
    public int depth() { return st.size(); }
    public AffineTransform getTransform() { return new AffineTransform(tx); }
    public void setTransform(AffineTransform t) { tx = new AffineTransform(t); final AffineTransform c = new AffineTransform(t); ops.add(r -> { AffineTransform b = new AffineTransform(r.base); b.concatenate(c); r.t.setTransform(b); }); }
    public void concat(AffineTransform t) { tx.concatenate(t); final AffineTransform c = new AffineTransform(t); ops.add(r -> r.t.concat(c)); }
    public void clip(Shape s) {
        Rectangle2D b = tx.createTransformedShape(s).getBounds2D(); clipB = clipB == null ? b : clipB.createIntersection(b);
        final Shape c = cs(s); ops.add(r -> r.t.clip(c));
    }
    public void clipOut(Shape s) { final Shape c = cs(s); ops.add(r -> r.t.clipOut(c)); }
    public Rectangle getClipBounds() {
        if (clipB == null) return null;
        try { return tx.createInverse().createTransformedShape(clipB).getBounds(); } catch (NoninvertibleTransformException e) { return new Rectangle(); }
    }
    public void drawShape(Shape s, Paint p, boolean f, boolean k) { final Shape c = cs(s); final Paint q = cp(p); ops.add(r -> r.t.drawShape(c, q, f, k)); }
    public void drawPaint(Paint p) { final Paint q = cp(p); ops.add(r -> r.t.drawPaint(q)); }
    public void drawText(String s, float x, float y, int style, int size, Paint p) { final Paint q = cp(p); ops.add(r -> r.t.drawText(s, x, y, style, size, q)); }
    public void drawImage(jet.gfx.PixelBuffer im, AffineTransform at, Paint p) { final Paint q = cp(p); final AffineTransform a = new AffineTransform(at); ops.add(r -> r.t.drawImage(im, a, q)); }
    public void drawColor(int c, int mode) { ops.add(r -> r.t.drawColor(c, mode)); }
    public void flush() {} public void dispose() {}
    /** Replay onto target; balanced save/restore. Nested RenderNodes are replayed by reference (ops capture the node). */
    void replay(jet.gfx.Gfx target) {
        Replay r = new Replay(target); int d0 = target.depth(); target.save();
        for (Consumer<Replay> op : ops) op.accept(r);
        while (target.depth() > d0) target.restore();
    }
    void addNode(RenderNode n) { ops.add(r -> n.jetReplay(new Canvas(r.t))); }
}
