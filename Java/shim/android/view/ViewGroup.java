package android.view;
import android.content.Context;
import android.util.AttributeSet;
import java.util.ArrayList;

public class ViewGroup extends View implements ViewParent, ViewManager {
    /** AOSP: the default style (defStyleAttr theme attr / defStyleRes) supplies attributes the element does not set. */
    public ViewGroup(Context c, AttributeSet a, int defStyleAttr) { super(c, a, defStyleAttr, 0); }
    public ViewGroup(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }

    public static class LayoutParams {
        public static final int MATCH_PARENT = -1, FILL_PARENT = -1, WRAP_CONTENT = -2;
        public int width, height;
        // merged Margin/LinearLayout params to keep the prototype small
        public int leftMargin, topMargin, rightMargin, bottomMargin;
        public float weight;
        public int gravity = -1;
        public LayoutParams(int w, int h) { width = w; height = h; }
        public LayoutParams(LayoutParams o) { this(o.width, o.height); leftMargin=o.leftMargin; topMargin=o.topMargin; rightMargin=o.rightMargin; bottomMargin=o.bottomMargin; weight=o.weight; gravity=o.gravity; }
        public LayoutParams(Context c, AttributeSet a) {
            this(WRAP_CONTENT, WRAP_CONTENT);
            if (a instanceof jet.runtime.XmlAttrs && c != null && c.getResources() != null) {
                jet.runtime.XmlAttrs x = (jet.runtime.XmlAttrs) a; jet.runtime.ResTable t = c.getResources().table(); String v;
                try {
                    if ((v = x.get("layout_width")) != null) width = (int) t.dimen(v);
                    if ((v = x.get("layout_height")) != null) height = (int) t.dimen(v);
                    if ((v = x.get("layout_weight")) != null) weight = Float.parseFloat(t.resolve(v));
                    if ((v = x.get("layout_gravity")) != null) gravity = jet.runtime.ResTable.intVal(t.resolve(v));
                    if ((v = x.get("layout_margin")) != null) { int m = (int) t.dimen(v); leftMargin = topMargin = rightMargin = bottomMargin = m; }
                    if ((v = x.get("layout_marginHorizontal")) != null) { int m = (int) t.dimen(v); leftMargin = rightMargin = m; }
                    if ((v = x.get("layout_marginVertical")) != null) { int m = (int) t.dimen(v); topMargin = bottomMargin = m; }
                    if ((v = x.get("layout_marginLeft")) != null || (v = x.get("layout_marginStart")) != null) leftMargin = (int) t.dimen(v);
                    if ((v = x.get("layout_marginTop")) != null) topMargin = (int) t.dimen(v);
                    if ((v = x.get("layout_marginRight")) != null || (v = x.get("layout_marginEnd")) != null) rightMargin = (int) t.dimen(v);
                    if ((v = x.get("layout_marginBottom")) != null) bottomMargin = (int) t.dimen(v);
                } catch (RuntimeException ignored) {}
            }
        }
        public void resolveLayoutDirection(int d) {}
        protected void setBaseAttributes(android.content.res.TypedArray a, int w, int h) { width = a.getLayoutDimension(w, "layout_width"); height = a.getLayoutDimension(h, "layout_height"); }
    }
    public static class MarginLayoutParams extends LayoutParams {
        public MarginLayoutParams(int w, int h) { super(w, h); }
        public MarginLayoutParams(LayoutParams o) { super(o); }
        public MarginLayoutParams(Context c, AttributeSet a) { super(c, a); }
        public void setMargins(int l, int t, int r, int b) { leftMargin=l; topMargin=t; rightMargin=r; bottomMargin=b; }
        public MarginLayoutParams(MarginLayoutParams o) { super(o); }
        public void setMarginStart(int m) { leftMargin = m; } public void setMarginEnd(int m) { rightMargin = m; }
        public int getMarginStart() { return leftMargin; } public int getMarginEnd() { return rightMargin; }
        public boolean isMarginRelative() { return false; } public int getLayoutDirection() { return 0; } public void setLayoutDirection(int d) {}
    }
    protected final ArrayList<View> mChildren = new ArrayList<>();
    public ViewGroup(Context c) { super(c); }
    public ViewGroup(Context c, AttributeSet a) { super(c, a); }
    public interface OnHierarchyChangeListener { void onChildViewAdded(View parent, View child); void onChildViewRemoved(View parent, View child); }
    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000, FOCUS_AFTER_DESCENDANTS = 0x40000, FOCUS_BLOCK_DESCENDANTS = 0x60000;
    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0, LAYOUT_MODE_OPTICAL_BOUNDS = 1;
    private OnHierarchyChangeListener mHierarchy;
    private int mDescendantFocusability = FOCUS_BEFORE_DESCENDANTS;
    private boolean mClipChildren = true, mClipToPadding = true;
    public void addView(View v) { addView(v, -1); }
    public void addView(View v, int index) { ViewGroup.LayoutParams p = v.getLayoutParams(); addView(v, index, p == null ? generateDefaultLayoutParams() : p); }
    public void addView(View v, int w, int h) { LayoutParams p = generateDefaultLayoutParams(); p.width = w; p.height = h; addView(v, -1, p); }
    public void addView(View v, ViewGroup.LayoutParams lp) { addView(v, -1, lp); }
    public void addView(View v, int index, ViewGroup.LayoutParams lp) { requestLayout(); invalidate(); addViewInner(v, index, lp); }
    private void addViewInner(View v, int index, ViewGroup.LayoutParams lp) {
        if (v.mParent != null) throw new IllegalStateException("The specified child already has a parent. You must call removeView() on the child's parent first.");
        if (lp == null) lp = generateDefaultLayoutParams();
        if (!checkLayoutParams(lp)) lp = generateLayoutParams(lp);
        v.mLayoutParams = lp;
        v.mParent = this;
        lp.resolveLayoutDirection(v.getLayoutDirection());
        if (index < 0 || index > mChildren.size()) mChildren.add(v); else mChildren.add(index, v);
        onViewAdded(v);
        if (mHierarchy != null) mHierarchy.onChildViewAdded(this, v);
        if (isAttachedToWindow()) jetAttachTree(v);
    }
    protected boolean addViewInLayout(View v, int index, LayoutParams p) { addViewInner(v, index, p); return true; }
    protected boolean addViewInLayout(View v, int index, LayoutParams p, boolean pre) { return addViewInLayout(v, index, p); }
    public static void jetAttachTree(View v) { v.jetDispatchAttached(); if (v instanceof ViewGroup) for (View c : new ArrayList<>(((ViewGroup) v).mChildren)) jetAttachTree(c); }
    public void removeView(View v) { if (mChildren.remove(v)) { v.mParent = null; onViewRemoved(v); if (mHierarchy != null) mHierarchy.onChildViewRemoved(this, v); } requestLayout(); }
    public void removeViewInLayout(View v) { removeView(v); }
    public void removeViewsInLayout(int start, int count) { removeViews(start, count); }
    public void removeViews(int start, int count) { for (int i = 0; i < count && start < mChildren.size(); i++) removeView(mChildren.get(start)); }
    public void removeAllViews() { for (View v : new ArrayList<>(mChildren)) removeView(v); }
    public void removeAllViewsInLayout() { removeAllViews(); }
    public void removeViewAt(int i) { removeView(mChildren.get(i)); }
    protected void detachViewFromParent(View v) { mChildren.remove(v); v.mParent = null; }
    protected void detachViewFromParent(int i) { detachViewFromParent(mChildren.get(i)); }
    protected void detachAllViewsFromParent() { for (View v : mChildren) v.mParent = null; mChildren.clear(); }
    protected void attachViewToParent(View v, int index, LayoutParams p) { v.mLayoutParams = p; v.mParent = this; if (index < 0 || index > mChildren.size()) mChildren.add(v); else mChildren.add(index, v); }
    protected void removeDetachedView(View v, boolean animate) {}
    public void onViewAdded(View v) {}
    public void onViewRemoved(View v) {}
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener l) { mHierarchy = l; }
    public int indexOfChild(View v) { return mChildren.indexOf(v); }
    public void bringChildToFront(View v) { if (mChildren.remove(v)) mChildren.add(v); }
    public boolean shouldDelayChildPressedState() { return true; }
    private boolean mDisallowIntercept; private View mTouchTarget;
    public void requestDisallowInterceptTouchEvent(boolean d) { mDisallowIntercept = d; if (mParent != null) mParent.requestDisallowInterceptTouchEvent(d); }
    /** ViewGroup.dispatchTouchEvent semantics: intercept check, DOWN hit-test children top-most first, then stick to the target. */
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        View focused = getFocusedChild();
        if (focused != null && focused.dispatchKeyEvent(event)) return true;
        return super.dispatchKeyEvent(event);
    }
    public boolean dispatchTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) { mTouchTarget = null; mDisallowIntercept = false; }
        boolean intercepted = (a == MotionEvent.ACTION_DOWN || mTouchTarget != null) ? (!mDisallowIntercept && onInterceptTouchEvent(e)) : true;
        if (intercepted && mTouchTarget != null) {
            MotionEvent c = MotionEvent.obtain(e); c.setAction(MotionEvent.ACTION_CANCEL); toChild(mTouchTarget, c); mTouchTarget = null; return true;
        }
        if (!intercepted && a == MotionEvent.ACTION_DOWN) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View ch = getChildAt(i); if (ch == null || ch.getVisibility() != VISIBLE) continue;
                float cx = e.getX() + getScrollX() - ch.getLeft() - ch.getTranslationX(), cy = e.getY() + getScrollY() - ch.getTop() - ch.getTranslationY();
                if (cx < 0 || cy < 0 || cx >= ch.getWidth() || cy >= ch.getHeight()) continue;
                if (toChild(ch, e)) { mTouchTarget = ch; return true; }
            }
        }
        if (mTouchTarget == null) return super.dispatchTouchEvent(e);
        boolean r = toChild(mTouchTarget, e);
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) mTouchTarget = null;
        return r;
    }
    private boolean toChild(View ch, MotionEvent e) {
        float dx = getScrollX() - ch.getLeft() - ch.getTranslationX(), dy = getScrollY() - ch.getTop() - ch.getTranslationY();
        e.offsetLocation(dx, dy); try { return ch.dispatchTouchEvent(e); } finally { e.offsetLocation(-dx, -dy); }
    }
    public boolean onInterceptTouchEvent(android.view.MotionEvent e) { return false; }
    @Override public View findFocus() { if (isFocused()) return this; for (int i = 0; i < getChildCount(); i++) { View f = getChildAt(i).findFocus(); if (f != null) return f; } return null; }
    @Override protected void dispatchSaveInstanceState(android.util.SparseArray<android.os.Parcelable> c) {
        super.dispatchSaveInstanceState(c);
        for (int i = 0; i < getChildCount(); i++) { View ch = getChildAt(i); if (ch.isSaveFromParentEnabled()) ch.dispatchSaveInstanceState(c); }
    }
    protected void dispatchFreezeSelfOnly(android.util.SparseArray<android.os.Parcelable> c) { super.dispatchSaveInstanceState(c); }
    @Override protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> c) {
        super.dispatchRestoreInstanceState(c);
        for (int i = 0; i < getChildCount(); i++) { View ch = getChildAt(i); if (ch.isSaveFromParentEnabled()) ch.dispatchRestoreInstanceState(c); }
    }
    protected void dispatchThawSelfOnly(android.util.SparseArray<android.os.Parcelable> c) { super.dispatchRestoreInstanceState(c); }
    public View getFocusedChild() { for (int i = 0; i < getChildCount(); i++) { View c = getChildAt(i); if (c.findFocus() != null) return c; } return null; }
    /** AOSP ViewGroup.requestFocus: honour descendantFocusability (before / after / block descendants). */
    @Override public boolean requestFocus(int dir, android.graphics.Rect r) {
        int d = getDescendantFocusability();
        if (d == FOCUS_BLOCK_DESCENDANTS) return jetRequestFocusNoSearch(dir, r);
        if (d == FOCUS_BEFORE_DESCENDANTS) return jetRequestFocusNoSearch(dir, r) || onRequestFocusInDescendants(dir, r);
        return onRequestFocusInDescendants(dir, r) || jetRequestFocusNoSearch(dir, r);
    }
    protected boolean onRequestFocusInDescendants(int dir, android.graphics.Rect r) {
        int n = getChildCount(); boolean back = dir == FOCUS_UP || dir == FOCUS_LEFT || dir == FOCUS_BACKWARD;
        for (int k = 0; k < n; k++) { View c = getChildAt(back ? n - 1 - k : k); if (c.getVisibility() == VISIBLE && c.requestFocus(dir, r)) return true; }
        return false;
    }
    public void setDescendantFocusability(int f) { mDescendantFocusability = f; } public int getDescendantFocusability() { return mDescendantFocusability; }
    public void setClipChildren(boolean c) { mClipChildren = c; } public boolean getClipChildren() { return mClipChildren; }
    public void setClipToPadding(boolean c) { mClipToPadding = c; } public boolean getClipToPadding() { return mClipToPadding; }
    public void setMotionEventSplittingEnabled(boolean e) {} public boolean isMotionEventSplittingEnabled() { return false; }
    public void setAddStatesFromChildren(boolean a) {} public boolean addStatesFromChildren() { return false; }
    private android.animation.LayoutTransition mLayoutTransition; public void setLayoutTransition(android.animation.LayoutTransition t) { mLayoutTransition = t; } public android.animation.LayoutTransition getLayoutTransition() { return mLayoutTransition; }
    public void setLayoutAnimation(android.view.animation.LayoutAnimationController c) {} public android.view.animation.LayoutAnimationController getLayoutAnimation() { return null; } public void scheduleLayoutAnimation() {}
    public void setTouchscreenBlocksFocus(boolean b) {} public void setTransitionGroup(boolean g) {}
    public void setLayoutMode(int m) {} public int getLayoutMode() { return 0; }
    private boolean mChildrenDrawingOrder; protected void setChildrenDrawingOrderEnabled(boolean e) { mChildrenDrawingOrder = e; } protected int getChildDrawingOrder(int count, int i) { return i; }
    public void childDrawableStateChanged(View c) {}
    public void startViewTransition(View v) {} public void endViewTransition(View v) {}
    public void focusableViewAvailable(View v) {}
    public void recomputeViewAttributes(View v) {}
    public boolean onRequestSendAccessibilityEvent(View c, Object e) { return true; }
    public void requestChildFocus(View child, View focused) {}
    public void clearChildFocus(View c) {}
    public void childHasTransientStateChanged(View c, boolean t) {}
    public void onDescendantInvalidated(View child, View target) {}
    public boolean showContextMenuForChild(View v) { return false; }
    public void requestTransparentRegion(View c) {}
    public void invalidateChild(View c, android.graphics.Rect r) { invalidate(); }
    public boolean getChildVisibleRect(View c, android.graphics.Rect r, android.graphics.Point p) { return true; }
    public void setPersistentDrawingCache(int c) {}
    public boolean isTransitionGroup() { return false; }
    public boolean onStartNestedScroll(View c, View t, int axes) { return false; }
    public void onNestedScrollAccepted(View c, View t, int axes) {} public void onStopNestedScroll(View t) {}
    public void onNestedScroll(View t, int a, int b, int c, int d) {} public void onNestedPreScroll(View t, int dx, int dy, int[] consumed) {}
    public boolean onNestedFling(View t, float vx, float vy, boolean c) { return false; } public boolean onNestedPreFling(View t, float vx, float vy) { return false; }
    public int getNestedScrollAxes() { return 0; }
    protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); }
    public LayoutParams generateLayoutParams(AttributeSet a) { return new LayoutParams(getContext(), a); }
    protected LayoutParams generateLayoutParams(LayoutParams p) { return p; }
    protected boolean checkLayoutParams(LayoutParams p) { return p != null; }
    protected void measureChildren(int ws, int hs) { for (View c : mChildren) if (c.getVisibility() != GONE) measureChild(c, ws, hs); }
    protected void measureChild(View c, int ws, int hs) {
        LayoutParams lp = c.getLayoutParams();
        c.measure(getChildMeasureSpec(ws, getPaddingLeft() + getPaddingRight(), lp.width), getChildMeasureSpec(hs, getPaddingTop() + getPaddingBottom(), lp.height));
    }
    protected void measureChildWithMargins(View c, int ws, int wUsed, int hs, int hUsed) {
        LayoutParams lp = c.getLayoutParams();
        c.measure(getChildMeasureSpec(ws, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + wUsed, lp.width),
                  getChildMeasureSpec(hs, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + hUsed, lp.height));
    }
    protected boolean drawChild(android.graphics.Canvas cv, View c, long t) {
        int save = cv.save();
        try {
            cv.translate(c.getLeft() - getScrollX(), c.getTop() - getScrollY());
            android.graphics.Matrix cm = c.getMatrix();
            if (!cm.isIdentity()) cv.concat(cm);
            cv.clipRect(0, 0, c.getWidth(), c.getHeight());
            c.draw(cv);
        } finally { cv.restoreToCount(save); }
        return false;
    }
    public int getChildCount() { return mChildren.size(); }
    public View getChildAt(int i) { return i < 0 || i >= mChildren.size() ? null : mChildren.get(i); } // Android: null when out of range
    @Override protected View findViewTraversal(int id) {
        for (View c : mChildren) { View r = c.findViewById(id); if (r != null) return r; }
        return null;
    }
    public static int getChildMeasureSpec(int spec, int padding, int dim) {
        int mode = MeasureSpec.getMode(spec), size = Math.max(0, MeasureSpec.getSize(spec) - padding);
        if (dim >= 0) return MeasureSpec.makeMeasureSpec(dim, MeasureSpec.EXACTLY);
        if (mode == MeasureSpec.EXACTLY) return MeasureSpec.makeMeasureSpec(size, dim == -1 ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
        if (mode == MeasureSpec.AT_MOST) return MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST);
        return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }
    @Override protected void dispatchDraw(android.graphics.Canvas cv) {
        long t = android.os.SystemClock.uptimeMillis();
        for (View c : jetDrawingOrder()) {
            if (c.getVisibility() != VISIBLE) continue;
            c.computeScroll();  // View.draw(Canvas, ViewGroup, long): scrollers advance in (virtual) draw time
            if (c.getVisibility() != VISIBLE) continue;
            drawChild(cv, c, t);
        }
    }
    /** AOSP buildOrderedChildList: custom drawing order (isChildrenDrawingOrderEnabled), then stable sort by Z. */
    private java.util.List<View> jetDrawingOrder() {
        ArrayList<View> l = new ArrayList<>(mChildren); int n = l.size();
        if (mChildrenDrawingOrder && n > 1) { ArrayList<View> o = new ArrayList<>(n); for (int i = 0; i < n; i++) { int j = getChildDrawingOrder(n, i); o.add(j >= 0 && j < n ? l.get(j) : l.get(i)); } l = o; }
        boolean z = false; for (View c : l) if (c.getZ() != 0) { z = true; break; }
        if (z) l.sort((a, b) -> Float.compare(a.getZ(), b.getZ()));
        return l;
    }
    /** Deepest clickable view under (x,y) in this group's coordinates. */
    public View hitTest(int x, int y) {
        for (int i = mChildren.size() - 1; i >= 0; i--) {
            View c = mChildren.get(i);
            if (x >= c.getLeft() && x < c.getLeft() + c.getWidth() && y >= c.getTop() && y < c.getTop() + c.getHeight()) {
                if (c instanceof ViewGroup) { View h = ((ViewGroup) c).hitTest(x - c.getLeft(), y - c.getTop()); if (h != null) return h; }
                if (c.isClickableForJet()) return c;
            }
        }
        return null;
    }
    public boolean requestChildRectangleOnScreen(View child, android.graphics.Rect rectangle, boolean immediate) { return false; /* AOSP ViewGroup default; scrolling containers override */ }
    @Override public boolean hasTransientState() { if (super.hasTransientState()) return true; for (int i = 0; i < getChildCount(); i++) { View c = getChildAt(i); if (c != null && c.hasTransientState()) return true; } return false; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.view.animation.Animation.AnimationListener jet_LayoutAnimationListener;
    private boolean jet_AlwaysDrawnWithCacheEnabled;
    private boolean jet_AnimationCacheEnabled;
    private boolean jet_ChildrenDrawnWithCacheEnabled;
    public boolean showContextMenuForChild(android.view.View p0, float p1, float p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.showContextMenuForChild(Landroid/view/View;FF)Z"); return false; }
    public android.view.ActionMode startActionModeForChild(android.view.View p0, android.view.ActionMode.Callback p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.startActionModeForChild(Landroid/view/View;Landroid/view/ActionMode$Callback;)Landroid/view/ActionMode;"); return null; }
    public android.view.ActionMode startActionModeForChild(android.view.View p0, android.view.ActionMode.Callback p1, int p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.startActionModeForChild(Landroid/view/View;Landroid/view/ActionMode$Callback;I)Landroid/view/ActionMode;"); return null; }
    public android.view.View focusSearch(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.focusSearch(Landroid/view/View;I)Landroid/view/View;"); return null; }
    public boolean requestSendAccessibilityEvent(android.view.View p0, android.view.accessibility.AccessibilityEvent p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.requestSendAccessibilityEvent(Landroid/view/View;Landroid/view/accessibility/AccessibilityEvent;)Z"); return false; }
    public boolean onRequestSendAccessibilityEvent(android.view.View p0, android.view.accessibility.AccessibilityEvent p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.onRequestSendAccessibilityEvent(Landroid/view/View;Landroid/view/accessibility/AccessibilityEvent;)Z"); return false; }
    public boolean dispatchUnhandledMove(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchUnhandledMove(Landroid/view/View;I)Z"); return false; }
    public void addFocusables(java.util.ArrayList p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.addFocusables(Ljava/util/ArrayList;II)V"); }
    public void addKeyboardNavigationClusters(java.util.Collection p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.addKeyboardNavigationClusters(Ljava/util/Collection;I)V"); }
    public boolean getTouchscreenBlocksFocus() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.getTouchscreenBlocksFocus()Z"); return false; }
    public void findViewsWithText(java.util.ArrayList p0, java.lang.CharSequence p1, int p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.findViewsWithText(Ljava/util/ArrayList;Ljava/lang/CharSequence;I)V"); }
    @Override public void dispatchWindowFocusChanged(boolean f) { super.dispatchWindowFocusChanged(f); for (View c : new ArrayList<>(mChildren)) c.dispatchWindowFocusChanged(f); }
    public void addTouchables(java.util.ArrayList p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.addTouchables(Ljava/util/ArrayList;)V"); }
    public void dispatchDisplayHint(int p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchDisplayHint(I)V"); }
    protected void dispatchVisibilityChanged(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchVisibilityChanged(Landroid/view/View;I)V"); }
    public void dispatchWindowVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchWindowVisibilityChanged(I)V"); }
    public void dispatchConfigurationChanged(android.content.res.Configuration p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchConfigurationChanged(Landroid/content/res/Configuration;)V"); }
    public boolean dispatchDragEvent(android.view.DragEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchDragEvent(Landroid/view/DragEvent;)Z"); return false; }
    public void dispatchWindowSystemUiVisiblityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchWindowSystemUiVisiblityChanged(I)V"); }
    public void dispatchSystemUiVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchSystemUiVisibilityChanged(I)V"); }
    public boolean dispatchKeyEventPreIme(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchKeyEventPreIme(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean dispatchKeyShortcutEvent(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchKeyShortcutEvent(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean dispatchTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean dispatchCapturedPointerEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchCapturedPointerEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public void dispatchPointerCaptureChanged(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchPointerCaptureChanged(Z)V"); }
    public android.view.PointerIcon onResolvePointerIcon(android.view.MotionEvent p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.onResolvePointerIcon(Landroid/view/MotionEvent;I)Landroid/view/PointerIcon;"); return null; }
    protected boolean dispatchHoverEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchHoverEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public void addChildrenForAccessibility(java.util.ArrayList p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.addChildrenForAccessibility(Ljava/util/ArrayList;)V"); }
    public boolean onInterceptHoverEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.onInterceptHoverEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected boolean dispatchGenericPointerEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchGenericPointerEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected boolean dispatchGenericFocusedEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchGenericFocusedEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean restoreDefaultFocus() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.restoreDefaultFocus()Z"); return false; }
    public void dispatchStartTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchStartTemporaryDetach()V"); }
    public void dispatchFinishTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchFinishTemporaryDetach()V"); }
    public java.lang.CharSequence getAccessibilityClassName() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.getAccessibilityClassName()Ljava/lang/CharSequence;"); return null; }
    public void notifySubtreeAccessibilityStateChanged(android.view.View p0, android.view.View p1, int p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.notifySubtreeAccessibilityStateChanged(Landroid/view/View;Landroid/view/View;I)V"); }
    public boolean onNestedPrePerformAccessibilityAction(android.view.View p0, int p1, android.os.Bundle p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.onNestedPrePerformAccessibilityAction(Landroid/view/View;ILandroid/os/Bundle;)Z"); return false; }
    protected void setChildrenDrawingCacheEnabled(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.setChildrenDrawingCacheEnabled(Z)V"); }
    public final int getChildDrawingOrder(int drawingPosition) { return isChildrenDrawingOrderEnabled() ? getChildDrawingOrder(getChildCount(), drawingPosition) : drawingPosition; }
    public void dispatchSetSelected(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchSetSelected(Z)V"); }
    public void dispatchSetActivated(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchSetActivated(Z)V"); }
    protected void dispatchSetPressed(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchSetPressed(Z)V"); }
    public void dispatchDrawableHotspotChanged(float p0, float p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchDrawableHotspotChanged(FF)V"); }
    protected void setStaticTransformationsEnabled(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.setStaticTransformationsEnabled(Z)V"); }
    /** AOSP ViewManager.updateViewLayout: the view must be a child; validate params via checkLayoutParams, then setLayoutParams. */
    public void updateViewLayout(android.view.View view, android.view.ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        if (view.getParent() != this) throw new IllegalArgumentException("Given view not a child of " + this);
        view.setLayoutParams(params);
    }
    protected void cleanupLayoutState(android.view.View p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.cleanupLayoutState(Landroid/view/View;)V"); }
    protected void attachLayoutAnimationParameters(android.view.View p0, android.view.ViewGroup.LayoutParams p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.attachLayoutAnimationParameters(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;II)V"); }
    protected void detachViewsFromParent(int p0, int p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.detachViewsFromParent(II)V"); }
    public android.view.ViewParent invalidateChildInParent(int[] p0, android.graphics.Rect p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.invalidateChildInParent([ILandroid/graphics/Rect;)Landroid/view/ViewParent;"); return null; }
    /** AOSP: walk from the descendant up to this group, adding each view's position minus its scroll. */
    public final void offsetDescendantRectToMyCoords(View d, android.graphics.Rect r) { jetOffsetRect(d, r, true); }
    public final void offsetRectIntoDescendantCoords(View d, android.graphics.Rect r) { jetOffsetRect(d, r, false); }
    private void jetOffsetRect(View d, android.graphics.Rect r, boolean toMine) {
        if (d == this) return; View v = d; android.view.ViewParent p = d.getParent();
        while (p instanceof View && p != this) { int dx = v.getLeft() - v.getScrollX(), dy = v.getTop() - v.getScrollY(); if (toMine) r.offset(dx, dy); else r.offset(-dx, -dy); v = (View) p; p = v.getParent(); }
        if (p != this) throw new IllegalArgumentException("parameter must be a descendant of this view");
        int dx = v.getLeft() - v.getScrollX(), dy = v.getTop() - v.getScrollY(); if (toMine) r.offset(dx, dy); else r.offset(-dx, -dy);
    }
    protected boolean canAnimate() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.canAnimate()Z"); return false; }
    public void startLayoutAnimation() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.startLayoutAnimation()V"); }
    public boolean isAnimationCacheEnabled() { return jet_AnimationCacheEnabled; }
    public void setAnimationCacheEnabled(boolean p0) { jet_AnimationCacheEnabled = p0; }
    public boolean isAlwaysDrawnWithCacheEnabled() { return jet_AlwaysDrawnWithCacheEnabled; }
    public void setAlwaysDrawnWithCacheEnabled(boolean p0) { jet_AlwaysDrawnWithCacheEnabled = p0; }
    protected boolean isChildrenDrawnWithCacheEnabled() { return jet_ChildrenDrawnWithCacheEnabled; }
    protected void setChildrenDrawnWithCacheEnabled(boolean p0) { jet_ChildrenDrawnWithCacheEnabled = p0; }
    protected boolean isChildrenDrawingOrderEnabled() { return mChildrenDrawingOrder; }
    public int getPersistentDrawingCache() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.getPersistentDrawingCache()I"); return 0; }
    protected void debug(int p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.debug(I)V"); }
    public void clearDisappearingChildren() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.clearDisappearingChildren()V"); }
    public void suppressLayout(boolean p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.suppressLayout(Z)V"); }
    public boolean isLayoutSuppressed() { jet.runtime.ShimStubs.hit("android.view.ViewGroup.isLayoutSuppressed()Z"); return false; }
    public boolean gatherTransparentRegion(android.graphics.Region p0) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.gatherTransparentRegion(Landroid/graphics/Region;)Z"); return false; }
    public android.view.WindowInsets dispatchWindowInsetsAnimationProgress(android.view.WindowInsets p0, java.util.List p1) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchWindowInsetsAnimationProgress(Landroid/view/WindowInsets;Ljava/util/List;)Landroid/view/WindowInsets;"); return null; }
    public void dispatchScrollCaptureSearch(android.graphics.Rect p0, android.graphics.Point p1, java.util.function.Consumer p2) { jet.runtime.ShimStubs.hit("android.view.ViewGroup.dispatchScrollCaptureSearch(Landroid/graphics/Rect;Landroid/graphics/Point;Ljava/util/function/Consumer;)V"); }
    public android.view.animation.Animation.AnimationListener getLayoutAnimationListener() { return jet_LayoutAnimationListener; }
    public void setLayoutAnimationListener(android.view.animation.Animation.AnimationListener p0) { jet_LayoutAnimationListener = p0; }
    // ---- END generated SDK surface ----
}
