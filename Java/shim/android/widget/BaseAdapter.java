package android.widget;
public abstract class BaseAdapter implements ListAdapter, SpinnerAdapter {
    public boolean isEmpty() { return getCount() == 0; }
    public boolean hasStableIds() { return false; }
    public int getItemViewType(int position) { return 0; }
    public int getViewTypeCount() { return 1; }
    public boolean areAllItemsEnabled() { return true; }
    public boolean isEnabled(int position) { return true; }
    public void notifyDataSetChanged() {}
    public void notifyDataSetInvalidated() {}
}
