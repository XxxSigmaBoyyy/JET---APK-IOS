package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;
/** AOSP RadioGroup: vertical LinearLayout by default; at most one RadioButton child checked; checkedButton attr. */
public class RadioGroup extends LinearLayout {
    public interface OnCheckedChangeListener { void onCheckedChanged(RadioGroup group, int checkedId); }
    public static class LayoutParams extends LinearLayout.LayoutParams {
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(int w, int h, float wt) { super(w, h, wt); }
        public LayoutParams(android.view.ViewGroup.LayoutParams o) { super(o); }
        public LayoutParams(Context c, AttributeSet a) { super(c, a); }
    }
    private int mCheckedId = View.NO_ID; private boolean mProtect; private OnCheckedChangeListener mListener;
    private final CompoundButton.OnCheckedChangeListener mChildListener = (b, checked) -> {
        if (mProtect) return;
        mProtect = true; if (mCheckedId != View.NO_ID) setCheckedStateForView(mCheckedId, false); mProtect = false;
        setCheckedId(b.getId());
    };
    public RadioGroup(Context c) { this(c, null); }
    public RadioGroup(Context c, AttributeSet a) {
        super(c, a);
        XmlAttrs x = mJetAttrs != null ? mJetAttrs : XmlAttrs.of(a);
        if (x == null || x.get("orientation") == null) mOrientation = VERTICAL;
        if (x != null) { String v = x.get("checkedButton"); if (v != null && v.startsWith("@")) mCheckedId = ResTable.ref(v); }
    }
    @Override public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof RadioButton) {
            RadioButton rb = (RadioButton) child;
            if (rb.getId() == View.NO_ID) rb.setId(View.generateViewId());
            if (rb.isChecked()) { mProtect = true; if (mCheckedId != View.NO_ID && mCheckedId != rb.getId()) setCheckedStateForView(mCheckedId, false); mProtect = false; setCheckedId(rb.getId()); }
            else if (mCheckedId == rb.getId()) { mProtect = true; rb.setChecked(true); mProtect = false; }
            rb.mJetWidgetListener = mChildListener;
        }
    }
    @Override public void onViewRemoved(View child) { if (child instanceof RadioButton) ((RadioButton) child).mJetWidgetListener = null; super.onViewRemoved(child); }
    public void check(int id) {
        if (id != View.NO_ID && id == mCheckedId) return;
        if (mCheckedId != View.NO_ID) setCheckedStateForView(mCheckedId, false);
        if (id != View.NO_ID) setCheckedStateForView(id, true);
        setCheckedId(id);
    }
    private void setCheckedId(int id) { boolean changed = id != mCheckedId; mCheckedId = id; if (mListener != null && changed) mListener.onCheckedChanged(this, mCheckedId); }
    private void setCheckedStateForView(int id, boolean checked) { View v = findViewById(id); if (v instanceof RadioButton) { mProtect = true; ((RadioButton) v).setChecked(checked); mProtect = false; } }
    public int getCheckedRadioButtonId() { return mCheckedId; }
    public void clearCheck() { check(View.NO_ID); }
    public void setOnCheckedChangeListener(OnCheckedChangeListener l) { mListener = l; }
    @Override public LayoutParams generateLayoutParams(AttributeSet a) { return new LayoutParams(getContext(), a); }
    @Override protected LinearLayout.LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-2, -2); }
    @Override public CharSequence getAccessibilityClassName() { return RadioGroup.class.getName(); }
}
