package android.view.contentcapture;
/** Content capture is disabled on JET (View.getContentCaptureSession() returns null, as on a device with the service off); the type exists so app code links. */
public abstract class ContentCaptureSession implements AutoCloseable {
    protected ContentCaptureSession() {}
    public final ContentCaptureSessionId getContentCaptureSessionId() { return null; }
    public final ContentCaptureSession createContentCaptureSession(ContentCaptureContext context) { return null; }
    public final void setContentCaptureContext(ContentCaptureContext context) {}
    public final ContentCaptureContext getContentCaptureContext() { return null; }
    public final void destroy() {}
    public void close() { destroy(); }
    public final void notifyViewAppeared(android.view.ViewStructure node) {}
    public final void notifyViewDisappeared(android.view.autofill.AutofillId id) {}
    public final void notifyViewsDisappeared(android.view.autofill.AutofillId hostId, long[] virtualIds) {}
    public final void notifyViewTextChanged(android.view.autofill.AutofillId id, CharSequence text) {}
    public final void notifyViewInsetsChanged(android.graphics.Insets viewInsets) {}
    public final void notifySessionResumed() {}
    public final void notifySessionPaused() {}
    public final android.view.ViewStructure newViewStructure(android.view.View view) { return null; }
    public android.view.autofill.AutofillId newAutofillId(android.view.autofill.AutofillId hostId, long virtualChildId) { return null; }
    public final android.view.ViewStructure newVirtualViewStructure(android.view.autofill.AutofillId parentId, long virtualId) { return null; }
}
