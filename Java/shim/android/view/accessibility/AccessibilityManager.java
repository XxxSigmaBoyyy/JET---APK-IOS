package android.view.accessibility;
import java.util.*;
/** No accessibility services are running on this host: disabled, touch exploration off; listeners are registered and never fire. */
public final class AccessibilityManager {
    public static final int FLAG_CONTENT_CONTROLS = 4;
    public static final int FLAG_CONTENT_ICONS = 1;
    public static final int FLAG_CONTENT_TEXT = 2;
    public interface AccessibilityStateChangeListener { void onAccessibilityStateChanged(boolean enabled); }
    public interface TouchExplorationStateChangeListener { void onTouchExplorationStateChanged(boolean enabled); }
    public interface AccessibilityServicesStateChangeListener { void onAccessibilityServicesStateChanged(AccessibilityManager m); }
    public interface AudioDescriptionRequestedChangeListener { void onAudioDescriptionRequestedChanged(boolean enabled); }
    private final Set<Object> mListeners = Collections.synchronizedSet(new LinkedHashSet<>());
    AccessibilityManager() {}
    public boolean isEnabled() { return false; } public boolean isTouchExplorationEnabled() { return false; }
    public boolean isAudioDescriptionRequested() { return false; } public boolean isRequestFromAccessibilityTool() { return false; }
    public void sendAccessibilityEvent(AccessibilityEvent e) { throw new IllegalStateException("Accessibility off. Did you forget to check that?"); }
    public void interrupt() { throw new IllegalStateException("Accessibility off. Did you forget to check that?"); }
    public List<android.accessibilityservice.AccessibilityServiceInfo> getInstalledAccessibilityServiceList() { return new ArrayList<>(); }
    public List<android.accessibilityservice.AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackTypeFlags) { return new ArrayList<>(); }
    public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener l) { return mListeners.add(l) || true; }
    public void addAccessibilityStateChangeListener(AccessibilityStateChangeListener l, android.os.Handler h) { mListeners.add(l); }
    public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener l) { return mListeners.remove(l); }
    public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener l) { return mListeners.add(l) || true; }
    public void addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener l, android.os.Handler h) { mListeners.add(l); }
    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener l) { return mListeners.remove(l); }
    public void addAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener l) { mListeners.add(l); }
    public void addAccessibilityServicesStateChangeListener(java.util.concurrent.Executor e, AccessibilityServicesStateChangeListener l) { mListeners.add(l); }
    public boolean removeAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener l) { return mListeners.remove(l); }
    public void addAudioDescriptionRequestedChangeListener(java.util.concurrent.Executor e, AudioDescriptionRequestedChangeListener l) { mListeners.add(l); }
    public boolean removeAudioDescriptionRequestedChangeListener(AudioDescriptionRequestedChangeListener l) { return mListeners.remove(l); }
    public int getRecommendedTimeoutMillis(int originalTimeout, int uiContentFlags) { return originalTimeout; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public java.util.List getAccessibilityServiceList() { jet.runtime.ShimStubs.hit("android.view.accessibility.AccessibilityManager.getAccessibilityServiceList()Ljava/util/List;"); return null; }
    public int getAccessibilityFocusStrokeWidth() { jet.runtime.ShimStubs.hit("android.view.accessibility.AccessibilityManager.getAccessibilityFocusStrokeWidth()I"); return 0; }
    public int getAccessibilityFocusColor() { jet.runtime.ShimStubs.hit("android.view.accessibility.AccessibilityManager.getAccessibilityFocusColor()I"); return 0; }
    public static boolean isAccessibilityButtonSupported() { jet.runtime.ShimStubs.hit("android.view.accessibility.AccessibilityManager.isAccessibilityButtonSupported()Z"); return false; }
    // ---- END generated SDK surface ----
}
