package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
public class HorizontalScrollView extends FrameLayout {
    public HorizontalScrollView(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public HorizontalScrollView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }

    public HorizontalScrollView(Context c) { this(c, null); }
    public HorizontalScrollView(Context c, AttributeSet a) { super(c, a); if (a != null) mFillViewport = a.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", "fillViewport", false); }
    private boolean mFillViewport;
    public boolean isFillViewport() { return mFillViewport; }
    public void setFillViewport(boolean fill) { if (fill != mFillViewport) { mFillViewport = fill; requestLayout(); } }
    /** AOSP HorizontalScrollView: the child is measured with an UNSPECIFIED width (content may be wider than the
     *  viewport) and its own height spec; fillViewport stretches a narrower child to the viewport width. */
    @Override protected void onMeasure(int ws, int hs) {
        android.view.View c = getChildCount() > 0 ? getChildAt(0) : null;
        if (c == null || c.getVisibility() == android.view.View.GONE) { super.onMeasure(ws, hs); return; }
        android.view.ViewGroup.LayoutParams lp = c.getLayoutParams();
        int mh = lp.leftMargin + lp.rightMargin, mv = lp.topMargin + lp.bottomMargin;
        int h = getChildMeasureSpec(hs, mPaddingTop + mPaddingBottom + mv, lp.height);
        int hint = Math.max(0, MeasureSpec.getSize(ws) - mPaddingLeft - mPaddingRight - mh);
        c.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.getMode(ws) == MeasureSpec.UNSPECIFIED ? 0 : hint, MeasureSpec.UNSPECIFIED), h);
        setMeasuredDimension(resolveSize(Math.max(c.getMeasuredWidth() + mPaddingLeft + mPaddingRight + mh, getSuggestedMinimumWidth()), ws),
                resolveSize(Math.max(c.getMeasuredHeight() + mPaddingTop + mPaddingBottom + mv, getSuggestedMinimumHeight()), hs));
        int vw = getMeasuredWidth() - mPaddingLeft - mPaddingRight - mh;
        if (mFillViewport && MeasureSpec.getMode(ws) != MeasureSpec.UNSPECIFIED && c.getMeasuredWidth() < vw)
            c.measure(MeasureSpec.makeMeasureSpec(vw, MeasureSpec.EXACTLY),
                    getChildMeasureSpec(MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY), mPaddingTop + mPaddingBottom + mv, lp.height));
    }
}
