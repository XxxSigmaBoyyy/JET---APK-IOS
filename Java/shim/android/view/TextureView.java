package android.view;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
/** Headless TextureView: once attached and laid out it creates a SurfaceTexture and reports onSurfaceTextureAvailable (AOSP order).
 *  GL frames rendered into that texture (EGL window surface -> host pbuffer, Linux-only Mesa) arrive via jet.gl.FrameSink and are
 *  drawn as this view's content, scaled to the view and honouring setTransform/alpha. */
public class TextureView extends View implements jet.gl.FrameSink {
    public interface SurfaceTextureListener {
        void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);
        void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);
        boolean onSurfaceTextureDestroyed(SurfaceTexture surface);
        void onSurfaceTextureUpdated(SurfaceTexture surface);
    }
    private SurfaceTexture texture; private SurfaceTextureListener listener; private boolean opaque = true, available;
    private int lastW = -1, lastH = -1; private volatile jet.gfx.PixelBuffer frame; private final Matrix transform = new Matrix();
    private jet.gfx.PixelBuffer soft;
    public static volatile int jetFramesPosted;
    public TextureView(Context c) { this(c, null); }
    public TextureView(Context c, AttributeSet a) { super(c, a); }
    public TextureView(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public TextureView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }
    public void setSurfaceTextureListener(SurfaceTextureListener l) { listener = l; }
    public SurfaceTextureListener getSurfaceTextureListener() { return listener; }
    public SurfaceTexture getSurfaceTexture() { return texture; }
    public void setSurfaceTexture(SurfaceTexture st) { if (st == null) throw new NullPointerException("surfaceTexture must not be null"); if (texture != null) texture.jetSetOwner(null); texture = st; st.jetSetOwner(this); available = true; }
    public boolean isAvailable() { return available && texture != null; }
    public void setOpaque(boolean o) { opaque = o; invalidate(); }
    public boolean isOpaque() { return opaque; }
    public void setTransform(Matrix m) { transform.set(m); invalidate(); }
    public Matrix getTransform(Matrix m) { if (m == null) m = new Matrix(); m.set(transform); return m; }
    public Bitmap getBitmap() { return getBitmap(Math.max(1, getWidth()), Math.max(1, getHeight())); }
    public Bitmap getBitmap(int w, int h) { Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); return getBitmap(b); }
    public Bitmap getBitmap(Bitmap b) { jet.gfx.PixelBuffer f = frame; if (f != null && b != null) { Canvas c = new Canvas(b); c.jetDrawImage(f, java.awt.geom.AffineTransform.getScaleInstance(b.getWidth() / (double) f.w, b.getHeight() / (double) f.h), null); } return b; }
    public Canvas lockCanvas() { return lockCanvas(null); }
    public Canvas lockCanvas(android.graphics.Rect dirty) {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        if (soft == null || soft.w != w || soft.h != h) soft = new jet.gfx.PixelBuffer(w, h);
        return new Canvas(android.graphics.JetGfx.forBitmap(soft));
    }
    public void unlockCanvasAndPost(Canvas c) { if (c != null && c.jetGfx() != null) { c.jetGfx().flush(); c.jetGfx().dispose(); } frame = soft; jetFramesPosted++; postInvalidate(); }
    public int jetSinkWidth() { return Math.max(1, getWidth()); }
    public int jetSinkHeight() { return Math.max(1, getHeight()); }
    @Override public void jetPostFrame(int[] argb, int w, int h) {
        int[] px = argb.clone();
        if (Boolean.getBoolean("jet.tex.readswaprb")) {
            for (int i = 0; i < px.length; i++) {
                int c = px[i];
                px[i] = (c & 0xFF00FF00) | ((c & 0x00FF0000) >> 16) | ((c & 0x000000FF) << 16);
            }
        }
        jet.gfx.PixelBuffer img = new jet.gfx.PixelBuffer(w, h, px); img.opaque = opaque;
        frame = img; jetFramesPosted++; jet.gl.JetEGL10.texFrames++;
        final SurfaceTextureListener l = listener; final SurfaceTexture t = texture;
        if (l != null && t != null) post(() -> l.onSurfaceTextureUpdated(t));
        postInvalidate();
    }
    public jet.gfx.PixelBuffer jetLastFrame() { return frame; }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); jetUpdate(); }
    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); jetUpdate(); }
    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) { super.onLayout(ch, l, t, r, b); jetUpdate(); }
    @Override protected void onDetachedFromWindow() {
        if (available && texture != null) { available = false; boolean rel = listener == null || listener.onSurfaceTextureDestroyed(texture); if (rel) texture.release(); }
        super.onDetachedFromWindow();
    }
    static final boolean DBG = Boolean.getBoolean("jet.texdebug");
    private void jetUpdate() {
        if (DBG) System.err.println("[JETTEX] update attached=" + isAttachedToWindow() + " " + getWidth() + "x" + getHeight() + " vis=" + getVisibility());
        if (!isAttachedToWindow() || getWidth() <= 0 || getHeight() <= 0) return;
        int w = getWidth(), h = getHeight();
        if (texture == null) { texture = new SurfaceTexture(false); texture.jetSetOwner(this); }
        texture.setDefaultBufferSize(w, h);
        if (!available) { available = true; lastW = w; lastH = h; if (DBG) System.err.println("[JETTEX] available " + w + "x" + h + " listener=" + listener); if (listener != null) listener.onSurfaceTextureAvailable(texture, w, h); }
        else if (w != lastW || h != lastH) { lastW = w; lastH = h; if (listener != null) listener.onSurfaceTextureSizeChanged(texture, w, h); }
    }
    @Override public void draw(Canvas g) {
        // Drawn => it is in the window even if the attach walk missed this subtree (e.g. a parent re-added via a path without dispatch).
        if (!isAttachedToWindow()) jetDispatchAttached();
        if (!available) jetUpdate();
        if (texture != null) texture.jetSetOwner(this);
        // AOSP TextureView does not paint background over the hardware/GL content.
        jet.gfx.PixelBuffer f = frame;
        if (f == null && texture != null) f = texture.jetLastFrame();
        if (f == null && android.graphics.SurfaceTexture.lastPosted != null) f = android.graphics.SurfaceTexture.lastPosted.jetLastFrame();
        if (DBG) {
            int nz = 0, sample = 0; java.util.HashSet<Integer> cols = new java.util.HashSet<>();
            if (f != null) {
                int n = Math.min(f.px.length, f.w * f.h);
                for (int i = 0; i < n; i += Math.max(1, n / 2000)) {
                    int rgb = f.px[i] & 0x00FFFFFF; cols.add(rgb);
                    if (rgb != 0xFFFFFF && rgb != 0) { nz++; if (sample == 0) sample = f.px[i]; }
                }
            }
            System.err.println("[JETTEX] draw frame=" + (f == null ? "null" : f.w + "x" + f.h) + " nzSample=" + nz + " uniq~" + cols.size() + " firstNz=0x" + Integer.toHexString(sample) + " posts=" + jetFramesPosted + " ownerSet=" + (texture != null));
            if (f != null && jetFramesPosted > 30 && !java.nio.file.Files.exists(java.nio.file.Path.of("/tmp/jet_tex_dump.png"))) {
                try { android.graphics.Bitmap.jetWrap(f, false).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, new java.io.FileOutputStream("/tmp/jet_tex_dump.png"));
                    System.err.println("[JETTEX] dumped /tmp/jet_tex_dump.png"); } catch (Exception ignore) {}
            }
        }
        if (f != null && getVisibility() == VISIBLE && g != null) {
            java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getScaleInstance(getWidth() / (double) f.w, getHeight() / (double) f.h);
            g.jetDrawImage(f, at, null);
        }
    }
}
