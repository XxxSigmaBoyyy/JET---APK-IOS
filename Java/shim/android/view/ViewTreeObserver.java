package android.view;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
/** Per-view observer (Android merges these into the window's on attach). The harness dispatches all live observers
 * after each layout/draw pass, like ViewRootImpl.performTraversals(). */
public final class ViewTreeObserver {
    public interface OnGlobalFocusChangeListener { void onGlobalFocusChanged(View oldFocus, View newFocus); }
    public interface OnGlobalLayoutListener { void onGlobalLayout(); }
    public interface OnPreDrawListener { boolean onPreDraw(); }
    public interface OnDrawListener { void onDraw(); }
    public interface OnTouchModeChangeListener { void onTouchModeChanged(boolean isInTouchMode); }
    public interface OnScrollChangedListener { void onScrollChanged(); }
    public interface OnWindowFocusChangeListener { void onWindowFocusChanged(boolean hasFocus); }
    public interface OnWindowAttachListener { void onWindowAttached(); void onWindowDetached(); }
    public interface OnWindowVisibilityChangeListener { void onWindowVisibilityChanged(int visibility); }
    private static final Set<ViewTreeObserver> LIVE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private final List<OnGlobalFocusChangeListener> focus = new CopyOnWriteArrayList<>();
    private final List<OnGlobalLayoutListener> global = new CopyOnWriteArrayList<>();
    private final List<OnPreDrawListener> pre = new CopyOnWriteArrayList<>();
    private final List<OnDrawListener> draw = new CopyOnWriteArrayList<>();
    private final List<OnTouchModeChangeListener> touch = new CopyOnWriteArrayList<>();
    private final List<OnScrollChangedListener> scroll = new CopyOnWriteArrayList<>();
    private final List<OnWindowFocusChangeListener> wfocus = new CopyOnWriteArrayList<>();
    private final List<OnWindowAttachListener> attach = new CopyOnWriteArrayList<>();
    private boolean alive = true;
    public ViewTreeObserver() { LIVE.add(this); }
    public ViewTreeObserver(android.content.Context c) { this(); }
    public boolean isAlive() { return alive; }
    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener l) { focus.add(l); } public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener l) { focus.remove(l); }
    public void addOnGlobalLayoutListener(OnGlobalLayoutListener l) { global.add(l); } public void removeOnGlobalLayoutListener(OnGlobalLayoutListener l) { global.remove(l); }
    @Deprecated public void removeGlobalOnLayoutListener(OnGlobalLayoutListener l) { global.remove(l); }
    public void addOnPreDrawListener(OnPreDrawListener l) { pre.add(l); } public void removeOnPreDrawListener(OnPreDrawListener l) { pre.remove(l); }
    public void addOnDrawListener(OnDrawListener l) { draw.add(l); } public void removeOnDrawListener(OnDrawListener l) { draw.remove(l); }
    public void addOnTouchModeChangeListener(OnTouchModeChangeListener l) { touch.add(l); } public void removeOnTouchModeChangeListener(OnTouchModeChangeListener l) { touch.remove(l); }
    public void addOnScrollChangedListener(OnScrollChangedListener l) { scroll.add(l); } public void removeOnScrollChangedListener(OnScrollChangedListener l) { scroll.remove(l); }
    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener l) { wfocus.add(l); } public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener l) { wfocus.remove(l); }
    public void addOnWindowAttachListener(OnWindowAttachListener l) { attach.add(l); } public void removeOnWindowAttachListener(OnWindowAttachListener l) { attach.remove(l); }
    public final void dispatchOnGlobalLayout() { for (OnGlobalLayoutListener l : global) l.onGlobalLayout(); }
    public final boolean dispatchOnPreDraw() { boolean cancel = false; for (OnPreDrawListener l : pre) cancel |= !l.onPreDraw(); return cancel; }
    public final void dispatchOnDraw() { for (OnDrawListener l : draw) l.onDraw(); }
    public final void dispatchOnScrollChanged() { for (OnScrollChangedListener l : scroll) l.onScrollChanged(); }
    public void jetDispatchWindowFocus(boolean f) { for (OnWindowFocusChangeListener l : wfocus) l.onWindowFocusChanged(f); }
    public void jetDispatchAttached() { for (OnWindowAttachListener l : attach) l.onWindowAttached(); }
    private static List<ViewTreeObserver> live() { synchronized (LIVE) { return new ArrayList<>(LIVE); } }
    /** Harness traversal hooks: returns number of listeners invoked. */
    public static int jetTraversal() {
        int n = 0;
        for (ViewTreeObserver o : live()) { n += o.global.size(); o.dispatchOnGlobalLayout(); }
        for (ViewTreeObserver o : live()) { n += o.pre.size(); o.dispatchOnPreDraw(); }
        for (ViewTreeObserver o : live()) { n += o.draw.size(); o.dispatchOnDraw(); }
        return n;
    }
}
