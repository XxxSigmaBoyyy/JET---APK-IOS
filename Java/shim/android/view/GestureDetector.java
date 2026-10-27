package android.view;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
/** JET: GestureDetector following AOSP's state machine (single-pointer subset). */
public class GestureDetector {
    public interface OnGestureListener {
        boolean onDown(MotionEvent e); void onShowPress(MotionEvent e); boolean onSingleTapUp(MotionEvent e);
        boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy); void onLongPress(MotionEvent e);
        boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy);
    }
    public interface OnDoubleTapListener { boolean onSingleTapConfirmed(MotionEvent e); boolean onDoubleTap(MotionEvent e); boolean onDoubleTapEvent(MotionEvent e); }
    public interface OnContextClickListener { boolean onContextClick(MotionEvent e); }
    public static class SimpleOnGestureListener implements OnGestureListener, OnDoubleTapListener, OnContextClickListener {
        public boolean onSingleTapUp(MotionEvent e) { return false; } public void onLongPress(MotionEvent e) {}
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) { return false; }
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) { return false; }
        public void onShowPress(MotionEvent e) {} public boolean onDown(MotionEvent e) { return false; }
        public boolean onDoubleTap(MotionEvent e) { return false; } public boolean onDoubleTapEvent(MotionEvent e) { return false; }
        public boolean onSingleTapConfirmed(MotionEvent e) { return false; } public boolean onContextClick(MotionEvent e) { return false; }
    }
    private static final int SHOW_PRESS = 1, LONG_PRESS = 2, TAP = 3;
    private final OnGestureListener mListener; private OnDoubleTapListener mDoubleTapListener; private OnContextClickListener mContextClickListener;
    private final int mTouchSlopSquare, mDoubleTapSlopSquare, mMinFling, mMaxFling;
    private final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout(), LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout(), DOUBLE_TAP_TIMEOUT = 300;
    private boolean mStillDown, mDeferConfirmSingleTap, mInLongPress, mAlwaysInTapRegion, mAlwaysInBiggerTapRegion, mIsDoubleTapping, mLongpressEnabled = true;
    private MotionEvent mCurrentDownEvent, mPreviousUpEvent;
    private float mLastFocusX, mLastFocusY, mDownFocusX, mDownFocusY;
    private VelocityTracker mVelocityTracker;
    private final Handler mHandler;
    public GestureDetector(OnGestureListener l) { this(null, l, null); }
    public GestureDetector(OnGestureListener l, Handler h) { this(null, l, h); }
    public GestureDetector(Context c, OnGestureListener l) { this(c, l, null); }
    public GestureDetector(Context c, OnGestureListener l, Handler h, boolean unused) { this(c, l, h); }
    public GestureDetector(Context c, OnGestureListener l, Handler h) {
        mListener = l;
        if (l instanceof OnDoubleTapListener) mDoubleTapListener = (OnDoubleTapListener) l;
        if (l instanceof OnContextClickListener) mContextClickListener = (OnContextClickListener) l;
        ViewConfiguration vc = ViewConfiguration.get(c);
        int slop = vc.getScaledTouchSlop(); mTouchSlopSquare = slop * slop;
        int dts = slop * 12; mDoubleTapSlopSquare = dts * dts;
        mMinFling = vc.getScaledMinimumFlingVelocity(); mMaxFling = vc.getScaledMaximumFlingVelocity();
        mHandler = new Handler(h != null ? h.getLooper() : android.os.Looper.myLooper() != null ? android.os.Looper.myLooper() : android.os.Looper.getMainLooper()) {
            @Override public void handleMessage(Message m) {
                switch (m.what) {
                    case SHOW_PRESS: mListener.onShowPress(mCurrentDownEvent); break;
                    case LONG_PRESS: mHandler.removeMessages(TAP); mDeferConfirmSingleTap = false; mInLongPress = true; mListener.onLongPress(mCurrentDownEvent); break;
                    case TAP: if (mDoubleTapListener != null) { if (!mStillDown) mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent); else mDeferConfirmSingleTap = true; } break;
                }
            }
        };
    }
    public void setOnDoubleTapListener(OnDoubleTapListener l) { mDoubleTapListener = l; }
    public void setContextClickListener(OnContextClickListener l) { mContextClickListener = l; }
    public void setIsLongpressEnabled(boolean e) { mLongpressEnabled = e; }
    public boolean isLongpressEnabled() { return mLongpressEnabled; }
    public boolean onGenericMotionEvent(MotionEvent e) { return false; }
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);
        final float fx = ev.getX(), fy = ev.getY();
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mDoubleTapListener != null) {
                    boolean hadTap = mHandler.hasMessages(TAP);
                    if (hadTap) mHandler.removeMessages(TAP);
                    if (mCurrentDownEvent != null && mPreviousUpEvent != null && hadTap && isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, ev)) {
                        mIsDoubleTapping = true;
                        handled |= mDoubleTapListener.onDoubleTap(mCurrentDownEvent);
                        handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                    } else mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
                }
                mDownFocusX = mLastFocusX = fx; mDownFocusY = mLastFocusY = fy;
                mCurrentDownEvent = MotionEvent.obtain(ev);
                mAlwaysInTapRegion = true; mAlwaysInBiggerTapRegion = true; mStillDown = true; mInLongPress = false; mDeferConfirmSingleTap = false;
                if (mLongpressEnabled) { mHandler.removeMessages(LONG_PRESS); mHandler.sendEmptyMessageAtTime(LONG_PRESS, mCurrentDownEvent.getDownTime() + TAP_TIMEOUT + LONGPRESS_TIMEOUT); }
                mHandler.sendEmptyMessageAtTime(SHOW_PRESS, mCurrentDownEvent.getDownTime() + TAP_TIMEOUT);
                handled |= mListener.onDown(ev);
                break;
            case MotionEvent.ACTION_MOVE: {
                if (mInLongPress) break;
                final float scrollX = mLastFocusX - fx, scrollY = mLastFocusY - fy;
                if (mIsDoubleTapping) handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                else if (mAlwaysInTapRegion) {
                    int dx = (int) (fx - mDownFocusX), dy = (int) (fy - mDownFocusY), dist = dx * dx + dy * dy;
                    if (dist > mTouchSlopSquare) {
                        handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                        mLastFocusX = fx; mLastFocusY = fy; mAlwaysInTapRegion = false;
                        mHandler.removeMessages(TAP); mHandler.removeMessages(SHOW_PRESS); mHandler.removeMessages(LONG_PRESS);
                    }
                    if (dist > mDoubleTapSlopSquare) mAlwaysInBiggerTapRegion = false;
                } else if (Math.abs(scrollX) >= 1 || Math.abs(scrollY) >= 1) {
                    handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                    mLastFocusX = fx; mLastFocusY = fy;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mStillDown = false;
                MotionEvent currentUp = MotionEvent.obtain(ev);
                if (mIsDoubleTapping) handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                else if (mInLongPress) { mHandler.removeMessages(TAP); mInLongPress = false; }
                else if (mAlwaysInTapRegion) {
                    handled = mListener.onSingleTapUp(ev);
                    if (mDeferConfirmSingleTap && mDoubleTapListener != null) mDoubleTapListener.onSingleTapConfirmed(ev);
                } else {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxFling);
                    float vy = mVelocityTracker.getYVelocity(), vx = mVelocityTracker.getXVelocity();
                    if (Math.abs(vy) > mMinFling || Math.abs(vx) > mMinFling) handled = mListener.onFling(mCurrentDownEvent, ev, vx, vy);
                }
                mPreviousUpEvent = currentUp;
                if (mVelocityTracker != null) { mVelocityTracker.recycle(); mVelocityTracker = null; }
                mIsDoubleTapping = false; mDeferConfirmSingleTap = false;
                mHandler.removeMessages(SHOW_PRESS); mHandler.removeMessages(LONG_PRESS);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeMessages(SHOW_PRESS); mHandler.removeMessages(LONG_PRESS); mHandler.removeMessages(TAP);
                if (mVelocityTracker != null) { mVelocityTracker.recycle(); mVelocityTracker = null; }
                mIsDoubleTapping = false; mStillDown = false; mAlwaysInTapRegion = false; mAlwaysInBiggerTapRegion = false; mDeferConfirmSingleTap = false; mInLongPress = false;
                break;
        }
        return handled;
    }
    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        if (!mAlwaysInBiggerTapRegion) return false;
        long dt = secondDown.getEventTime() - firstUp.getEventTime();
        if (dt > DOUBLE_TAP_TIMEOUT || dt < 40) return false;
        int dx = (int) firstDown.getX() - (int) secondDown.getX(), dy = (int) firstDown.getY() - (int) secondDown.getY();
        return dx * dx + dy * dy < mDoubleTapSlopSquare;
    }
}
