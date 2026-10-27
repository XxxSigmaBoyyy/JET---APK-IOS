package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
public class ScrollView extends FrameLayout {
    public ScrollView(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public ScrollView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }

    public ScrollView(Context c) { this(c, null); }
    public ScrollView(Context c, AttributeSet a) { super(c, a); if (a != null) mFillViewport = a.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", "fillViewport", false); }
    private boolean mFillViewport;
    public boolean isFillViewport() { return mFillViewport; }
    public void setFillViewport(boolean fill) { if (fill != mFillViewport) { mFillViewport = fill; requestLayout(); } }
    /** AOSP ScrollView.measureChildWithMargins + onMeasure: the child gets its own width spec (minus padding and
     *  margins) and an UNSPECIFIED height that still carries the viewport size as a hint (API 23+
     *  makeSafeMeasureSpec); margins count toward the ScrollView's wrap size; fillViewport stretches a shorter child. */
    @Override protected void onMeasure(int ws, int hs) {
        View c = getChildCount() > 0 ? getChildAt(0) : null;
        if (c == null || c.getVisibility() == View.GONE) { super.onMeasure(ws, hs); return; }
        android.view.ViewGroup.LayoutParams lp = c.getLayoutParams();
        int mh = lp.leftMargin + lp.rightMargin, mv = lp.topMargin + lp.bottomMargin;
        int w = getChildMeasureSpec(ws, mPaddingLeft + mPaddingRight + mh, lp.width);
        int hint = Math.max(0, MeasureSpec.getSize(hs) - mPaddingTop - mPaddingBottom - mv);
        c.measure(w, MeasureSpec.makeMeasureSpec(MeasureSpec.getMode(hs) == MeasureSpec.UNSPECIFIED ? 0 : hint, MeasureSpec.UNSPECIFIED));
        setMeasuredDimension(resolveSize(Math.max(c.getMeasuredWidth() + mPaddingLeft + mPaddingRight + mh, getSuggestedMinimumWidth()), ws),
                resolveSize(Math.max(c.getMeasuredHeight() + mPaddingTop + mPaddingBottom + mv, getSuggestedMinimumHeight()), hs));
        int vh = getMeasuredHeight() - mPaddingTop - mPaddingBottom - mv;
        if (mFillViewport && MeasureSpec.getMode(hs) != MeasureSpec.UNSPECIFIED && c.getMeasuredHeight() < vh)
            c.measure(getChildMeasureSpec(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), mPaddingLeft + mPaddingRight + mh, lp.width),
                    MeasureSpec.makeMeasureSpec(vh, MeasureSpec.EXACTLY));
    }
}
