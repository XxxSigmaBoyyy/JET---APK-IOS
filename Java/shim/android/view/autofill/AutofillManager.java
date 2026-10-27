package android.view.autofill;
/** No autofill service is selected on this host: disabled; notify* calls are accepted and ignored, like AOSP without a service. */
public final class AutofillManager {
    public static final String EXTRA_ASSIST_STRUCTURE = "android.view.autofill.extra.ASSIST_STRUCTURE";
    public static final String EXTRA_AUTHENTICATION_RESULT = "android.view.autofill.extra.AUTHENTICATION_RESULT";
    public static final String EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET = "android.view.autofill.extra.AUTHENTICATION_RESULT_EPHEMERAL_DATASET";
    public static final String EXTRA_CLIENT_STATE = "android.view.autofill.extra.CLIENT_STATE";
    public static final String EXTRA_INLINE_SUGGESTIONS_REQUEST = "android.view.autofill.extra.INLINE_SUGGESTIONS_REQUEST";
    public abstract static class AutofillCallback { public void onAutofillEvent(android.view.View v, int event) {} public void onAutofillEvent(android.view.View v, int virtualId, int event) {} }
    AutofillManager() {}
    public boolean isEnabled() { return false; } public boolean hasEnabledAutofillServices() { return false; }
    public boolean isAutofillSupported() { return false; } public boolean isFieldClassificationEnabled() { return false; }
    public boolean isAutofillServiceAvailable() { return false; }
    public void requestAutofill(android.view.View v) {} public void requestAutofill(android.view.View v, int id, android.graphics.Rect r) {}
    public void notifyViewEntered(android.view.View v) {} public void notifyViewEntered(android.view.View v, int id, android.graphics.Rect r) {}
    public void notifyViewExited(android.view.View v) {} public void notifyViewExited(android.view.View v, int id) {}
    public void notifyViewVisibilityChanged(android.view.View v, boolean visible) {} public void notifyViewVisibilityChanged(android.view.View v, int id, boolean visible) {}
    public void notifyValueChanged(android.view.View v) {} public void notifyValueChanged(android.view.View v, int id, AutofillValue value) {}
    public void notifyViewClicked(android.view.View v) {} public void notifyViewClicked(android.view.View v, int id) {}
    public void commit() {} public void cancel() {} public void disableAutofillServices() {}
    public void registerCallback(AutofillCallback cb) {} public void unregisterCallback(AutofillCallback cb) {}
    public android.content.ComponentName getAutofillServiceComponentName() { return null; }
    public AutofillId getNextAutofillId() { return null; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public void notifyVirtualViewsReady(android.view.View p0, android.util.SparseArray p1) { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.notifyVirtualViewsReady(Landroid/view/View;Landroid/util/SparseArray;)V"); }
    public java.lang.String getUserDataId() { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.getUserDataId()Ljava/lang/String;"); return null; }
    public java.lang.String getDefaultFieldClassificationAlgorithm() { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.getDefaultFieldClassificationAlgorithm()Ljava/lang/String;"); return null; }
    public java.util.List getAvailableFieldClassificationAlgorithms() { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.getAvailableFieldClassificationAlgorithms()Ljava/util/List;"); return null; }
    public boolean showAutofillDialog(android.view.View p0) { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.showAutofillDialog(Landroid/view/View;)Z"); return false; }
    public boolean showAutofillDialog(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.autofill.AutofillManager.showAutofillDialog(Landroid/view/View;I)Z"); return false; }
    // ---- END generated SDK surface ----
}
