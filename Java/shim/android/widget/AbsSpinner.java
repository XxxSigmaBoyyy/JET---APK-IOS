package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
/** Shows the selected adapter item (Android's AbsSpinner/Spinner closed state). */
public abstract class AbsSpinner extends AdapterView<SpinnerAdapter> {
    private int mShown = -2; private boolean mFiredInitial;
    public AbsSpinner(Context c) { this(c, null); }
    public AbsSpinner(Context c, AttributeSet a) { super(c, a); }
    public AbsSpinner(Context c, AttributeSet a, int s) { super(c, a, s); } public AbsSpinner(Context c, AttributeSet a, int s, int r) { super(c, a, s, r); }
    @Override public void setAdapter(SpinnerAdapter a) { super.setAdapter(a); mShown = -2; }
    @Override public SpinnerAdapter getAdapter() { return super.getAdapter(); }
    @Override public void setSelection(int p) { if (p != mSelected) { super.setSelection(p); mShown = -2; if (mFiredInitial) jetFireSelected(); } }
    public void setSelection(int p, boolean animate) { setSelection(p); }
    public void setEntries(CharSequence[] e) {}
    public int pointToPosition(int x, int y) { return mSelected; }
    private void sync() {
        if (Boolean.getBoolean("jet.dbgSpinner")) System.err.println("[SPIN] sync sel=" + mSelected + " shown=" + mShown + " adapter=" + adapter + " n=" + (adapter == null ? -1 : adapter.getCount()));
        if (mShown == mSelected) return;
        View old = getChildCount() > 0 ? getChildAt(0) : null; removeAllViewsInLayout();
        if (adapter != null && mSelected >= 0 && mSelected < adapter.getCount()) {
            View v = adapter.getView(mSelected, old, this);
            if (v != null) addViewInLayout(v, 0, v.getLayoutParams() != null ? v.getLayoutParams() : generateDefaultLayoutParams());
        }
        mShown = mSelected;
        if (Boolean.getBoolean("jet.dbgSpinner")) { System.err.println("[SPIN] after sync children=" + getChildCount() + (getChildCount() > 0 ? " " + getChildAt(0) + " parent=" + getChildAt(0).getParent() : "")); new Throwable().printStackTrace(); }
    }
    @Override protected void onMeasure(int ws, int hs) {
        sync(); int w = 0, h = 0;
        if (getChildCount() > 0) { View c = getChildAt(0); measureChild(c, ws, hs); w = c.getMeasuredWidth(); h = c.getMeasuredHeight(); }
        int arrow = getClass().getName().contains("Spinner") ? Math.round(24 * getResources().getDisplayMetrics().density) : 0;
        setMeasuredDimension(resolveSize(Math.max(getSuggestedMinimumWidth(), w + arrow + mPaddingLeft + mPaddingRight), ws), resolveSize(Math.max(getSuggestedMinimumHeight(), h + mPaddingTop + mPaddingBottom), hs));
    }
    @Override protected void onLayout(boolean ch, int l, int t, int r, int b) {
        if (getChildCount() > 0) { View c = getChildAt(0); int ch0 = c.getMeasuredHeight(); int y = mPaddingTop + Math.max(0, (getHeight() - mPaddingTop - mPaddingBottom - ch0) / 2); c.layout(mPaddingLeft, y, mPaddingLeft + c.getMeasuredWidth(), y + ch0); }
        if (!mFiredInitial) { mFiredInitial = true; post(this::jetFireSelected); }
    }
}
