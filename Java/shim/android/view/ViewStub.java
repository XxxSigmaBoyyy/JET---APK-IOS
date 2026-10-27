package android.view;
import android.content.Context;
import android.util.AttributeSet;
/** AOSP android.view.ViewStub: invisible zero-size placeholder that inflates android:layout into its parent on inflate()
 *  or setVisibility(VISIBLE/INVISIBLE), replacing itself (keeping index and layout params; android:inflatedId -> id). */
public final class ViewStub extends View {
    public interface OnInflateListener { void onInflate(ViewStub stub, View inflated); }
    private int mInflatedId = NO_ID, mLayoutResource;
    private java.lang.ref.WeakReference<View> mInflatedViewRef;
    private LayoutInflater mInflater; private OnInflateListener mInflateListener;
    public ViewStub(Context c) { this(c, 0); }
    public ViewStub(Context c, int layoutResource) { this(c, (AttributeSet) null); mLayoutResource = layoutResource; }
    public ViewStub(Context c, AttributeSet a) { this(c, a, 0, 0); }
    public ViewStub(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public ViewStub(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        jet.runtime.XmlAttrs x = mJetAttrs;
        if (x != null) {
            String v = x.get("inflatedId"); if (v != null) { int id = jet.runtime.ResTable.refId(v); if (id != 0) mInflatedId = id; }
            v = x.get("layout"); if (v != null) mLayoutResource = jet.runtime.ResTable.refId(v);
        }
        super.setVisibility(GONE); setWillNotDraw(true);
    }
    public int getInflatedId() { return mInflatedId; }
    public void setInflatedId(int id) { mInflatedId = id; }
    public int getLayoutResource() { return mLayoutResource; }
    public void setLayoutResource(int r) { mLayoutResource = r; }
    public void setLayoutInflater(LayoutInflater i) { mInflater = i; }
    public LayoutInflater getLayoutInflater() { return mInflater; }
    public void setOnInflateListener(OnInflateListener l) { mInflateListener = l; }
    @Override protected void onMeasure(int ws, int hs) { setMeasuredDimension(0, 0); }
    @Override public void draw(android.graphics.Canvas c) {}
    @Override protected void dispatchDraw(android.graphics.Canvas c) {}
    @Override public void setVisibility(int visibility) {
        if (mInflatedViewRef != null) { View v = mInflatedViewRef.get(); if (v != null) v.setVisibility(visibility); else throw new IllegalStateException("setVisibility called on un-referenced view"); }
        else { super.setVisibility(visibility); if (visibility == VISIBLE || visibility == INVISIBLE) inflate(); }
    }
    public View inflate() {
        ViewParent vp = getParent();
        if (!(vp instanceof ViewGroup)) throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        if (mLayoutResource == 0) throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
        ViewGroup parent = (ViewGroup) vp;
        LayoutInflater inf = mInflater != null ? mInflater : LayoutInflater.from(getContext());
        View view = inf.inflate(mLayoutResource, parent, false);
        if (mInflatedId != NO_ID) view.setId(mInflatedId);
        int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) parent.addView(view, index, lp); else parent.addView(view, index);
        mInflatedViewRef = new java.lang.ref.WeakReference<>(view);
        if (mInflateListener != null) mInflateListener.onInflate(this, view);
        return view;
    }
}
