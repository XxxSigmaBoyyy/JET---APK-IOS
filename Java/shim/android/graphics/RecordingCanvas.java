package android.graphics;
/** android.graphics.RecordingCanvas (API 29): a Canvas whose backend is a display-list recorder. */
public final class RecordingCanvas extends Canvas {
    final JetRecordingGfx rec;
    RecordingCanvas(int w, int h) { super(new JetRecordingGfx(w, h)); rec = (JetRecordingGfx) g; }
    @Override public boolean isHardwareAccelerated() { return true; }
    @Override public void drawRenderNode(RenderNode n) { if (n != null) rec.addNode(n); }
    @Override public void enableZ() {} @Override public void disableZ() {}
}
