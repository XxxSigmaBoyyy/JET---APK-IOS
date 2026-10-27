package android.widget;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
/** AOSP PopupWindow state machine. JET has a single window: a shown popup is measured/laid out at its anchor position and tracked as an overlay (jetShowing). */
public class PopupWindow {
    public interface OnDismissListener { void onDismiss(); }
    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0, INPUT_METHOD_NEEDED = 1, INPUT_METHOD_NOT_NEEDED = 2;
    public static final java.util.List<PopupWindow> jetShowing = new java.util.ArrayList<>();
    private Context mContext; private View mContentView; private int mWidth = -2, mHeight = -2, mAnimationStyle = -1, mInputMethodMode, mSoftInputMode, mWindowLayoutType = 1000, mGravity, mX, mY;
    private boolean mFocusable, mTouchable = true, mOutsideTouchable, mClipping = true, mClippedToScreen, mSplitTouch = true, mLaidOutInScreen, mAttachedInDecor = true, mTouchModal = true, mOverlapAnchor, mShowing, mAboveAnchor;
    private Drawable mBackground; private float mElevation; private OnDismissListener mOnDismissListener; private View.OnTouchListener mTouchInterceptor;
    private android.transition.Transition mEnter, mExit; private Rect mEpicenter; private View mAnchor;
    public PopupWindow(Context c) { this(c, null); }
    public PopupWindow(Context c, AttributeSet a) { this(c, a, 0x01010076 /*popupWindowStyle*/); }
    public PopupWindow(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public PopupWindow(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { mContext = c; }
    public PopupWindow() { this(null, 0, 0); }
    public PopupWindow(View contentView) { this(contentView, 0, 0); }
    public PopupWindow(int width, int height) { this(null, width, height); }
    public PopupWindow(View contentView, int width, int height) { this(contentView, width, height, false); }
    public PopupWindow(View contentView, int width, int height, boolean focusable) { if (contentView != null) { mContext = contentView.getContext(); } setContentView(contentView); mWidth = width; mHeight = height; mFocusable = focusable; }
    public void setEnterTransition(android.transition.Transition t) { mEnter = t; } public android.transition.Transition getEnterTransition() { return mEnter; }
    public void setExitTransition(android.transition.Transition t) { mExit = t; } public android.transition.Transition getExitTransition() { return mExit; }
    public Rect getEpicenterBounds() { return mEpicenter == null ? null : new Rect(mEpicenter); } public void setEpicenterBounds(Rect r) { mEpicenter = r == null ? null : new Rect(r); }
    public Drawable getBackground() { return mBackground; } public void setBackgroundDrawable(Drawable d) { mBackground = d; }
    public float getElevation() { return mElevation; } public void setElevation(float e) { mElevation = e; }
    public int getAnimationStyle() { return mAnimationStyle; } public void setIgnoreCheekPress() {} public void setAnimationStyle(int s) { mAnimationStyle = s; }
    public View getContentView() { return mContentView; }
    public void setContentView(View v) { if (mShowing) return; mContentView = v; if (mContext == null && v != null) mContext = v.getContext(); }
    public void setTouchInterceptor(View.OnTouchListener l) { mTouchInterceptor = l; }
    public boolean isFocusable() { return mFocusable; } public void setFocusable(boolean f) { mFocusable = f; }
    public int getInputMethodMode() { return mInputMethodMode; } public void setInputMethodMode(int m) { mInputMethodMode = m; }
    public void setSoftInputMode(int m) { mSoftInputMode = m; } public int getSoftInputMode() { return mSoftInputMode; }
    public boolean isTouchable() { return mTouchable; } public void setTouchable(boolean t) { mTouchable = t; }
    public boolean isOutsideTouchable() { return mOutsideTouchable; } public void setOutsideTouchable(boolean t) { mOutsideTouchable = t; }
    public boolean isClippingEnabled() { return mClipping; } public void setClippingEnabled(boolean e) { mClipping = e; }
    public boolean isClippedToScreen() { return mClippedToScreen; } public void setIsClippedToScreen(boolean e) { mClippedToScreen = e; }
    public boolean isSplitTouchEnabled() { return mSplitTouch; } public void setSplitTouchEnabled(boolean e) { mSplitTouch = e; }
    public boolean isLaidOutInScreen() { return mLaidOutInScreen; } public void setIsLaidOutInScreen(boolean e) { mLaidOutInScreen = e; }
    public boolean isAttachedInDecor() { return mAttachedInDecor; } public void setAttachedInDecor(boolean e) { mAttachedInDecor = e; }
    public void setWindowLayoutType(int t) { mWindowLayoutType = t; } public int getWindowLayoutType() { return mWindowLayoutType; }
    public boolean isTouchModal() { return mTouchModal; } public void setTouchModal(boolean m) { mTouchModal = m; }
    public void setWindowLayoutMode(int w, int h) { mWidth = w; mHeight = h; }
    public int getHeight() { return mHeight; } public void setHeight(int h) { mHeight = h; }
    public int getWidth() { return mWidth; } public void setWidth(int w) { mWidth = w; }
    public void setOverlapAnchor(boolean o) { mOverlapAnchor = o; } public boolean getOverlapAnchor() { return mOverlapAnchor; }
    public boolean isShowing() { return mShowing; }
    private static int spec(int lp, int max) { return lp >= 0 ? View.MeasureSpec.makeMeasureSpec(lp, View.MeasureSpec.EXACTLY) : View.MeasureSpec.makeMeasureSpec(max, lp == -1 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST); }
    private void jetShow(int x, int y) {
        if (mShowing || mContentView == null) return;
        int sw = android.util.DisplayMetrics.jetScreenW, sh = android.util.DisplayMetrics.jetScreenH;
        mContentView.measure(spec(mWidth, sw), spec(mHeight, sh));
        int w = mContentView.getMeasuredWidth(), h = mContentView.getMeasuredHeight();
        if (mClipping) { x = Math.max(0, Math.min(x, sw - w)); y = Math.max(0, Math.min(y, sh - h)); }
        mX = x; mY = y; mContentView.layout(0, 0, w, h);
        try { android.view.ViewGroup.jetAttachTree(mContentView); } catch (Throwable ignored) {}
        mShowing = true; jetShowing.add(this);
    }
    public int jetX() { return mX; } public int jetY() { return mY; }
    public void showAtLocation(View parent, int gravity, int x, int y) {
        mGravity = gravity; int sw = android.util.DisplayMetrics.jetScreenW, sh = android.util.DisplayMetrics.jetScreenH;
        if (mContentView != null) mContentView.measure(spec(mWidth, sw), spec(mHeight, sh));
        int w = mContentView == null ? 0 : mContentView.getMeasuredWidth(), h = mContentView == null ? 0 : mContentView.getMeasuredHeight();
        int hg = gravity & 7, vg = gravity & 0x70;
        int px = hg == 1 ? (sw - w) / 2 + x : hg == 5 ? sw - w - x : x, py = vg == 0x10 ? (sh - h) / 2 + y : vg == 0x50 ? sh - h - y : y;
        jetShow(px, py);
    }
    public void showAsDropDown(View anchor) { showAsDropDown(anchor, 0, 0); }
    public void showAsDropDown(View anchor, int xoff, int yoff) { showAsDropDown(anchor, xoff, yoff, 0x800033); }
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        mAnchor = anchor; int[] loc = new int[2]; anchor.getLocationInWindow(loc);
        int sh = android.util.DisplayMetrics.jetScreenH;
        if (mContentView != null) mContentView.measure(spec(mWidth, android.util.DisplayMetrics.jetScreenW), spec(mHeight, sh));
        int h = mContentView == null ? 0 : mContentView.getMeasuredHeight(), w = mContentView == null ? 0 : mContentView.getMeasuredWidth();
        int x = loc[0] + xoff; if ((android.view.Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection()) & 7) == 5) x = loc[0] + anchor.getWidth() - w - xoff;
        int y = loc[1] + (mOverlapAnchor ? 0 : anchor.getHeight()) + yoff;
        mAboveAnchor = y + h > sh && loc[1] - h - yoff >= 0; if (mAboveAnchor) y = loc[1] - h - yoff;
        jetShow(x, y);
    }
    public boolean isAboveAnchor() { return mAboveAnchor; }
    public int getMaxAvailableHeight(View anchor) { return getMaxAvailableHeight(anchor, 0); }
    public int getMaxAvailableHeight(View anchor, int yOffset) { return getMaxAvailableHeight(anchor, yOffset, false); }
    public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations) { int[] loc = new int[2]; anchor.getLocationInWindow(loc); int sh = android.util.DisplayMetrics.jetScreenH; int below = sh - (loc[1] + anchor.getHeight()) - yOffset, above = loc[1] + yOffset; int r = Math.max(below, above); if (mBackground != null) { Rect p = new Rect(); mBackground.getPadding(p); r -= p.top + p.bottom; } return r; }
    public void dismiss() { if (!mShowing) return; mShowing = false; jetShowing.remove(this); if (mOnDismissListener != null) mOnDismissListener.onDismiss(); }
    public void setOnDismissListener(OnDismissListener l) { mOnDismissListener = l; }
    public void update() { if (mShowing && mContentView != null) { mContentView.requestLayout(); } }
    public void update(int width, int height) { mWidth = width; mHeight = height; update(); }
    public void update(int x, int y, int width, int height) { update(x, y, width, height, false); }
    public void update(int x, int y, int width, int height, boolean force) { if (width >= 0) mWidth = width; if (height >= 0) mHeight = height; if (mShowing) { mX = x; mY = y; } update(); }
    public void update(View anchor, int width, int height) { update(anchor, 0, 0, width, height); }
    public void update(View anchor, int xoff, int yoff, int width, int height) { if (width >= 0) mWidth = width; if (height >= 0) mHeight = height; if (mShowing) { dismissQuiet(); showAsDropDown(anchor, xoff, yoff); } }
    private void dismissQuiet() { mShowing = false; jetShowing.remove(this); }
}
