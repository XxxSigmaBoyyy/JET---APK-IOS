package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
public abstract class AdapterView<T extends Adapter> extends ViewGroup {
    public interface OnItemSelectedListener {
        void onItemSelected(AdapterView<?> parent, View view, int position, long id);
        void onNothingSelected(AdapterView<?> parent);
    }
    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }
    public interface OnItemLongClickListener { boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id); }
    public static final int INVALID_POSITION = -1, ITEM_VIEW_TYPE_IGNORE = -1, ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2; public static final long INVALID_ROW_ID = Long.MIN_VALUE;
    protected OnItemSelectedListener selected; protected OnItemClickListener click; protected OnItemLongClickListener longClick;
    protected T adapter; protected int mSelected = INVALID_POSITION; private View mEmpty;
    public AdapterView(Context c) { this(c, null); }
    public AdapterView(Context c, AttributeSet a) { super(c, a); }
    public AdapterView(Context c, AttributeSet a, int s) { super(c, a, s); } public AdapterView(Context c, AttributeSet a, int s, int r) { super(c, a, s, r); }
    public void setOnItemSelectedListener(OnItemSelectedListener l) { selected = l; } public final OnItemSelectedListener getOnItemSelectedListener() { return selected; }
    public void setOnItemClickListener(OnItemClickListener l) { click = l; } public final OnItemClickListener getOnItemClickListener() { return click; }
    public void setOnItemLongClickListener(OnItemLongClickListener l) { longClick = l; } public final OnItemLongClickListener getOnItemLongClickListener() { return longClick; }
    public void setAdapter(T adapter) { this.adapter = adapter; mSelected = adapter != null && adapter.getCount() > 0 ? 0 : INVALID_POSITION; requestLayout(); invalidate(); }
    public T getAdapter() { return adapter; }
    public int getCount() { return adapter == null ? 0 : adapter.getCount(); }
    public void setSelection(int p) { mSelected = p; requestLayout(); invalidate(); }
    public int getSelectedItemPosition() { return mSelected; }
    public long getSelectedItemId() { return adapter == null || mSelected < 0 ? INVALID_ROW_ID : adapter.getItemId(mSelected); }
    public Object getSelectedItem() { return adapter == null || mSelected < 0 || mSelected >= adapter.getCount() ? null : adapter.getItem(mSelected); }
    public View getSelectedView() { return getChildCount() > 0 ? getChildAt(0) : null; }
    public Object getItemAtPosition(int p) { return adapter == null || p < 0 ? null : adapter.getItem(p); }
    public long getItemIdAtPosition(int p) { return adapter == null || p < 0 ? INVALID_ROW_ID : adapter.getItemId(p); }
    public int getPositionForView(View v) { return indexOfChild(v); } public int getFirstVisiblePosition() { return 0; } public int getLastVisiblePosition() { return getChildCount() - 1; }
    public void setEmptyView(View v) { mEmpty = v; } public View getEmptyView() { return mEmpty; }
    public boolean performItemClick(View v, int p, long id) { if (click == null) return false; click.onItemClick(this, v, p, id); return true; }
    protected void jetFireSelected() { if (selected == null) return; if (mSelected >= 0) selected.onItemSelected(this, getSelectedView(), mSelected, getSelectedItemId()); else selected.onNothingSelected(this); }
}
