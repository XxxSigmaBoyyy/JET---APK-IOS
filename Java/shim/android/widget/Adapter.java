package android.widget;
import android.view.View;
import android.view.ViewGroup;
public interface Adapter {
    int getCount();
    Object getItem(int position);
    long getItemId(int position);
    View getView(int position, View convertView, ViewGroup parent);
    boolean isEmpty();
    int IGNORE_ITEM_VIEW_TYPE = -1, NO_SELECTION = Integer.MIN_VALUE;
    default void registerDataSetObserver(android.database.DataSetObserver o) {}
    default void unregisterDataSetObserver(android.database.DataSetObserver o) {}
    default boolean hasStableIds() { return false; }
    default int getItemViewType(int position) { return 0; }
    default int getViewTypeCount() { return 1; }
    default CharSequence[] getAutofillOptions() { return null; }
}
