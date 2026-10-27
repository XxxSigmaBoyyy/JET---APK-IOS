package android.view;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;

public class View implements Drawable.Callback {
    public View(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public View(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        mContext = c;
        XmlAttrs x = XmlAttrs.of(a);
        java.util.Map<String, String> d = (defStyleAttr | defStyleRes) != 0 ? XmlAttrs.defStyle(c, defStyleAttr, defStyleRes) : java.util.Collections.emptyMap();
        if (!d.isEmpty()) x = x == null ? XmlAttrs.defaultsOnly(d) : x.withDefaults(d);
        if (x != null && c != null) { try { x = x.themed(c.getTheme()); } catch (Throwable ignored) {} }
        mJetAttrs = x;
        if (x != null) applyAttrs(x);
    }
    /** Effective attributes (element, style=, defStyle) for shim subclasses; null for code-created views without a default style. */
    protected XmlAttrs mJetAttrs;

    public interface OnClickListener { void onClick(View v); }
    /** android:onClick="name": resolved lazily on the context chain (AOSP View.DeclaredOnClickListener). */
    private static final class DeclaredOnClickListener implements OnClickListener {
        private final View mHost; private final String mName; private java.lang.reflect.Method mMethod; private Object mTarget;
        DeclaredOnClickListener(View host, String name) { mHost = host; mName = name; }
        @Override public void onClick(View v) {
            if (mMethod == null) {
                Object ctx = mHost.getContext();
                while (ctx != null) {
                    for (Class<?> k = ctx.getClass(); k != null && mMethod == null; k = k.getSuperclass()) {
                        try { java.lang.reflect.Method m = k.getDeclaredMethod(mName, View.class); m.setAccessible(true); mMethod = m; mTarget = ctx; } catch (NoSuchMethodException ignored) {}
                    }
                    if (mMethod != null) break;
                    ctx = ctx instanceof android.content.ContextWrapper ? ((android.content.ContextWrapper) ctx).getBaseContext() : null;
                }
                if (mMethod == null) throw new IllegalStateException("Could not find method " + mName + "(View) in a parent or ancestor Context for android:onClick attribute defined on view " + mHost.getClass());
            }
            try { mMethod.invoke(mTarget, v); }
            catch (IllegalAccessException e) { throw new IllegalStateException("Could not execute non-public method for android:onClick", e); }
            catch (java.lang.reflect.InvocationTargetException e) { Throwable c = e.getCause(); if (c instanceof RuntimeException) throw (RuntimeException) c; if (c instanceof Error) throw (Error) c; throw new IllegalStateException("Could not execute method for android:onClick", c); }
        }
    }
    public interface OnLongClickListener { boolean onLongClick(View v); }
    public interface OnTouchListener { boolean onTouch(View v, MotionEvent event); }
    public interface OnKeyListener { boolean onKey(View v, int keyCode, KeyEvent event); }
    public interface OnFocusChangeListener { void onFocusChange(View v, boolean hasFocus); }
    public static final int VISIBLE = 0, INVISIBLE = 4, GONE = 8;
    public static final int NO_ID = -1, LAYOUT_DIRECTION_LTR = 0, LAYOUT_DIRECTION_RTL = 1, LAYOUT_DIRECTION_INHERIT = 2, LAYOUT_DIRECTION_LOCALE = 3;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0, IMPORTANT_FOR_ACCESSIBILITY_YES = 1, IMPORTANT_FOR_ACCESSIBILITY_NO = 2, IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int LAYER_TYPE_NONE = 0, LAYER_TYPE_SOFTWARE = 1, LAYER_TYPE_HARDWARE = 2;
    public static final int OVER_SCROLL_ALWAYS = 0, OVER_SCROLL_IF_CONTENT_SCROLLS = 1, OVER_SCROLL_NEVER = 2;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0, SYSTEM_UI_FLAG_LOW_PROFILE = 1, SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2, SYSTEM_UI_FLAG_FULLSCREEN = 4,
        SYSTEM_UI_FLAG_LAYOUT_STABLE = 256, SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512, SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024, SYSTEM_UI_FLAG_IMMERSIVE = 2048, SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096, SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 8192;
    public static final int MEASURED_STATE_MASK = 0xff000000, MEASURED_SIZE_MASK = 0x00ffffff, MEASURED_STATE_TOO_SMALL = 0x01000000, MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int FOCUSABLE_AUTO = 16, FOCUSABLE = 1, NOT_FOCUSABLE = 0, TEXT_ALIGNMENT_GRAVITY = 1, TEXT_DIRECTION_LOCALE = 5;
    public static final int[] EMPTY_STATE_SET = new int[0], ENABLED_STATE_SET = {16842910}, PRESSED_ENABLED_STATE_SET = {16842919, 16842910};
    public static final int[] PRESSED_STATE_SET = {16842919},
        FOCUSED_STATE_SET = {16842908},
        SELECTED_STATE_SET = {16842913},
        WINDOW_FOCUSED_STATE_SET = {16842909},
        PRESSED_FOCUSED_STATE_SET = {16842919, 16842908},
        PRESSED_SELECTED_STATE_SET = {16842919, 16842913},
        PRESSED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842909},
        ENABLED_FOCUSED_STATE_SET = {16842910, 16842908},
        ENABLED_SELECTED_STATE_SET = {16842910, 16842913},
        ENABLED_WINDOW_FOCUSED_STATE_SET = {16842910, 16842909},
        FOCUSED_SELECTED_STATE_SET = {16842908, 16842913},
        FOCUSED_WINDOW_FOCUSED_STATE_SET = {16842908, 16842909},
        SELECTED_WINDOW_FOCUSED_STATE_SET = {16842913, 16842909},
        PRESSED_ENABLED_FOCUSED_STATE_SET = {16842919, 16842910, 16842908},
        PRESSED_ENABLED_SELECTED_STATE_SET = {16842919, 16842910, 16842913},
        PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842910, 16842909},
        PRESSED_FOCUSED_SELECTED_STATE_SET = {16842919, 16842908, 16842913},
        PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842908, 16842909},
        PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842913, 16842909},
        ENABLED_FOCUSED_SELECTED_STATE_SET = {16842910, 16842908, 16842913},
        ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = {16842910, 16842908, 16842909},
        ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842910, 16842913, 16842909},
        FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842908, 16842913, 16842909},
        PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = {16842919, 16842910, 16842908, 16842913},
        PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842910, 16842908, 16842909},
        PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842910, 16842913, 16842909},
        PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842908, 16842913, 16842909},
        ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842910, 16842908, 16842913, 16842909},
        PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {16842919, 16842910, 16842908, 16842913, 16842909};
    public interface OnAttachStateChangeListener { void onViewAttachedToWindow(View v); void onViewDetachedFromWindow(View v); }
    public interface OnApplyWindowInsetsListener { WindowInsets onApplyWindowInsets(View v, WindowInsets insets); }
    public interface OnSystemUiVisibilityChangeListener { void onSystemUiVisibilityChange(int visibility); }
    public interface OnGenericMotionListener { boolean onGenericMotion(View v, MotionEvent event); }
    public interface OnHoverListener { boolean onHover(View v, MotionEvent event); }
    public interface OnCreateContextMenuListener { void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info); }
    public interface OnScrollChangeListener { void onScrollChange(View v, int x, int y, int ox, int oy); }
    public interface OnDragListener { boolean onDrag(View v, DragEvent e); }
    public interface OnLayoutChangeListener {
        void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom);
    }

    public static final class MeasureSpec {
        public static final int UNSPECIFIED = 0, EXACTLY = 1 << 30, AT_MOST = 2 << 30;
        private static final int MASK = 3 << 30;
        public static int makeMeasureSpec(int size, int mode) { return (size & ~MASK) | mode; }
        public static int getMode(int s) { return s & MASK; }
        public static int getSize(int s) { return s & ~MASK; }
        public static String toString(int s) { int m = getMode(s); return (m == EXACTLY ? "EXACTLY " : m == AT_MOST ? "AT_MOST " : "UNSPEC ") + getSize(s); }
    }

    protected final Context mContext;
    private ViewTreeObserver mTreeObserver;
    private int mId = -1;
    private Object mTag;
    private CharSequence mContentDescription;
    protected Drawable mBackground;
    protected int mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom;
    protected ViewGroup.LayoutParams mLayoutParams;   // null until attached/inflated, as on Android
    protected ViewGroup mParent;
    private OnClickListener mOnClick;
    private OnLongClickListener mOnLongClick;
    private OnTouchListener mOnTouch;
    private OnKeyListener mOnKey;
    private boolean mSelected, mPressed;
    int mLeft, mTop, mRight, mBottom;
    private int mMeasuredWidth, mMeasuredHeight;

    public View(Context c) { this(c, null); }
    public View(Context c, AttributeSet attrs) { this(c, attrs, 0, 0); }

    protected ResTable res() { return mContext.getResources().table(); }

    private float jetF(String v) { try { return v.startsWith("@") || v.startsWith("?") ? res().dimen(v) : Float.parseFloat(v); } catch (Throwable e) { return 0f; } }
    private void applyAttrs(XmlAttrs a) {
        ResTable t = res();
        String v;
        if ((v = a.get("id")) != null && v.startsWith("@")) mId = ResTable.ref(v);
        if ((v = a.get("scaleX")) != null) mScaleX = jetF(v);
        if ((v = a.get("scaleY")) != null) mScaleY = jetF(v);
        if ((v = a.get("rotation")) != null) mRotation = jetF(v);
        if ((v = a.get("translationX")) != null) mTranslationX = t.dimen(v);
        if ((v = a.get("translationY")) != null) mTranslationY = t.dimen(v);
        if ((v = a.get("alpha")) != null) mAlpha = jetF(v);
        if ((v = a.get("transformPivotX")) != null) { mPivotX = t.dimen(v); mPivotExplicit = true; }
        if ((v = a.get("transformPivotY")) != null) { mPivotY = t.dimen(v); mPivotExplicit = true; }
        if ((v = a.get("onClick")) != null && !v.isEmpty() && !v.startsWith("@")) { mOnClick = new DeclaredOnClickListener(this, v); setClickable(true); }
        if ((v = a.get("visibility")) != null) { String r = v.startsWith("@") || v.startsWith("?") ? t.resolve(v) : v; // enum: visible=0, invisible=1, gone=2
            if ("2".equals(r) || "gone".equals(r)) mVisibility = GONE; else if ("1".equals(r) || "invisible".equals(r)) mVisibility = INVISIBLE; }
        if ((v = a.get("background")) != null) { mBackground = t.drawable(v); if (mBackground == null && v.startsWith("?")) mBackground = jetThemeDrawable(v); }
        if ((v = a.get("padding")) != null) { int p = (int) t.dimen(v); mPaddingLeft = mPaddingTop = mPaddingRight = mPaddingBottom = p; }
        if ((v = a.get("paddingLeft")) != null) mPaddingLeft = (int) t.dimen(v);
        if ((v = a.get("paddingTop")) != null) mPaddingTop = (int) t.dimen(v);
        if ((v = a.get("paddingRight")) != null) mPaddingRight = (int) t.dimen(v);
        if ((v = a.get("paddingBottom")) != null) mPaddingBottom = (int) t.dimen(v);
        if ((v = a.get("contentDescription")) != null) mContentDescription = t.resolve(v);
        try { if ((v = a.get("minWidth")) != null) mMinWidth = (int) t.dimen(v); if ((v = a.get("minHeight")) != null) mMinHeight = (int) t.dimen(v); } catch (RuntimeException ignored) {}
        if ((v = a.get("paddingStart")) != null) mPaddingLeft = (int) t.dimen(v);
        if ((v = a.get("paddingEnd")) != null) mPaddingRight = (int) t.dimen(v);
        // AOSP: a background with padding (9-patch, inset, layer-list) supplies the view padding for every side the
        // view/style does not set itself (View(Context,AttributeSet) -> setBackground + mUserPadding* bookkeeping).
        if (mBackground != null) {
            android.graphics.Rect bp = new android.graphics.Rect();
            try { if (mBackground.getPadding(bp)) {
                boolean all = a.get("padding") != null;
                if (!all && a.get("paddingLeft") == null && a.get("paddingStart") == null && a.get("paddingHorizontal") == null) mPaddingLeft = bp.left;
                if (!all && a.get("paddingRight") == null && a.get("paddingEnd") == null && a.get("paddingHorizontal") == null) mPaddingRight = bp.right;
                if (!all && a.get("paddingTop") == null && a.get("paddingVertical") == null) mPaddingTop = bp.top;
                if (!all && a.get("paddingBottom") == null && a.get("paddingVertical") == null) mPaddingBottom = bp.bottom;
            } } catch (RuntimeException ignored) {}
        }
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-2, -2);
        if ((v = a.get("layout_width")) != null) lp.width = (int) t.dimen(v);
        if ((v = a.get("layout_height")) != null) lp.height = (int) t.dimen(v);
        if ((v = a.get("layout_weight")) != null) lp.weight = Float.parseFloat(t.resolve(v));
        if ((v = a.get("layout_gravity")) != null) lp.gravity = ResTable.intVal(t.resolve(v));
        if ((v = a.get("layout_margin")) != null) { int m = (int) t.dimen(v); lp.leftMargin = lp.topMargin = lp.rightMargin = lp.bottomMargin = m; }
        if ((v = a.get("layout_marginLeft")) != null) lp.leftMargin = (int) t.dimen(v);
        if ((v = a.get("layout_marginTop")) != null) lp.topMargin = (int) t.dimen(v);
        if ((v = a.get("layout_marginRight")) != null) lp.rightMargin = (int) t.dimen(v);
        if ((v = a.get("layout_marginBottom")) != null) lp.bottomMargin = (int) t.dimen(v);
        mLayoutParams = lp;
    }

    // ---- identity / properties
    public int getId() { return mId; }
    public int getBaseline() { return -1; }
    public void setId(int id) { mId = id; }
    public Object getTag() { return mTag; }
    public void setTag(Object o) { mTag = o; }
    private java.util.HashMap<Integer, Object> mKeyedTags;
    public void setTag(int key, Object o) { if (mKeyedTags == null) mKeyedTags = new java.util.HashMap<>(); mKeyedTags.put(key, o); }
    public Object getTag(int key) { return mKeyedTags == null ? null : mKeyedTags.get(key); }
    public Context getContext() { return mContext; }
    public Resources getResources() { return mContext.getResources(); }
    public boolean isInEditMode() { return false; }
    public CharSequence getContentDescription() { return mContentDescription; }
    public void setContentDescription(CharSequence c) { mContentDescription = c; }
    public void setSelected(boolean s) { mSelected = s; invalidate(); }
    public boolean isSelected() { return mSelected; }
    public void setPressed(boolean p) { mPressed = p; invalidate(); }
    public boolean isPressed() { return mPressed; }
    public void setBackground(Drawable d) { setBackgroundDrawable(d); }
    public void setPadding(int l, int t, int r, int b) { mPaddingLeft = l; mPaddingTop = t; mPaddingRight = r; mPaddingBottom = b; requestLayout(); }
    public ViewGroup.LayoutParams getLayoutParams() { return mLayoutParams; }
    public void setLayoutParams(ViewGroup.LayoutParams lp) { if (lp == null) throw new NullPointerException("Layout parameters cannot be null"); mLayoutParams = lp; if (mParent != null) lp.resolveLayoutDirection(getLayoutDirection()); requestLayout(); }
    public ViewParent getParent() { return mParent; }
    public ViewTreeObserver getViewTreeObserver() {
        if (mTreeObserver == null) mTreeObserver = new ViewTreeObserver();
        return mTreeObserver;
    }
    public void invalidate() { jet.runtime.Looper.dirty = true; }
    private boolean mForceLayout = true, mLaidOut, mInLayout;
    public void requestLayout() { jet.runtime.Looper.dirty = true; mForceLayout = true; if (mParent != null && !mParent.isLayoutRequested()) mParent.requestLayout(); }

    // ---- listeners / events
    public void setOnClickListener(OnClickListener l) { if (!isClickable()) setClickable(true); mOnClick = l; }
    public void setOnLongClickListener(OnLongClickListener l) { mOnLongClick = l; }
    public void setOnTouchListener(OnTouchListener l) { mOnTouch = l; }
    public void setOnKeyListener(OnKeyListener l) { mOnKey = l; }
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (mOnTouch != null && mOnTouch.onTouch(this, e)) return true;
        return onTouchEvent(e);
    }
    /** Android default: clickable views take the gesture; press on DOWN, performClick on UP inside bounds. */
    public boolean onTouchEvent(MotionEvent e) {
        boolean clickable = isClickable() || isLongClickable();
        if (!isEnabled()) { if (e.getActionMasked() == MotionEvent.ACTION_UP) setPressed(false); return clickable; }
        if (!clickable) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: setPressed(true); break;
            case MotionEvent.ACTION_MOVE: if (e.getX() < 0 || e.getY() < 0 || e.getX() >= getWidth() || e.getY() >= getHeight()) setPressed(false); break;
            case MotionEvent.ACTION_UP: if (isPressed()) { setPressed(false); performClick(); } break;
            case MotionEvent.ACTION_CANCEL: setPressed(false); break;
        }
        return true;
    }
    public boolean hasOnClickListeners() { return mOnClick != null; }
    public boolean isClickableForJet() { return mOnClick != null || mOnLongClick != null; }
    public boolean performClick() { if (mOnClick != null) { mOnClick.onClick(this); return true; } return false; }
    public boolean performLongClick() { return mOnLongClick != null && mOnLongClick.onLongClick(this); }

    public View findViewById(int id) { return id == mId ? this : findViewTraversal(id); }
    protected View findViewTraversal(int id) { return null; }

    // ---- measure / layout
    private static final boolean JET_TRACE_MEASURE = Boolean.getBoolean("jet.traceMeasure");
    public final void measure(int w, int h) { if (mForceLayout && mLayoutParams != null) mLayoutParams.resolveLayoutDirection(getLayoutDirection()); onMeasure(w, h); if (JET_TRACE_MEASURE) System.err.println("[measure] " + getClass().getSimpleName() + " " + MeasureSpec.toString(w) + " x " + MeasureSpec.toString(h) + " -> " + mMeasuredWidth + "x" + mMeasuredHeight); }
    protected void onMeasure(int w, int h) { setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), w), getDefaultSize(getSuggestedMinimumHeight(), h)); }
    protected final void setMeasuredDimension(int w, int h) { mMeasuredWidth = w; mMeasuredHeight = h; }
    public final int getMeasuredWidth() { return mMeasuredWidth & MEASURED_SIZE_MASK; }
    public final int getMeasuredHeight() { return mMeasuredHeight & MEASURED_SIZE_MASK; }
    public static int getDefaultSize(int size, int spec) {
        int m = MeasureSpec.getMode(spec), s = MeasureSpec.getSize(spec);
        return m == MeasureSpec.EXACTLY || m == MeasureSpec.AT_MOST ? s : size; // AOSP: AT_MOST yields specSize
    }
    public static int resolveSize(int desired, int spec) { return resolveSizeAndState(desired, spec, 0) & MEASURED_SIZE_MASK; }
    public void layout(int l, int t, int r, int b) {
        int ow = mRight - mLeft, oh = mBottom - mTop; boolean changed = l != mLeft || t != mTop || r != mRight || b != mBottom;
        mLeft = l; mTop = t; mRight = r; mBottom = b;
        if ((r - l != ow || b - t != oh) || !mLaidOut) onSizeChanged(r - l, b - t, ow, oh);
        mInLayout = true; try { onLayout(changed, l, t, r, b); } finally { mInLayout = false; }
        mForceLayout = false; mLaidOut = true;
        if (mLayoutListeners != null && changed) for (OnLayoutChangeListener li : new java.util.ArrayList<>(mLayoutListeners)) li.onLayoutChange(this, l, t, r, b, l, t, l + ow, t + oh);
    }
    protected void onLayout(boolean changed, int l, int t, int r, int b) {}
    public final int getLeft() { return mLeft; }
    public final int getTop() { return mTop; }
    public final int getWidth() { return mRight - mLeft; }
    public final int getHeight() { return mBottom - mTop; }

    // ---- drawing (Java2D here; Metal/CoreGraphics on iOS)
    /** ?attr drawable (e.g. android:background="?primaryButtonBackground" in a style): resolve through the context theme. */
    protected android.graphics.drawable.Drawable jetThemeDrawable(String v) {
        try {
            int attr = jet.runtime.ResTable.refId(v); if (attr == 0 || getContext() == null) return null;
            android.util.TypedValue tv = new android.util.TypedValue();
            boolean ok = getContext().getTheme().resolveAttribute(attr, tv, true);
            if (!ok) return null;
            if (tv.resourceId != 0) { android.graphics.drawable.Drawable d = getContext().getResources().table().drawable("@" + Integer.toHexString(tv.resourceId)); if (d != null) return d; }
            if (tv.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) return new android.graphics.drawable.ColorDrawable(tv.data);
        } catch (RuntimeException ignored) {}
        return null;
    }
    protected int drawState() { return (mPressed ? 1 : 0) | (mSelected ? 2 : 0); }
    /** AOSP View.draw order: background, onDraw, dispatchDraw (children), foreground; view alpha is a layer (applies to children). */
    public void draw(android.graphics.Canvas c) {
        if (c == null || c.jetGfx() == null || mVisibility != VISIBLE) return;
        int save = c.getSaveCount();
        if (mAlpha < 1f) c.saveLayerAlpha(null, Math.round(255 * Math.max(0f, mAlpha)));
        try {
            if (mBackground != null) { mBackground.setState(getDrawableState()); mBackground.jetDraw(c, 0, 0, getWidth(), getHeight(), drawState()); }
            onDraw(c);
            dispatchDraw(c);
            if (mForeground != null) { mForeground.setState(getDrawableState()); mForeground.jetDraw(c, 0, 0, getWidth(), getHeight(), drawState()); }
        } finally { c.restoreToCount(save); }
    }
    protected void onDraw(android.graphics.Canvas c) {}
    protected void dispatchDraw(android.graphics.Canvas c) {}

    // ---- broad Android View surface
    private int mVisibility = VISIBLE;
    private float mAlpha = 1f, mElevation, mTranslationX, mTranslationY, mTranslationZ, mScaleX = 1f, mScaleY = 1f, mRotation, mRotationX, mRotationY, mPivotX, mPivotY;
    private boolean mEnabled = true, mClickable, mLongClickable, mFocusable, mFocusableInTouchMode, mActivated, mFocused, mAttached, mWillNotDraw, mSaveEnabled = true, mHaptic = true, mSound = true, mKeepScreenOn, mFitsSystemWindows, mClipToOutline, mNestedScroll, mHovered, mDuplicateParentState, mVerticalScrollBar, mHorizontalScrollBar;
    private int mScrollX, mScrollY, mMinWidth, mMinHeight, mLayoutDirection, mImportantForA11y, mLayerType, mOverScrollMode, mSystemUi, mTextAlignment, mTextDirection, mScrollIndicators, mLabelFor = NO_ID, mNextFocusDown = NO_ID, mNextFocusUp = NO_ID, mNextFocusLeft = NO_ID, mNextFocusRight = NO_ID, mNextFocusForward = NO_ID;
    protected Drawable mForeground;
    private OnFocusChangeListener mOnFocus;
    private java.util.ArrayList<OnAttachStateChangeListener> mAttachListeners;
    private java.util.ArrayList<OnLayoutChangeListener> mLayoutListeners;
    private OnApplyWindowInsetsListener mInsetsListener;
    private AccessibilityDelegate mA11yDelegate;
    public static class AccessibilityDelegate { public AccessibilityDelegate() {}
        public void sendAccessibilityEvent(View h, int t) {} public boolean performAccessibilityAction(View h, int a, android.os.Bundle args) { return false; }
        public void onInitializeAccessibilityEvent(View h, android.view.accessibility.AccessibilityEvent e) {} public void onPopulateAccessibilityEvent(View h, android.view.accessibility.AccessibilityEvent e) {}
        public boolean dispatchPopulateAccessibilityEvent(View h, android.view.accessibility.AccessibilityEvent e) { return false; }
        public boolean onRequestSendAccessibilityEvent(ViewGroup p, View c, android.view.accessibility.AccessibilityEvent e) { return true; } public void sendAccessibilityEventUnchecked(View h, android.view.accessibility.AccessibilityEvent e) {} }
    public interface OnContextClickListener { boolean onContextClick(View v); }
    private CharSequence mTooltip;
    private ViewOutlineProvider mOutlineProvider = ViewOutlineProvider.BACKGROUND;
    public void setVisibility(int v) { if (mVisibility != v) { mVisibility = v; requestLayout(); invalidate(); } }
    public int getVisibility() { return mVisibility; }
    public boolean isShown() { View v = this; while (v != null) { if (v.mVisibility != VISIBLE) return false; v = v.mParent; } return true; }
    /** AOSP View animation properties. */
    public static final android.util.Property<View, Float> ALPHA = new android.util.FloatProperty<View>("alpha") { @Override public void setValue(View v, float f) { v.setAlpha(f); } @Override public Float get(View v) { return v.getAlpha(); } };
    public static final android.util.Property<View, Float> TRANSLATION_X = new android.util.FloatProperty<View>("translationX") { @Override public void setValue(View v, float f) { v.setTranslationX(f); } @Override public Float get(View v) { return v.getTranslationX(); } };
    public static final android.util.Property<View, Float> TRANSLATION_Y = new android.util.FloatProperty<View>("translationY") { @Override public void setValue(View v, float f) { v.setTranslationY(f); } @Override public Float get(View v) { return v.getTranslationY(); } };
    public static final android.util.Property<View, Float> TRANSLATION_Z = new android.util.FloatProperty<View>("translationZ") { @Override public void setValue(View v, float f) { v.setTranslationZ(f); } @Override public Float get(View v) { return v.getTranslationZ(); } };
    public static final android.util.Property<View, Float> X = new android.util.FloatProperty<View>("x") { @Override public void setValue(View v, float f) { v.setX(f); } @Override public Float get(View v) { return v.getX(); } };
    public static final android.util.Property<View, Float> Y = new android.util.FloatProperty<View>("y") { @Override public void setValue(View v, float f) { v.setY(f); } @Override public Float get(View v) { return v.getY(); } };
    public static final android.util.Property<View, Float> Z = new android.util.FloatProperty<View>("z") { @Override public void setValue(View v, float f) { v.setZ(f); } @Override public Float get(View v) { return v.getZ(); } };
    public static final android.util.Property<View, Float> ROTATION = new android.util.FloatProperty<View>("rotation") { @Override public void setValue(View v, float f) { v.setRotation(f); } @Override public Float get(View v) { return v.getRotation(); } };
    public static final android.util.Property<View, Float> ROTATION_X = new android.util.FloatProperty<View>("rotationX") { @Override public void setValue(View v, float f) { v.setRotationX(f); } @Override public Float get(View v) { return v.getRotationX(); } };
    public static final android.util.Property<View, Float> ROTATION_Y = new android.util.FloatProperty<View>("rotationY") { @Override public void setValue(View v, float f) { v.setRotationY(f); } @Override public Float get(View v) { return v.getRotationY(); } };
    public static final android.util.Property<View, Float> SCALE_X = new android.util.FloatProperty<View>("scaleX") { @Override public void setValue(View v, float f) { v.setScaleX(f); } @Override public Float get(View v) { return v.getScaleX(); } };
    public static final android.util.Property<View, Float> SCALE_Y = new android.util.FloatProperty<View>("scaleY") { @Override public void setValue(View v, float f) { v.setScaleY(f); } @Override public Float get(View v) { return v.getScaleY(); } };
    public void setAlpha(float a) { mAlpha = a; invalidate(); }
    public float getAlpha() { return mAlpha; }
    public boolean hasOverlappingRendering() { return true; }
    public void setElevation(float e) { mElevation = e; }
    public float getElevation() { return mElevation; }
    public void setTranslationX(float v) { mTranslationX = v; } public float getTranslationX() { return mTranslationX; }
    public void setTranslationY(float v) { mTranslationY = v; } public float getTranslationY() { return mTranslationY; }
    public void setTranslationZ(float v) { mTranslationZ = v; } public float getTranslationZ() { return mTranslationZ; }
    public float getZ() { return mElevation + mTranslationZ; } public void setZ(float z) { mTranslationZ = z - mElevation; }
    public void setScaleX(float v) { mScaleX = v; } public float getScaleX() { return mScaleX; }
    public void setScaleY(float v) { mScaleY = v; } public float getScaleY() { return mScaleY; }
    public void setRotation(float v) { mRotation = v; } public float getRotation() { return mRotation; }
    public void setRotationX(float v) { mRotationX = v; } public float getRotationX() { return mRotationX; }
    public void setRotationY(float v) { mRotationY = v; } public float getRotationY() { return mRotationY; }
    public void setPivotX(float v) { mPivotX = v; mPivotExplicit = true; } public float getPivotX() { return mPivotX; }
    public void setPivotY(float v) { mPivotY = v; mPivotExplicit = true; } public float getPivotY() { return mPivotY; }
    public float getX() { return mLeft + mTranslationX; } public void setX(float x) { mTranslationX = x - mLeft; }
    public float getY() { return mTop + mTranslationY; } public void setY(float y) { mTranslationY = y - mTop; }
    public final int getRight() { return mRight; }
    public final int getBottom() { return mBottom; }
    public void setLeft(int l) { mLeft = l; } public void setTop(int t) { mTop = t; } public void setRight(int r) { mRight = r; } public void setBottom(int b) { mBottom = b; }
    public void offsetLeftAndRight(int d) { mLeft += d; mRight += d; }
    public void offsetTopAndBottom(int d) { mTop += d; mBottom += d; }
    public void setEnabled(boolean e) { mEnabled = e; refreshDrawableState(); }
    public boolean isEnabled() { return mEnabled; }
    public void setClickable(boolean c) { mClickable = c; } public boolean isClickable() { return mClickable || mOnClickPresent(); }
    private boolean mOnClickPresent() { return hasOnClickListeners(); }
    public void setLongClickable(boolean c) { mLongClickable = c; } public boolean isLongClickable() { return mLongClickable; }
    public void setContextClickable(boolean c) {}
    public void setFocusable(boolean f) { mFocusable = f; } public void setFocusable(int f) { mFocusable = f != 0; } public boolean isFocusable() { return mFocusable; }
    public void setFocusableInTouchMode(boolean f) { mFocusableInTouchMode = f; } public boolean isFocusableInTouchMode() { return mFocusableInTouchMode; }
    public boolean isFocused() { return mFocused; } public boolean hasFocus() { return findFocus() != null; } public boolean hasFocusable() { return mFocusable && isShown(); }
    public static final int FOCUS_BACKWARD = 1, FOCUS_FORWARD = 2, FOCUS_LEFT = 17, FOCUS_UP = 33, FOCUS_RIGHT = 66, FOCUS_DOWN = 130;
    // AOSP focus model: requestFocus() -> requestFocus(FOCUS_DOWN) -> requestFocus(int, Rect) (overridable by apps
    // and ViewGroup), at most one focused view per window, onFocusChanged/OnFocusChangeListener on gain and loss,
    // and the input method follows the focused editor.
    public final boolean requestFocus() { return requestFocus(FOCUS_DOWN); }
    public final boolean requestFocus(int dir) { return requestFocus(dir, null); }
    public boolean requestFocus(int dir, android.graphics.Rect r) { return jetRequestFocusNoSearch(dir, r); }
    final boolean jetRequestFocusNoSearch(int dir, android.graphics.Rect r) {
        if (!(mFocusable || mFocusableInTouchMode) || getVisibility() != VISIBLE || !mEnabled) { jetTraceFocus("refused (focusable=" + mFocusable + "/" + mFocusableInTouchMode + " vis=" + getVisibility() + " enabled=" + mEnabled + ")"); return false; }
        for (ViewParent p = getParent(); p instanceof ViewGroup; p = p.getParent())
            if (((ViewGroup) p).getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) { jetTraceFocus("blocked by " + p.getClass().getName()); return false; }
        jetHandleFocusGain(dir, r); jetTraceFocus("gained"); return true;
    }
    private void jetHandleFocusGain(int dir, android.graphics.Rect r) {
        if (mFocused) return;
        View old = getRootView().findFocus();
        if (old != null && old != this) old.jetLoseFocus();
        mFocused = true;
        try { onFocusChanged(true, dir, r); } finally { refreshDrawableState(); }
        if (mOnFocus != null) mOnFocus.onFocusChange(this, true);
        android.view.inputmethod.InputMethodManager.jetFocusIn(this);
    }
    private static final boolean TRACE_FOCUS = Boolean.getBoolean("jet.traceFocus");
    private void jetTraceFocus(String what) { if (TRACE_FOCUS) System.err.println("[jet-focus] " + getClass().getName() + " " + what); }
    void jetLoseFocus() {
        if (!mFocused) return; mFocused = false; jetTraceFocus("lost");
        try { onFocusChanged(false, 0, null); } finally { refreshDrawableState(); }
        if (mOnFocus != null) mOnFocus.onFocusChange(this, false);
    }
    public void clearFocus() { jetLoseFocus(); }
    public View findFocus() { return mFocused ? this : null; }
    public boolean isInTouchMode() { return true; }
    public void setOnFocusChangeListener(OnFocusChangeListener l) { mOnFocus = l; } public OnFocusChangeListener getOnFocusChangeListener() { return mOnFocus; }
    public void setActivated(boolean a) { mActivated = a; } public boolean isActivated() { return mActivated; }
    public void setHovered(boolean h) { mHovered = h; } public boolean isHovered() { return mHovered; }
    public void setDuplicateParentStateEnabled(boolean e) { mDuplicateParentState = e; }
    public int getPaddingLeft() { return mPaddingLeft; } public int getPaddingTop() { return mPaddingTop; }
    public int getPaddingRight() { return mPaddingRight; } public int getPaddingBottom() { return mPaddingBottom; }
    public int getPaddingStart() { return mPaddingLeft; } public int getPaddingEnd() { return mPaddingRight; }
    public void setPaddingRelative(int s, int t, int e, int b) { setPadding(s, t, e, b); }
    public boolean isPaddingRelative() { return false; }
    public void setBackgroundColor(int c) { setBackground(new android.graphics.drawable.ColorDrawable(c)); }
    public void setBackgroundResource(int id) { setBackground(id == 0 ? null : getResources().getDrawable(id)); }
    @Deprecated public void setBackgroundDrawable(Drawable d) {
        if (d == mBackground) return;
        if (mBackground != null) mBackground.setCallback(null);
        mBackground = d;
        if (d != null) {
            if (d.isStateful()) d.setState(getDrawableState()); d.setLayoutDirection(getLayoutDirection());
            android.graphics.Rect bp = new android.graphics.Rect();   // AOSP setBackgroundDrawable: drawable padding replaces the view padding
            try { if (d.getPadding(bp)) { mPaddingLeft = bp.left; mPaddingTop = bp.top; mPaddingRight = bp.right; mPaddingBottom = bp.bottom; } } catch (RuntimeException ignored) {}
        }
        requestLayout(); invalidate();
    }
    public Drawable getBackground() { return mBackground; }
    public void setBackgroundTintList(android.content.res.ColorStateList l) { if (mBackground != null) mBackground.setTintList(l); }
    public void setBackgroundTintMode(android.graphics.PorterDuff.Mode m) {}
    public android.content.res.ColorStateList getBackgroundTintList() { return null; }
    public void setForeground(Drawable d) { mForeground = d; } public Drawable getForeground() { return mForeground; }
    public void setMinimumWidth(int w) { mMinWidth = w; } public void setMinimumHeight(int h) { mMinHeight = h; }
    public int getMinimumWidth() { return mMinWidth; } public int getMinimumHeight() { return mMinHeight; }
    protected int getSuggestedMinimumWidth() { return Math.max(mMinWidth, mBackground == null ? 0 : mBackground.getMinimumWidth()); }
    protected int getSuggestedMinimumHeight() { return Math.max(mMinHeight, mBackground == null ? 0 : mBackground.getMinimumHeight()); }
    public final int getMeasuredWidthAndState() { return mMeasuredWidth; }
    public final int getMeasuredHeightAndState() { return mMeasuredHeight; }
    public final int getMeasuredState() { return (mMeasuredWidth & MEASURED_STATE_MASK) | ((mMeasuredHeight >> MEASURED_HEIGHT_STATE_SHIFT) & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT)); }
    public static int combineMeasuredStates(int a, int b) { return a | b; }
    public static int resolveSizeAndState(int size, int spec, int childState) {
        int m = MeasureSpec.getMode(spec), s = MeasureSpec.getSize(spec), r;
        if (m == MeasureSpec.AT_MOST) r = s < size ? (s | MEASURED_STATE_TOO_SMALL) : size; else if (m == MeasureSpec.EXACTLY) r = s; else r = size;
        return r | (childState & MEASURED_STATE_MASK); }
    public void forceLayout() { mForceLayout = true; } public boolean isLayoutRequested() { return mForceLayout; } public boolean isLaidOut() { return mLaidOut; }
    public boolean isInLayout() { return mInLayout; }
    public void invalidate(int l, int t, int r, int b) { invalidate(); } public void invalidate(android.graphics.Rect r) { invalidate(); }
    public void postInvalidate() { invalidate(); } public void postInvalidateOnAnimation() { invalidate(); } public void postInvalidateDelayed(long d) { invalidate(); }
    public void invalidateDrawable(Drawable d) { invalidate(); }
    public void scheduleDrawable(Drawable who, Runnable what, long when) { postDelayed(what, Math.max(0, when - android.os.SystemClock.uptimeMillis())); }
    public void unscheduleDrawable(Drawable who, Runnable what) { removeCallbacks(what); }
    public void unscheduleDrawable(Drawable who) {}
    protected boolean verifyDrawable(Drawable who) { return who == mBackground || who == mForeground; }
    protected void drawableStateChanged() { if (mBackground != null) mBackground.setState(getDrawableState()); }
    public void refreshDrawableState() { drawableStateChanged(); }
    public final int[] getDrawableState() { return onCreateDrawableState(0); }
    protected int[] onCreateDrawableState(int extra) { int[] s = new int[extra + 1 + (mEnabled ? 1 : 0) + (mPressed ? 1 : 0) + (mSelected ? 1 : 0)]; int i = 0; s[i++] = 16842909 /*window_focused*/; if (mEnabled) s[i++] = 16842910; if (mPressed) s[i++] = 16842919; if (mSelected) s[i++] = 16842913; return s; }
    protected static int[] mergeDrawableStates(int[] base, int[] add) { int i = base.length - 1; while (i >= 0 && base[i] == 0) i--; System.arraycopy(add, 0, base, i + 1, Math.min(add.length, base.length - i - 1)); return base; }
    public void jumpDrawablesToCurrentState() { if (mBackground != null) mBackground.jumpToCurrentState(); }
    public void drawableHotspotChanged(float x, float y) {}
    private static android.os.Handler sJetMainHandler;
    public android.os.Handler getHandler() { if (sJetMainHandler == null) sJetMainHandler = new android.os.Handler(android.os.Looper.getMainLooper()); return sJetMainHandler; }
    public boolean post(Runnable r) { return getHandler().post(r); }
    public boolean postDelayed(Runnable r, long d) { return getHandler().postDelayed(r, d); }
    public void postOnAnimation(Runnable r) { post(r); }
    public void postOnAnimationDelayed(Runnable r, long d) { postDelayed(r, d); }
    public boolean removeCallbacks(Runnable r) { if (r != null) getHandler().removeCallbacks(r); return true; }
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener l) { if (mAttachListeners == null) mAttachListeners = new java.util.ArrayList<>(); mAttachListeners.add(l); if (mAttached) l.onViewAttachedToWindow(this); }
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener l) { if (mAttachListeners != null) mAttachListeners.remove(l); }
    public void addOnLayoutChangeListener(OnLayoutChangeListener l) { if (mLayoutListeners == null) mLayoutListeners = new java.util.ArrayList<>(); mLayoutListeners.add(l); }
    public void removeOnLayoutChangeListener(OnLayoutChangeListener l) { if (mLayoutListeners != null) mLayoutListeners.remove(l); }
    public android.window.OnBackInvokedDispatcher findOnBackInvokedDispatcher() { Context c = getContext(); while (c instanceof android.content.ContextWrapper && !(c instanceof android.app.Activity)) c = ((android.content.ContextWrapper) c).getBaseContext(); return c instanceof android.app.Activity ? ((android.app.Activity) c).getOnBackInvokedDispatcher() : null; }
    public boolean isAttachedToWindow() { return mAttached; }
    public void jetDispatchAttached() {
        if (mAttached) return;
        mAttached = true;
        try { onAttachedToWindow(); } catch (Throwable t) { System.err.println("[jet] onAttachedToWindow: " + t); }
        if (mAttachListeners != null) for (OnAttachStateChangeListener l : new java.util.ArrayList<>(mAttachListeners)) {
            try { l.onViewAttachedToWindow(this); } catch (Throwable t) { System.err.println("[jet] onViewAttachedToWindow: " + t); }
        }
        if (this instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) this;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View c = vg.getChildAt(i);
                if (c != null && !c.isAttachedToWindow()) c.jetDispatchAttached();
            }
        }
    }
    protected void onAttachedToWindow() {}
    protected void onDetachedFromWindow() {}
    protected void onFinishInflate() {}
    protected void onSizeChanged(int w, int h, int ow, int oh) {}
    protected void onVisibilityChanged(View changed, int v) {}
    public void onWindowFocusChanged(boolean f) {}
    protected void onFocusChanged(boolean f, int dir, android.graphics.Rect prev) {}
    public void onRtlPropertiesChanged(int dir) {}
    protected void onConfigurationChanged(android.content.res.Configuration c) {}
    public View getRootView() { View v = this; while (v.mParent != null) v = v.mParent; return v; }
    private static final android.os.IBinder JET_WINDOW_TOKEN = new android.os.IBinder() {};
    public android.os.IBinder getWindowToken() { return mAttached ? JET_WINDOW_TOKEN : null; } public android.os.IBinder getApplicationWindowToken() { return getWindowToken(); }
    public Display getDisplay() { return mAttached ? Display.jetDefault() : null; }
    public void setWillNotDraw(boolean w) { mWillNotDraw = w; } public boolean willNotDraw() { return mWillNotDraw; }
    public void setLayoutDirection(int d) { mLayoutDirection = d; } public int getLayoutDirection() { return LAYOUT_DIRECTION_LTR; }
    public boolean isLayoutDirectionResolved() { return true; } public int getRawLayoutDirection() { return mLayoutDirection; }
    public void setTextAlignment(int a) { mTextAlignment = a; } public int getTextAlignment() { return mTextAlignment; }
    public void setTextDirection(int d) { mTextDirection = d; } public int getTextDirection() { return mTextDirection; }
    public void setImportantForAccessibility(int m) { mImportantForA11y = m; } public int getImportantForAccessibility() { return mImportantForA11y; }
    private android.view.autofill.AutofillId mAutofillId;
    public android.view.autofill.AutofillId getAutofillId() { if (mAutofillId == null) mAutofillId = new android.view.autofill.AutofillId(); return mAutofillId; }
    public void setAutofillId(android.view.autofill.AutofillId id) { mAutofillId = id; }
    public void setImportantForAutofill(int m) { jetImportantForAutofill = m; } public void setAutofillHints(String... h) {}
    public void setAccessibilityDelegate(AccessibilityDelegate d) { mA11yDelegate = d; } public AccessibilityDelegate getAccessibilityDelegate() { return mA11yDelegate; }
    public void setAccessibilityLiveRegion(int m) {} public int getAccessibilityLiveRegion() { return 0; }
    public void setAccessibilityHeading(boolean h) {} public void setAccessibilityPaneTitle(CharSequence t) {}
    public void setScreenReaderFocusable(boolean f) {}
    public void sendAccessibilityEvent(int t) {}
    public void announceForAccessibility(CharSequence t) {}
    public void setLabelFor(int id) { mLabelFor = id; } public int getLabelFor() { return mLabelFor; }
    public void setNextFocusDownId(int id) { mNextFocusDown = id; } public void setNextFocusUpId(int id) { mNextFocusUp = id; }
    public void setNextFocusLeftId(int id) { mNextFocusLeft = id; } public void setNextFocusRightId(int id) { mNextFocusRight = id; }
    public void setNextFocusForwardId(int id) { mNextFocusForward = id; }
    public void setLayerType(int t, android.graphics.Paint p) { mLayerType = t; } public int getLayerType() { return mLayerType; }
    public void setOverScrollMode(int m) { mOverScrollMode = m; } public int getOverScrollMode() { return mOverScrollMode; }
    public void setVerticalScrollBarEnabled(boolean e) { mVerticalScrollBar = e; } public void setHorizontalScrollBarEnabled(boolean e) { mHorizontalScrollBar = e; }
    public boolean isVerticalScrollBarEnabled() { return mVerticalScrollBar; } public boolean isHorizontalScrollBarEnabled() { return mHorizontalScrollBar; }
    public void setScrollBarStyle(int s) {} public void setScrollbarFadingEnabled(boolean e) {} public void setScrollIndicators(int i) { mScrollIndicators = i; } public void setScrollIndicators(int i, int m) { mScrollIndicators = i; }
    public void setVerticalFadingEdgeEnabled(boolean e) { mVerticalFadingEdge = e; } public void setHorizontalFadingEdgeEnabled(boolean e) { mHorizontalFadingEdge = e; } public void setFadingEdgeLength(int l) { mFadingEdgeLength = l; }
    public void scrollTo(int x, int y) { int ox = mScrollX, oy = mScrollY; mScrollX = x; mScrollY = y; onScrollChanged(x, y, ox, oy); invalidate(); }
    public void scrollBy(int x, int y) { scrollTo(mScrollX + x, mScrollY + y); }
    public final int getScrollX() { return mScrollX; } public final int getScrollY() { return mScrollY; }
    public void setScrollX(int x) { scrollTo(x, mScrollY); } public void setScrollY(int y) { scrollTo(mScrollX, y); }
    protected void onScrollChanged(int l, int t, int ol, int ot) {}
    public boolean canScrollVertically(int d) { return false; } public boolean canScrollHorizontally(int d) { return false; }
    public void setNestedScrollingEnabled(boolean e) { mNestedScroll = e; } public boolean isNestedScrollingEnabled() { return mNestedScroll; }
    public boolean startNestedScroll(int axes) { return false; } public void stopNestedScroll() {} public boolean hasNestedScrollingParent() { return false; }
    public boolean dispatchNestedScroll(int a, int b, int c, int d, int[] o) { return false; } public boolean dispatchNestedPreScroll(int a, int b, int[] c, int[] o) { return false; }
    public boolean dispatchNestedFling(float vx, float vy, boolean c) { return false; } public boolean dispatchNestedPreFling(float vx, float vy) { return false; }
    public void computeScroll() {}
    public void setSaveEnabled(boolean e) { mSaveEnabled = e; } public boolean isSaveEnabled() { return mSaveEnabled; }
    private boolean mSaveFromParent = true;
    public void setSaveFromParentEnabled(boolean e) { mSaveFromParent = e; }
    public boolean isSaveFromParentEnabled() { return mSaveFromParent; }
    /** AOSP: a plain View has no state of its own -> BaseSavedState.EMPTY_STATE (subclasses wrap it). */
    protected android.os.Parcelable onSaveInstanceState() { return BaseSavedState.EMPTY_STATE; }
    protected void onRestoreInstanceState(android.os.Parcelable s) {}
    public void saveHierarchyState(android.util.SparseArray<android.os.Parcelable> container) { dispatchSaveInstanceState(container); }
    protected void dispatchSaveInstanceState(android.util.SparseArray<android.os.Parcelable> container) {
        if (mId != NO_ID && mSaveEnabled) { android.os.Parcelable st = onSaveInstanceState(); if (st != null) container.put(mId, st); }
    }
    public void restoreHierarchyState(android.util.SparseArray<android.os.Parcelable> container) { dispatchRestoreInstanceState(container); }
    protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> container) {
        if (mId != NO_ID) { android.os.Parcelable st = container.get(mId); if (st != null) onRestoreInstanceState(st); }
    }
    public static class BaseSavedState extends AbsSavedState {
        public BaseSavedState(android.os.Parcelable superState) { super(superState); }
        public BaseSavedState(android.os.Parcel source) { super(source); }
        public BaseSavedState(android.os.Parcel source, ClassLoader loader) { super(source, loader); }
        @Override public void writeToParcel(android.os.Parcel out, int flags) { super.writeToParcel(out, flags); }
        public static final android.os.Parcelable.Creator<BaseSavedState> CREATOR = new android.os.Parcelable.ClassLoaderCreator<BaseSavedState>() {
            public BaseSavedState createFromParcel(android.os.Parcel in) { return new BaseSavedState(in); }
            public BaseSavedState createFromParcel(android.os.Parcel in, ClassLoader l) { return new BaseSavedState(in, l); }
            public BaseSavedState[] newArray(int n) { return new BaseSavedState[n]; }
        };
    }
    public void setHapticFeedbackEnabled(boolean e) { mHaptic = e; } public boolean isHapticFeedbackEnabled() { return mHaptic; }
    public boolean performHapticFeedback(int c) { return false; } public boolean performHapticFeedback(int c, int f) { return false; }
    public void setSoundEffectsEnabled(boolean e) { mSound = e; } public boolean isSoundEffectsEnabled() { return mSound; }
    public void playSoundEffect(int s) {}
    public void setKeepScreenOn(boolean k) { mKeepScreenOn = k; } public boolean getKeepScreenOn() { return mKeepScreenOn; }
    public void setFitsSystemWindows(boolean f) { mFitsSystemWindows = f; } public boolean getFitsSystemWindows() { return mFitsSystemWindows; }
    public void setSystemUiVisibility(int v) { mSystemUi = v; } public int getSystemUiVisibility() { return mSystemUi; } public int getWindowSystemUiVisibility() { return mSystemUi; }
    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {}
    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener l) { mInsetsListener = l; }
    public WindowInsets onApplyWindowInsets(WindowInsets i) { return i; }
    public WindowInsets dispatchApplyWindowInsets(WindowInsets i) { return mInsetsListener != null ? mInsetsListener.onApplyWindowInsets(this, i) : onApplyWindowInsets(i); }
    public WindowInsets getRootWindowInsets() { return WindowInsets.CONSUMED; }
    public void requestApplyInsets() {} public void requestFitSystemWindows() {}
    public WindowInsetsController getWindowInsetsController() { // API 30: the hosting window's controller
        android.content.Context c = getContext();
        while (c instanceof android.content.ContextWrapper && !(c instanceof android.app.Activity)) c = ((android.content.ContextWrapper) c).getBaseContext();
        return c instanceof android.app.Activity && ((android.app.Activity) c).getWindow() != null ? ((android.app.Activity) c).getWindow().getInsetsController() : null; }
    private WindowInsetsAnimation.Callback mInsetsAnimCb;
    public void setWindowInsetsAnimationCallback(WindowInsetsAnimation.Callback cb) { mInsetsAnimCb = cb; }
    public WindowInsetsAnimation.Callback jetWindowInsetsAnimationCallback() { return mInsetsAnimCb; }
    public void setOnGenericMotionListener(OnGenericMotionListener l) {}
    public void setOnHoverListener(OnHoverListener l) {}
    public void setOnDragListener(OnDragListener l) {}
    public void setOnScrollChangeListener(OnScrollChangeListener l) {}
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {}
    public void setOnContextClickListener(OnContextClickListener l) {}
    public void setTooltipText(CharSequence t) { mTooltip = t; } public CharSequence getTooltipText() { return mTooltip; }
    public void setOutlineProvider(ViewOutlineProvider p) { mOutlineProvider = p; } public ViewOutlineProvider getOutlineProvider() { return mOutlineProvider; } public void invalidateOutline() {}
    public void setClipToOutline(boolean c) { mClipToOutline = c; } public boolean getClipToOutline() { return mClipToOutline; }
    public void setStateListAnimator(android.animation.StateListAnimator a) {} public android.animation.StateListAnimator getStateListAnimator() { return null; }
    public void setDrawingCacheEnabled(boolean e) {}
    public void setTransitionName(String n) {} public String getTransitionName() { return null; }
    public void setTag(String s) { setTag((Object) s); }
    public void getLocationOnScreen(int[] out) { getLocationInWindow(out); }
    public void getLocationInWindow(int[] out) { int x = 0, y = 0; View v = this; while (v != null) { x += v.mLeft - (v.mParent != null ? v.mParent.getScrollX() : 0); y += v.mTop - (v.mParent != null ? v.mParent.getScrollY() : 0); v = v.mParent; } out[0] = x; out[1] = y; }
    public boolean getGlobalVisibleRect(android.graphics.Rect r) { int[] l = new int[2]; getLocationInWindow(l); r.set(l[0], l[1], l[0] + getWidth(), l[1] + getHeight()); return getWidth() > 0 && getHeight() > 0; }
    public boolean getLocalVisibleRect(android.graphics.Rect r) { r.set(0, 0, getWidth(), getHeight()); return getWidth() > 0; }
    public void getHitRect(android.graphics.Rect r) { r.set(mLeft, mTop, mRight, mBottom); }
    public void getDrawingRect(android.graphics.Rect r) { r.set(mScrollX, mScrollY, mScrollX + getWidth(), mScrollY + getHeight()); }
    public void getWindowVisibleDisplayFrame(android.graphics.Rect r) { r.set(0, 0, 750, 1334); }
    public boolean requestRectangleOnScreen(android.graphics.Rect r) { return requestRectangleOnScreen(r, false); }
    public void bringToFront() {}
    public void cancelLongPress() {}
    public void setOnLongClickListenerCompat(OnLongClickListener l) { setOnLongClickListener(l); }
    public boolean callOnClick() { return performClick(); }
    public boolean onKeyDown(int k, KeyEvent e) { return false; } public boolean onKeyUp(int k, KeyEvent e) { return false; }
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (mOnKey != null && mOnKey.onKey(this, e.getKeyCode(), e)) return true;
        if (e.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(e.getKeyCode(), e);
        if (e.getAction() == KeyEvent.ACTION_UP) return onKeyUp(e.getKeyCode(), e);
        return false;
    }
    public boolean onInterceptTouchEvent(MotionEvent e) { return false; }
    public boolean onGenericMotionEvent(MotionEvent e) { return false; }
    public boolean onHoverEvent(MotionEvent e) { return false; }
    public boolean onCheckIsTextEditor() { return false; }
    public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo outAttrs) { return null; }
    private ViewPropertyAnimator mJetAnimator;
    public ViewPropertyAnimator animate() { if (mJetAnimator == null) mJetAnimator = new ViewPropertyAnimator(this); return mJetAnimator; }

    private android.view.animation.Animation mAnimation;
    public void startAnimation(android.view.animation.Animation a) { mAnimation = a; if (a != null) a.jetRunToEnd(); } public android.view.animation.Animation getAnimation() { return mAnimation; } public void clearAnimation() { mAnimation = null; }
    public void setAnimation(android.view.animation.Animation a) { mAnimation = a; }
    public static int generateViewId() { return sNextId.getAndIncrement(); }
    private static final java.util.concurrent.atomic.AtomicInteger sNextId = new java.util.concurrent.atomic.AtomicInteger(0x00F00001);
    public static View inflate(Context c, int res, ViewGroup root) { return LayoutInflater.from(c).inflate(res, root); }
    public void setPointerIcon(PointerIcon i) {} public PointerIcon getPointerIcon() { return null; }
    public void setDefaultFocusHighlightEnabled(boolean e) {}
    public void setRevealOnFocusHint(boolean r) {}
    public void setFilterTouchesWhenObscured(boolean f) {}
    public void setSelected$jet(boolean s) { setSelected(s); }
    // ---- AOSP semantics for small View APIs (Pass 33) ----
    private int jetImportantForAutofill; private int jetTransientState;
    private static final java.util.concurrent.atomic.AtomicLong JET_DRAWING_IDS = new java.util.concurrent.atomic.AtomicLong(1);
    private final long jetDrawingId = JET_DRAWING_IDS.getAndIncrement();
    public int getImportantForAutofill() { return jetImportantForAutofill; }
    public boolean isImportantForAutofill() { int m = jetImportantForAutofill; return m != 2 /* NO */ && m != 8 /* NO_EXCLUDE_DESCENDANTS */; }
    public boolean hasTransientState() { return jetTransientState > 0; }
    public void setHasTransientState(boolean has) { boolean old = hasTransientState(); jetTransientState = has ? jetTransientState + 1 : Math.max(0, jetTransientState - 1); boolean now = hasTransientState(); if (old != now && getParent() != null) try { getParent().childHasTransientStateChanged(this, now); } catch (AbstractMethodError e) {} }
    public void cancelPendingInputEvents() { /* input is delivered synchronously by the harness: nothing is ever pending */ }
    public void transformMatrixToGlobal(android.graphics.Matrix m) {
        ViewParent p = getParent();
        if (p instanceof View) { View vp = (View) p; vp.transformMatrixToGlobal(m); m.preTranslate(-vp.getScrollX(), -vp.getScrollY()); }
        m.preTranslate(getLeft(), getTop());
        m.preTranslate(getTranslationX(), getTranslationY());
    }
    public long getUniqueDrawingId() { return jetDrawingId; }
    public boolean isShowingLayoutBounds() { return false; }
    public void saveAttributeDataForStyleable(android.content.Context c, int[] styleable, android.util.AttributeSet a, android.content.res.TypedArray t, int defStyleAttr, int defStyleRes) { /* AOSP: only retained when debug view attributes are enabled (off) */ }
    public boolean requestRectangleOnScreen(android.graphics.Rect rectangle, boolean immediate) {
        View child = this; ViewParent parent = getParent(); boolean scrolled = false;
        android.graphics.Rect r = new android.graphics.Rect(rectangle);
        while (parent != null) {
            try { scrolled |= parent.requestChildRectangleOnScreen(child, r, immediate); } catch (AbstractMethodError e) { break; }
            if (!(parent instanceof View)) break;
            r.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
            child = (View) parent; parent = child.getParent();
        }
        return scrolled;
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.content.res.ColorStateList jet_ForegroundTintList;
    private android.graphics.BlendMode jet_BackgroundTintBlendMode;
    private android.graphics.BlendMode jet_ForegroundTintBlendMode;
    private android.graphics.Matrix jet_AnimationMatrix;
    private android.graphics.PorterDuff.Mode jet_ForegroundTintMode;
    private android.graphics.Rect jet_ClipBounds;
    private android.graphics.drawable.Drawable jet_HorizontalScrollbarThumbDrawable;
    private android.graphics.drawable.Drawable jet_HorizontalScrollbarTrackDrawable;
    private android.graphics.drawable.Drawable jet_VerticalScrollbarThumbDrawable;
    private android.graphics.drawable.Drawable jet_VerticalScrollbarTrackDrawable;
    private boolean jet_AutoHandwritingEnabled;
    private boolean jet_FocusedByDefault;
    private boolean jet_ForceDarkAllowed;
    private boolean jet_KeyboardNavigationCluster;
    private boolean jet_PreferKeepClear;
    private boolean jet_ScrollContainer;
    private float jet_CameraDistance;
    private float jet_TransitionAlpha;
    private int jet_AccessibilityTraversalAfter;
    private int jet_AccessibilityTraversalBefore;
    private int jet_DrawingCacheBackgroundColor;
    private int jet_DrawingCacheQuality;
    private int jet_ForegroundGravity;
    private int jet_ImportantForContentCapture;
    private int jet_NextClusterForwardId;
    private int jet_OutlineAmbientShadowColor;
    private int jet_OutlineSpotShadowColor;
    private int jet_ScrollBarDefaultDelayBeforeFade;
    private int jet_ScrollBarFadeDuration;
    private int jet_ScrollBarSize;
    private int jet_ScrollCaptureHint;
    private int jet_VerticalScrollbarPosition;
    private java.lang.CharSequence jet_StateDescription;
    private java.util.List jet_PreferKeepClearRects;
    private java.util.List jet_SystemGestureExclusionRects;
    public int[] getAttributeResolutionStack(int p0) { jet.runtime.ShimStubs.hit("android.view.View.getAttributeResolutionStack(I)[I"); return null; }
    public java.util.Map getAttributeSourceResourceMap() { jet.runtime.ShimStubs.hit("android.view.View.getAttributeSourceResourceMap()Ljava/util/Map;"); return null; }
    public int getExplicitStyle() { jet.runtime.ShimStubs.hit("android.view.View.getExplicitStyle()I"); return 0; }
    public int getVerticalFadingEdgeLength() { return mVerticalFadingEdge ? jetFadingEdgeLength() : 0; }
    public int getHorizontalFadingEdgeLength() { return mHorizontalFadingEdge ? jetFadingEdgeLength() : 0; }
    private int mFadingEdgeLength = -1; private boolean mVerticalFadingEdge, mHorizontalFadingEdge;
    private int jetFadingEdgeLength() { return mFadingEdgeLength >= 0 ? mFadingEdgeLength : android.view.ViewConfiguration.get(getContext()).getScaledFadingEdgeLength(); }
    public int getVerticalScrollbarWidth() { jet.runtime.ShimStubs.hit("android.view.View.getVerticalScrollbarWidth()I"); return 0; }
    protected int getHorizontalScrollbarHeight() { jet.runtime.ShimStubs.hit("android.view.View.getHorizontalScrollbarHeight()I"); return 0; }
    public void setVerticalScrollbarThumbDrawable(android.graphics.drawable.Drawable p0) { jet_VerticalScrollbarThumbDrawable = p0; }
    public void setVerticalScrollbarTrackDrawable(android.graphics.drawable.Drawable p0) { jet_VerticalScrollbarTrackDrawable = p0; }
    public void setHorizontalScrollbarThumbDrawable(android.graphics.drawable.Drawable p0) { jet_HorizontalScrollbarThumbDrawable = p0; }
    public void setHorizontalScrollbarTrackDrawable(android.graphics.drawable.Drawable p0) { jet_HorizontalScrollbarTrackDrawable = p0; }
    public android.graphics.drawable.Drawable getVerticalScrollbarThumbDrawable() { return jet_VerticalScrollbarThumbDrawable; }
    public android.graphics.drawable.Drawable getVerticalScrollbarTrackDrawable() { return jet_VerticalScrollbarTrackDrawable; }
    public android.graphics.drawable.Drawable getHorizontalScrollbarThumbDrawable() { return jet_HorizontalScrollbarThumbDrawable; }
    public android.graphics.drawable.Drawable getHorizontalScrollbarTrackDrawable() { return jet_HorizontalScrollbarTrackDrawable; }
    public void setVerticalScrollbarPosition(int p0) { jet_VerticalScrollbarPosition = p0; }
    public int getVerticalScrollbarPosition() { return jet_VerticalScrollbarPosition; }
    public int getScrollIndicators() { jet.runtime.ShimStubs.hit("android.view.View.getScrollIndicators()I"); return 0; }
    public boolean hasOnLongClickListeners() { jet.runtime.ShimStubs.hit("android.view.View.hasOnLongClickListeners()Z"); return false; }
    public boolean performLongClick(float p0, float p1) { jet.runtime.ShimStubs.hit("android.view.View.performLongClick(FF)Z"); return false; }
    public boolean performContextClick(float p0, float p1) { jet.runtime.ShimStubs.hit("android.view.View.performContextClick(FF)Z"); return false; }
    public boolean performContextClick() { jet.runtime.ShimStubs.hit("android.view.View.performContextClick()Z"); return false; }
    public boolean showContextMenu() { jet.runtime.ShimStubs.hit("android.view.View.showContextMenu()Z"); return false; }
    public boolean showContextMenu(float p0, float p1) { jet.runtime.ShimStubs.hit("android.view.View.showContextMenu(FF)Z"); return false; }
    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback p0) { jet.runtime.ShimStubs.hit("android.view.View.startActionMode(Landroid/view/ActionMode$Callback;)Landroid/view/ActionMode;"); return null; }
    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.startActionMode(Landroid/view/ActionMode$Callback;I)Landroid/view/ActionMode;"); return null; }
    public boolean getRevealOnFocusHint() { jet.runtime.ShimStubs.hit("android.view.View.getRevealOnFocusHint()Z"); return false; }
    public boolean hasExplicitFocusable() { jet.runtime.ShimStubs.hit("android.view.View.hasExplicitFocusable()Z"); return false; }
    public java.lang.CharSequence getAccessibilityPaneTitle() { jet.runtime.ShimStubs.hit("android.view.View.getAccessibilityPaneTitle()Ljava/lang/CharSequence;"); return null; }
    public void sendAccessibilityEventUnchecked(android.view.accessibility.AccessibilityEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.sendAccessibilityEventUnchecked(Landroid/view/accessibility/AccessibilityEvent;)V"); }
    public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchPopulateAccessibilityEvent(Landroid/view/accessibility/AccessibilityEvent;)Z"); return false; }
    public void onPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onPopulateAccessibilityEvent(Landroid/view/accessibility/AccessibilityEvent;)V"); }
    public void onInitializeAccessibilityEvent(android.view.accessibility.AccessibilityEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onInitializeAccessibilityEvent(Landroid/view/accessibility/AccessibilityEvent;)V"); }
    public java.lang.CharSequence getAccessibilityClassName() { jet.runtime.ShimStubs.hit("android.view.View.getAccessibilityClassName()Ljava/lang/CharSequence;"); return null; }
    public java.lang.String[] getReceiveContentMimeTypes() { jet.runtime.ShimStubs.hit("android.view.View.getReceiveContentMimeTypes()[Ljava/lang/String;"); return null; }
    public void autofill(android.util.SparseArray p0) { jet.runtime.ShimStubs.hit("android.view.View.autofill(Landroid/util/SparseArray;)V"); }
    public int getAutofillType() { jet.runtime.ShimStubs.hit("android.view.View.getAutofillType()I"); return 0; }
    public java.lang.String[] getAutofillHints() { jet.runtime.ShimStubs.hit("android.view.View.getAutofillHints()[Ljava/lang/String;"); return null; }
    public int getImportantForContentCapture() { return jet_ImportantForContentCapture; }
    public void setImportantForContentCapture(int p0) { jet_ImportantForContentCapture = p0; }
    public boolean isImportantForContentCapture() { jet.runtime.ShimStubs.hit("android.view.View.isImportantForContentCapture()Z"); return false; }
    public boolean isVisibleToUserForAutofill(int p0) { jet.runtime.ShimStubs.hit("android.view.View.isVisibleToUserForAutofill(I)Z"); return false; }
    public java.lang.CharSequence getStateDescription() { return jet_StateDescription; }
    public void setStateDescription(java.lang.CharSequence p0) { jet_StateDescription = p0; }
    public void setAccessibilityTraversalBefore(int p0) { jet_AccessibilityTraversalBefore = p0; }
    public int getAccessibilityTraversalBefore() { return jet_AccessibilityTraversalBefore; }
    public void setAccessibilityTraversalAfter(int p0) { jet_AccessibilityTraversalAfter = p0; }
    public int getAccessibilityTraversalAfter() { return jet_AccessibilityTraversalAfter; }
    public boolean isScrollContainer() { return jet_ScrollContainer; }
    public void setScrollContainer(boolean p0) { jet_ScrollContainer = p0; }
    public int getDrawingCacheQuality() { return jet_DrawingCacheQuality; }
    public void setDrawingCacheQuality(int p0) { jet_DrawingCacheQuality = p0; }
    public int getNextFocusLeftId() { jet.runtime.ShimStubs.hit("android.view.View.getNextFocusLeftId()I"); return 0; }
    public int getNextFocusRightId() { jet.runtime.ShimStubs.hit("android.view.View.getNextFocusRightId()I"); return 0; }
    public int getNextFocusUpId() { jet.runtime.ShimStubs.hit("android.view.View.getNextFocusUpId()I"); return 0; }
    public int getNextFocusDownId() { jet.runtime.ShimStubs.hit("android.view.View.getNextFocusDownId()I"); return 0; }
    public int getNextFocusForwardId() { jet.runtime.ShimStubs.hit("android.view.View.getNextFocusForwardId()I"); return 0; }
    public int getNextClusterForwardId() { return jet_NextClusterForwardId; }
    public void setNextClusterForwardId(int p0) { jet_NextClusterForwardId = p0; }
    protected boolean fitSystemWindows(android.graphics.Rect p0) { jet.runtime.ShimStubs.hit("android.view.View.fitSystemWindows(Landroid/graphics/Rect;)Z"); return false; }
    public android.view.WindowInsets dispatchWindowInsetsAnimationProgress(android.view.WindowInsets p0, java.util.List p1) { jet.runtime.ShimStubs.hit("android.view.View.dispatchWindowInsetsAnimationProgress(Landroid/view/WindowInsets;Ljava/util/List;)Landroid/view/WindowInsets;"); return null; }
    public void setSystemGestureExclusionRects(java.util.List p0) { jet_SystemGestureExclusionRects = p0; }
    public java.util.List getSystemGestureExclusionRects() { return jet_SystemGestureExclusionRects; }
    public void setPreferKeepClear(boolean p0) { jet_PreferKeepClear = p0; }
    public boolean isPreferKeepClear() { return jet_PreferKeepClear; }
    public void setPreferKeepClearRects(java.util.List p0) { jet_PreferKeepClearRects = p0; }
    public java.util.List getPreferKeepClearRects() { return jet_PreferKeepClearRects; }
    public void getLocationInSurface(int[] p0) { jet.runtime.ShimStubs.hit("android.view.View.getLocationInSurface([I)V"); }
    /** AOSP: with fitsSystemWindows off (host default) the view consumes nothing; out = zero local insets. */
    public android.view.WindowInsets computeSystemWindowInsets(android.view.WindowInsets in, android.graphics.Rect outLocalInsets) { if (outLocalInsets != null) outLocalInsets.set(0, 0, 0, 0); return in; }
    public void setWillNotCacheDrawing(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.setWillNotCacheDrawing(Z)V"); }
    public boolean willNotCacheDrawing() { jet.runtime.ShimStubs.hit("android.view.View.willNotCacheDrawing()Z"); return false; }
    public void setAllowClickWhenDisabled(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.setAllowClickWhenDisabled(Z)V"); }
    public boolean isContextClickable() { jet.runtime.ShimStubs.hit("android.view.View.isContextClickable()Z"); return false; }
    protected void dispatchSetPressed(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchSetPressed(Z)V"); }
    public boolean getFilterTouchesWhenObscured() { jet.runtime.ShimStubs.hit("android.view.View.getFilterTouchesWhenObscured()Z"); return false; }
    public int getFocusable() { jet.runtime.ShimStubs.hit("android.view.View.getFocusable()I"); return 0; }
    public boolean isScreenReaderFocusable() { jet.runtime.ShimStubs.hit("android.view.View.isScreenReaderFocusable()Z"); return false; }
    public boolean isAccessibilityHeading() { jet.runtime.ShimStubs.hit("android.view.View.isAccessibilityHeading()Z"); return false; }
    public android.view.View focusSearch(int p0) { jet.runtime.ShimStubs.hit("android.view.View.focusSearch(I)Landroid/view/View;"); return null; }
    public boolean isKeyboardNavigationCluster() { return jet_KeyboardNavigationCluster; }
    public void setKeyboardNavigationCluster(boolean p0) { jet_KeyboardNavigationCluster = p0; }
    public boolean isFocusedByDefault() { return jet_FocusedByDefault; }
    public void setFocusedByDefault(boolean p0) { jet_FocusedByDefault = p0; }
    public android.view.View keyboardNavigationClusterSearch(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.keyboardNavigationClusterSearch(Landroid/view/View;I)Landroid/view/View;"); return null; }
    public boolean dispatchUnhandledMove(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.dispatchUnhandledMove(Landroid/view/View;I)Z"); return false; }
    public boolean getDefaultFocusHighlightEnabled() { jet.runtime.ShimStubs.hit("android.view.View.getDefaultFocusHighlightEnabled()Z"); return false; }
    public java.util.ArrayList getFocusables(int p0) { jet.runtime.ShimStubs.hit("android.view.View.getFocusables(I)Ljava/util/ArrayList;"); return null; }
    public void addFocusables(java.util.ArrayList p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.addFocusables(Ljava/util/ArrayList;I)V"); }
    public void addFocusables(java.util.ArrayList p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.view.View.addFocusables(Ljava/util/ArrayList;II)V"); }
    public void addKeyboardNavigationClusters(java.util.Collection p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.addKeyboardNavigationClusters(Ljava/util/Collection;I)V"); }
    public void findViewsWithText(java.util.ArrayList p0, java.lang.CharSequence p1, int p2) { jet.runtime.ShimStubs.hit("android.view.View.findViewsWithText(Ljava/util/ArrayList;Ljava/lang/CharSequence;I)V"); }
    public java.util.ArrayList getTouchables() { jet.runtime.ShimStubs.hit("android.view.View.getTouchables()Ljava/util/ArrayList;"); return null; }
    public void addTouchables(java.util.ArrayList p0) { jet.runtime.ShimStubs.hit("android.view.View.addTouchables(Ljava/util/ArrayList;)V"); }
    public boolean isAccessibilityFocused() { jet.runtime.ShimStubs.hit("android.view.View.isAccessibilityFocused()Z"); return false; }
    public boolean restoreDefaultFocus() { jet.runtime.ShimStubs.hit("android.view.View.restoreDefaultFocus()Z"); return false; }
    public boolean requestFocusFromTouch() { jet.runtime.ShimStubs.hit("android.view.View.requestFocusFromTouch()Z"); return false; }
    public boolean isImportantForAccessibility() { jet.runtime.ShimStubs.hit("android.view.View.isImportantForAccessibility()Z"); return false; }
    public android.view.ViewParent getParentForAccessibility() { jet.runtime.ShimStubs.hit("android.view.View.getParentForAccessibility()Landroid/view/ViewParent;"); return null; }
    public void addChildrenForAccessibility(java.util.ArrayList p0) { jet.runtime.ShimStubs.hit("android.view.View.addChildrenForAccessibility(Ljava/util/ArrayList;)V"); }
    public void setTransitionVisibility(int p0) { jet.runtime.ShimStubs.hit("android.view.View.setTransitionVisibility(I)V"); }
    public boolean dispatchNestedPrePerformAccessibilityAction(int p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.view.View.dispatchNestedPrePerformAccessibilityAction(ILandroid/os/Bundle;)Z"); return false; }
    public boolean performAccessibilityAction(int p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.view.View.performAccessibilityAction(ILandroid/os/Bundle;)Z"); return false; }
    public boolean isTemporarilyDetached() { jet.runtime.ShimStubs.hit("android.view.View.isTemporarilyDetached()Z"); return false; }
    public void dispatchStartTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.View.dispatchStartTemporaryDetach()V"); }
    public void onStartTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.View.onStartTemporaryDetach()V"); }
    public void dispatchFinishTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.View.dispatchFinishTemporaryDetach()V"); }
    public void onFinishTemporaryDetach() { jet.runtime.ShimStubs.hit("android.view.View.onFinishTemporaryDetach()V"); }
    public boolean dispatchKeyEventPreIme(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchKeyEventPreIme(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean dispatchKeyShortcutEvent(android.view.KeyEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchKeyShortcutEvent(Landroid/view/KeyEvent;)Z"); return false; }
    public boolean onFilterTouchEventForSecurity(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onFilterTouchEventForSecurity(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean dispatchTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean dispatchCapturedPointerEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchCapturedPointerEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchGenericMotionEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected boolean dispatchHoverEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchHoverEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected boolean dispatchGenericPointerEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchGenericPointerEvent(Landroid/view/MotionEvent;)Z"); return false; }
    protected boolean dispatchGenericFocusedEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchGenericFocusedEvent(Landroid/view/MotionEvent;)Z"); return false; }
    static boolean sJetWindowFocus;
    public void dispatchWindowFocusChanged(boolean f) { onWindowFocusChanged(f); }
    public boolean hasWindowFocus() { return sJetWindowFocus && isAttachedToWindow(); }
    /** Host: the window gained/lost focus (ViewRootImpl.windowFocusChanged). */
    public static void jetSetWindowFocus(View root, boolean f) { sJetWindowFocus = f; root.dispatchWindowFocusChanged(f); }
    protected void dispatchVisibilityChanged(android.view.View p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.dispatchVisibilityChanged(Landroid/view/View;I)V"); }
    public void dispatchDisplayHint(int p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchDisplayHint(I)V"); }
    protected void onDisplayHint(int p0) { jet.runtime.ShimStubs.hit("android.view.View.onDisplayHint(I)V"); }
    public void dispatchWindowVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchWindowVisibilityChanged(I)V"); }
    protected void onWindowVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.onWindowVisibilityChanged(I)V"); }
    public void onVisibilityAggregated(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.onVisibilityAggregated(Z)V"); }
    public int getWindowVisibility() { jet.runtime.ShimStubs.hit("android.view.View.getWindowVisibility()I"); return 0; }
    public void dispatchConfigurationChanged(android.content.res.Configuration p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchConfigurationChanged(Landroid/content/res/Configuration;)V"); }
    public boolean onKeyPreIme(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.View.onKeyPreIme(ILandroid/view/KeyEvent;)Z"); return false; }
    public boolean onKeyLongPress(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.View.onKeyLongPress(ILandroid/view/KeyEvent;)Z"); return false; }
    public boolean onKeyMultiple(int p0, int p1, android.view.KeyEvent p2) { jet.runtime.ShimStubs.hit("android.view.View.onKeyMultiple(IILandroid/view/KeyEvent;)Z"); return false; }
    public boolean onKeyShortcut(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.view.View.onKeyShortcut(ILandroid/view/KeyEvent;)Z"); return false; }
    public boolean checkInputConnectionProxy(android.view.View p0) { jet.runtime.ShimStubs.hit("android.view.View.checkInputConnectionProxy(Landroid/view/View;)Z"); return false; }
    public void createContextMenu(android.view.ContextMenu p0) { jet.runtime.ShimStubs.hit("android.view.View.createContextMenu(Landroid/view/ContextMenu;)V"); }
    protected android.view.ContextMenu.ContextMenuInfo getContextMenuInfo() { jet.runtime.ShimStubs.hit("android.view.View.getContextMenuInfo()Landroid/view/ContextMenu$ContextMenuInfo;"); return null; }
    protected void onCreateContextMenu(android.view.ContextMenu p0) { jet.runtime.ShimStubs.hit("android.view.View.onCreateContextMenu(Landroid/view/ContextMenu;)V"); }
    public boolean onTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public void onHoverChanged(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.onHoverChanged(Z)V"); }
    public void requestUnbufferedDispatch(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.requestUnbufferedDispatch(Landroid/view/MotionEvent;)V"); }
    public void requestUnbufferedDispatch(int p0) { jet.runtime.ShimStubs.hit("android.view.View.requestUnbufferedDispatch(I)V"); }
    private boolean mPivotExplicit;
    public android.graphics.Matrix getMatrix() {
        android.graphics.Matrix m = new android.graphics.Matrix();
        float px = mPivotExplicit ? mPivotX : getWidth() / 2f, py = mPivotExplicit ? mPivotY : getHeight() / 2f;
        m.setTranslate(mTranslationX, mTranslationY);
        if (mRotation != 0) m.preRotate(mRotation, px, py);
        if (mScaleX != 1f || mScaleY != 1f) m.preScale(mScaleX, mScaleY, px, py);
        return m;
    }
    public float getCameraDistance() { return jet_CameraDistance; }
    public void setCameraDistance(float p0) { jet_CameraDistance = p0; }
    public boolean isPivotSet() { return mPivotExplicit; }
    public void resetPivot() { mPivotExplicit = false; }
    public void forceHasOverlappingRendering(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.forceHasOverlappingRendering(Z)V"); }
    public boolean getHasOverlappingRendering() { jet.runtime.ShimStubs.hit("android.view.View.getHasOverlappingRendering()Z"); return false; }
    public void setTransitionAlpha(float p0) { jet_TransitionAlpha = p0; }
    public float getTransitionAlpha() { return jet_TransitionAlpha; }
    public void setForceDarkAllowed(boolean p0) { jet_ForceDarkAllowed = p0; }
    public boolean isForceDarkAllowed() { return jet_ForceDarkAllowed; }
    public boolean isDirty() { jet.runtime.ShimStubs.hit("android.view.View.isDirty()Z"); return false; }
    public void setAnimationMatrix(android.graphics.Matrix p0) { jet_AnimationMatrix = p0; }
    public android.graphics.Matrix getAnimationMatrix() { return jet_AnimationMatrix; }
    public void setOutlineSpotShadowColor(int p0) { jet_OutlineSpotShadowColor = p0; }
    public int getOutlineSpotShadowColor() { return jet_OutlineSpotShadowColor; }
    public void setOutlineAmbientShadowColor(int p0) { jet_OutlineAmbientShadowColor = p0; }
    public int getOutlineAmbientShadowColor() { return jet_OutlineAmbientShadowColor; }
    public void getFocusedRect(android.graphics.Rect p0) { jet.runtime.ShimStubs.hit("android.view.View.getFocusedRect(Landroid/graphics/Rect;)V"); }
    public boolean getGlobalVisibleRect(android.graphics.Rect p0, android.graphics.Point p1) { jet.runtime.ShimStubs.hit("android.view.View.getGlobalVisibleRect(Landroid/graphics/Rect;Landroid/graphics/Point;)Z"); return false; }
    protected boolean awakenScrollBars() { return awakenScrollBars(1000, true); }
    protected boolean awakenScrollBars(int startDelay) { return awakenScrollBars(startDelay, true); }
    /** AOSP: true when scrollbars are enabled (fade-in scheduled); invalidates if asked. */
    protected boolean awakenScrollBars(int startDelay, boolean invalidate) { boolean r = mVerticalScrollBar || mHorizontalScrollBar; if (r && invalidate) invalidate(); return r; }
    public boolean isOpaque() { jet.runtime.ShimStubs.hit("android.view.View.isOpaque()Z"); return false; }
    public void postInvalidate(int p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.view.View.postInvalidate(IIII)V"); }
    public void postInvalidateDelayed(long p0, int p1, int p2, int p3, int p4) { jet.runtime.ShimStubs.hit("android.view.View.postInvalidateDelayed(JIIII)V"); }
    public void postInvalidateOnAnimation(int p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.view.View.postInvalidateOnAnimation(IIII)V"); }
    public boolean isHorizontalFadingEdgeEnabled() { return mHorizontalFadingEdge; }
    public boolean isVerticalFadingEdgeEnabled() { return mVerticalFadingEdge; }
    protected float getTopFadingEdgeStrength() { return computeVerticalScrollOffset() > 0 ? 1f : 0f; }
    protected float getBottomFadingEdgeStrength() { return computeVerticalScrollOffset() + computeVerticalScrollExtent() < computeVerticalScrollRange() ? 1f : 0f; }
    protected float getLeftFadingEdgeStrength() { return computeHorizontalScrollOffset() > 0 ? 1f : 0f; }
    protected float getRightFadingEdgeStrength() { return computeHorizontalScrollOffset() + computeHorizontalScrollExtent() < computeHorizontalScrollRange() ? 1f : 0f; }
    public boolean isScrollbarFadingEnabled() { jet.runtime.ShimStubs.hit("android.view.View.isScrollbarFadingEnabled()Z"); return false; }
    public int getScrollBarDefaultDelayBeforeFade() { return jet_ScrollBarDefaultDelayBeforeFade; }
    public void setScrollBarDefaultDelayBeforeFade(int p0) { jet_ScrollBarDefaultDelayBeforeFade = p0; }
    public int getScrollBarFadeDuration() { return jet_ScrollBarFadeDuration; }
    public void setScrollBarFadeDuration(int p0) { jet_ScrollBarFadeDuration = p0; }
    public int getScrollBarSize() { return jet_ScrollBarSize; }
    public void setScrollBarSize(int p0) { jet_ScrollBarSize = p0; }
    public int getScrollBarStyle() { jet.runtime.ShimStubs.hit("android.view.View.getScrollBarStyle()I"); return 0; }
    protected int computeHorizontalScrollRange() { return getWidth(); }
    protected int computeHorizontalScrollOffset() { return getScrollX(); }
    protected int computeHorizontalScrollExtent() { return getWidth(); }
    protected int computeVerticalScrollRange() { return getHeight(); }
    protected int computeVerticalScrollOffset() { return getScrollY(); }
    protected int computeVerticalScrollExtent() { return getHeight(); }
    protected void onDrawScrollBars(android.graphics.Canvas p0) { jet.runtime.ShimStubs.hit("android.view.View.onDrawScrollBars(Landroid/graphics/Canvas;)V"); }
    public void onScreenStateChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.onScreenStateChanged(I)V"); }
    public boolean canResolveLayoutDirection() { jet.runtime.ShimStubs.hit("android.view.View.canResolveLayoutDirection()Z"); return false; }
    protected int getWindowAttachCount() { jet.runtime.ShimStubs.hit("android.view.View.getWindowAttachCount()I"); return 0; }
    public void onCancelPendingInputEvents() { jet.runtime.ShimStubs.hit("android.view.View.onCancelPendingInputEvents()V"); }
    public long getDrawingTime() { jet.runtime.ShimStubs.hit("android.view.View.getDrawingTime()J"); return 0; }
    public boolean isDuplicateParentStateEnabled() { jet.runtime.ShimStubs.hit("android.view.View.isDuplicateParentStateEnabled()Z"); return false; }
    public void setLayerPaint(android.graphics.Paint p0) { jet.runtime.ShimStubs.hit("android.view.View.setLayerPaint(Landroid/graphics/Paint;)V"); }
    public void buildLayer() { jet.runtime.ShimStubs.hit("android.view.View.buildLayer()V"); }
    public boolean isDrawingCacheEnabled() { jet.runtime.ShimStubs.hit("android.view.View.isDrawingCacheEnabled()Z"); return false; }
    public android.graphics.Bitmap getDrawingCache() { jet.runtime.ShimStubs.hit("android.view.View.getDrawingCache()Landroid/graphics/Bitmap;"); return null; }
    public android.graphics.Bitmap getDrawingCache(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.getDrawingCache(Z)Landroid/graphics/Bitmap;"); return null; }
    public void destroyDrawingCache() { jet.runtime.ShimStubs.hit("android.view.View.destroyDrawingCache()V"); }
    public void setDrawingCacheBackgroundColor(int p0) { jet_DrawingCacheBackgroundColor = p0; }
    public int getDrawingCacheBackgroundColor() { return jet_DrawingCacheBackgroundColor; }
    public void buildDrawingCache() { jet.runtime.ShimStubs.hit("android.view.View.buildDrawingCache()V"); }
    public void buildDrawingCache(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.buildDrawingCache(Z)V"); }
    protected boolean isPaddingOffsetRequired() { jet.runtime.ShimStubs.hit("android.view.View.isPaddingOffsetRequired()Z"); return false; }
    protected int getLeftPaddingOffset() { jet.runtime.ShimStubs.hit("android.view.View.getLeftPaddingOffset()I"); return 0; }
    protected int getRightPaddingOffset() { jet.runtime.ShimStubs.hit("android.view.View.getRightPaddingOffset()I"); return 0; }
    protected int getTopPaddingOffset() { jet.runtime.ShimStubs.hit("android.view.View.getTopPaddingOffset()I"); return 0; }
    protected int getBottomPaddingOffset() { jet.runtime.ShimStubs.hit("android.view.View.getBottomPaddingOffset()I"); return 0; }
    /** Software-rendered window (Skia CPU raster, like a window without FLAG_HARDWARE_ACCELERATED): false, matching Canvas.isHardwareAccelerated(). */
    public boolean isHardwareAccelerated() { return false; }
    public void setClipBounds(android.graphics.Rect p0) { jet_ClipBounds = p0; }
    public android.graphics.Rect getClipBounds() { return jet_ClipBounds; }
    public boolean getClipBounds(android.graphics.Rect p0) { jet.runtime.ShimStubs.hit("android.view.View.getClipBounds(Landroid/graphics/Rect;)Z"); return false; }
    public int getSolidColor() { jet.runtime.ShimStubs.hit("android.view.View.getSolidColor()I"); return 0; }
    public void setLeftTopRightBottom(int p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.view.View.setLeftTopRightBottom(IIII)V"); }
    public void dispatchDrawableHotspotChanged(float p0, float p1) { jet.runtime.ShimStubs.hit("android.view.View.dispatchDrawableHotspotChanged(FF)V"); }
    public void setBackgroundTintBlendMode(android.graphics.BlendMode p0) { jet_BackgroundTintBlendMode = p0; }
    public android.graphics.PorterDuff.Mode getBackgroundTintMode() { jet.runtime.ShimStubs.hit("android.view.View.getBackgroundTintMode()Landroid/graphics/PorterDuff$Mode;"); return null; }
    public android.graphics.BlendMode getBackgroundTintBlendMode() { return jet_BackgroundTintBlendMode; }
    public int getForegroundGravity() { return jet_ForegroundGravity; }
    public void setForegroundGravity(int p0) { jet_ForegroundGravity = p0; }
    public void setForegroundTintList(android.content.res.ColorStateList p0) { jet_ForegroundTintList = p0; }
    public android.content.res.ColorStateList getForegroundTintList() { return jet_ForegroundTintList; }
    public void setForegroundTintMode(android.graphics.PorterDuff.Mode p0) { jet_ForegroundTintMode = p0; }
    public void setForegroundTintBlendMode(android.graphics.BlendMode p0) { jet_ForegroundTintBlendMode = p0; }
    public android.graphics.PorterDuff.Mode getForegroundTintMode() { return jet_ForegroundTintMode; }
    public android.graphics.BlendMode getForegroundTintBlendMode() { return jet_ForegroundTintBlendMode; }
    public void onDrawForeground(android.graphics.Canvas p0) { jet.runtime.ShimStubs.hit("android.view.View.onDrawForeground(Landroid/graphics/Canvas;)V"); }
    public int getSourceLayoutResId() { jet.runtime.ShimStubs.hit("android.view.View.getSourceLayoutResId()I"); return 0; }
    protected void dispatchSetSelected(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchSetSelected(Z)V"); }
    protected void dispatchSetActivated(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchSetActivated(Z)V"); }
    public void transformMatrixToLocal(android.graphics.Matrix p0) { jet.runtime.ShimStubs.hit("android.view.View.transformMatrixToLocal(Landroid/graphics/Matrix;)V"); }
    @SuppressWarnings("unchecked") public <T extends android.view.View> T requireViewById(int p0) { T v = (T) findViewById(p0); if (v == null) throw new IllegalArgumentException("ID does not reference a View inside this View"); return v; }
    public android.view.View findViewWithTag(java.lang.Object p0) { jet.runtime.ShimStubs.hit("android.view.View.findViewWithTag(Ljava/lang/Object;)Landroid/view/View;"); return null; }
    protected void onAnimationStart() { jet.runtime.ShimStubs.hit("android.view.View.onAnimationStart()V"); }
    protected void onAnimationEnd() { jet.runtime.ShimStubs.hit("android.view.View.onAnimationEnd()V"); }
    protected boolean onSetAlpha(int p0) { jet.runtime.ShimStubs.hit("android.view.View.onSetAlpha(I)Z"); return false; }
    public boolean gatherTransparentRegion(android.graphics.Region p0) { jet.runtime.ShimStubs.hit("android.view.View.gatherTransparentRegion(Landroid/graphics/Region;)Z"); return false; }
    public void onWindowSystemUiVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.onWindowSystemUiVisibilityChanged(I)V"); }
    public void dispatchWindowSystemUiVisiblityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchWindowSystemUiVisiblityChanged(I)V"); }
    public void dispatchSystemUiVisibilityChanged(int p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchSystemUiVisibilityChanged(I)V"); }
    public void cancelDragAndDrop() { jet.runtime.ShimStubs.hit("android.view.View.cancelDragAndDrop()V"); }
    public boolean onDragEvent(android.view.DragEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onDragEvent(Landroid/view/DragEvent;)Z"); return false; }
    public boolean dispatchDragEvent(android.view.DragEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchDragEvent(Landroid/view/DragEvent;)Z"); return false; }
    protected boolean overScrollBy(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, boolean p8) { jet.runtime.ShimStubs.hit("android.view.View.overScrollBy(IIIIIIIIZ)Z"); return false; }
    protected void onOverScrolled(int p0, int p1, boolean p2, boolean p3) { jet.runtime.ShimStubs.hit("android.view.View.onOverScrolled(IIZZ)V"); }
    public boolean canResolveTextDirection() { jet.runtime.ShimStubs.hit("android.view.View.canResolveTextDirection()Z"); return false; }
    public boolean isTextDirectionResolved() { jet.runtime.ShimStubs.hit("android.view.View.isTextDirectionResolved()Z"); return false; }
    public boolean canResolveTextAlignment() { jet.runtime.ShimStubs.hit("android.view.View.canResolveTextAlignment()Z"); return false; }
    public boolean isTextAlignmentResolved() { jet.runtime.ShimStubs.hit("android.view.View.isTextAlignmentResolved()Z"); return false; }
    public android.view.PointerIcon onResolvePointerIcon(android.view.MotionEvent p0, int p1) { jet.runtime.ShimStubs.hit("android.view.View.onResolvePointerIcon(Landroid/view/MotionEvent;I)Landroid/view/PointerIcon;"); return null; }
    public boolean hasPointerCapture() { jet.runtime.ShimStubs.hit("android.view.View.hasPointerCapture()Z"); return false; }
    public void requestPointerCapture() { jet.runtime.ShimStubs.hit("android.view.View.requestPointerCapture()V"); }
    public void releasePointerCapture() { jet.runtime.ShimStubs.hit("android.view.View.releasePointerCapture()V"); }
    public void onPointerCaptureChange(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.onPointerCaptureChange(Z)V"); }
    public void dispatchPointerCaptureChanged(boolean p0) { jet.runtime.ShimStubs.hit("android.view.View.dispatchPointerCaptureChanged(Z)V"); }
    public boolean onCapturedPointerEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.view.View.onCapturedPointerEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public int getScrollCaptureHint() { return jet_ScrollCaptureHint; }
    public void setScrollCaptureHint(int p0) { jet_ScrollCaptureHint = p0; }
    public void dispatchScrollCaptureSearch(android.graphics.Rect p0, android.graphics.Point p1, java.util.function.Consumer p2) { jet.runtime.ShimStubs.hit("android.view.View.dispatchScrollCaptureSearch(Landroid/graphics/Rect;Landroid/graphics/Point;Ljava/util/function/Consumer;)V"); }
    public void onScrollCaptureSearch(android.graphics.Rect p0, android.graphics.Point p1, java.util.function.Consumer p2) { jet.runtime.ShimStubs.hit("android.view.View.onScrollCaptureSearch(Landroid/graphics/Rect;Landroid/graphics/Point;Ljava/util/function/Consumer;)V"); }
    public void setAutoHandwritingEnabled(boolean p0) { jet_AutoHandwritingEnabled = p0; }
    public boolean isAutoHandwritingEnabled() { return jet_AutoHandwritingEnabled; }
    public void onCreateViewTranslationRequest(int[] p0, java.util.function.Consumer p1) { jet.runtime.ShimStubs.hit("android.view.View.onCreateViewTranslationRequest([ILjava/util/function/Consumer;)V"); }
    public void onCreateVirtualViewTranslationRequests(long[] p0, int[] p1, java.util.function.Consumer p2) { jet.runtime.ShimStubs.hit("android.view.View.onCreateVirtualViewTranslationRequests([J[ILjava/util/function/Consumer;)V"); }
    public void onVirtualViewTranslationResponses(android.util.LongSparseArray p0) { jet.runtime.ShimStubs.hit("android.view.View.onVirtualViewTranslationResponses(Landroid/util/LongSparseArray;)V"); }
    // ---- END generated SDK surface ----
    /** AOSP: null when content capture is disabled for the app (always, on JET). */
    public final android.view.contentcapture.ContentCaptureSession getContentCaptureSession() { return null; }
    public void setContentCaptureSession(android.view.contentcapture.ContentCaptureSession s) {}
    /** UI translation (API 31): no translation service on JET; callbacks are stored, never invoked. */
    private android.view.translation.ViewTranslationCallback mJetTranslationCb;
    public void setViewTranslationCallback(android.view.translation.ViewTranslationCallback cb) { mJetTranslationCb = cb; }
    public void clearViewTranslationCallback() { mJetTranslationCb = null; }
    public android.view.translation.ViewTranslationCallback getViewTranslationCallback() { return mJetTranslationCb; }
}
