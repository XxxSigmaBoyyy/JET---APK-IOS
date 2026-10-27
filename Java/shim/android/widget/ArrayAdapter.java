package android.widget;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.*;
public class ArrayAdapter<T> extends BaseAdapter implements Filterable, ThemedSpinnerAdapter {
    private final Context mContext; private final LayoutInflater mInflater; private int mResource, mDropDownResource, mFieldId; private List<T> mObjects; private boolean mNotify = true;
    private android.content.res.Resources.Theme mDropTheme;
    public ArrayAdapter(Context c, int res) { this(c, res, 0, new ArrayList<>()); }
    public ArrayAdapter(Context c, int res, int field) { this(c, res, field, new ArrayList<>()); }
    public ArrayAdapter(Context c, int res, T[] o) { this(c, res, 0, new ArrayList<>(Arrays.asList(o))); }
    public ArrayAdapter(Context c, int res, int field, T[] o) { this(c, res, field, new ArrayList<>(Arrays.asList(o))); }
    public ArrayAdapter(Context c, int res, List<T> o) { this(c, res, 0, o); }
    public ArrayAdapter(Context c, int res, int field, List<T> o) { mContext = c; mInflater = LayoutInflater.from(c); mResource = mDropDownResource = res; mFieldId = field; mObjects = o; }
    public static ArrayAdapter<CharSequence> createFromResource(Context c, int arrayId, int res) { return new ArrayAdapter<>(c, res, 0, c.getResources().getTextArray(arrayId)); }
    public void add(T o) { mObjects.add(o); if (mNotify) notifyDataSetChanged(); }
    public void addAll(Collection<? extends T> c) { mObjects.addAll(c); if (mNotify) notifyDataSetChanged(); }
    @SafeVarargs public final void addAll(T... items) { Collections.addAll(mObjects, items); if (mNotify) notifyDataSetChanged(); }
    public void insert(T o, int i) { mObjects.add(i, o); if (mNotify) notifyDataSetChanged(); } public void remove(T o) { mObjects.remove(o); if (mNotify) notifyDataSetChanged(); }
    public void clear() { mObjects.clear(); if (mNotify) notifyDataSetChanged(); } public void sort(Comparator<? super T> c) { mObjects.sort(c); if (mNotify) notifyDataSetChanged(); }
    public void setNotifyOnChange(boolean n) { mNotify = n; } public Context getContext() { return mContext; }
    public int getCount() { return mObjects.size(); } public T getItem(int p) { return mObjects.get(p); } public int getPosition(T o) { return mObjects.indexOf(o); } public long getItemId(int p) { return p; }
    public View getView(int p, View cv, ViewGroup parent) { return make(p, cv, parent, mResource); }
    public View getDropDownView(int p, View cv, ViewGroup parent) { return make(p, cv, parent, mDropDownResource); }
    public void setDropDownViewResource(int r) { mDropDownResource = r; }
    public void setDropDownViewTheme(android.content.res.Resources.Theme t) { mDropTheme = t; } public android.content.res.Resources.Theme getDropDownViewTheme() { return mDropTheme; }
    private View make(int p, View cv, ViewGroup parent, int res) {
        View v = cv != null ? cv : mInflater.inflate(res, parent, false);
        TextView tv;
        try { tv = mFieldId == 0 ? (TextView) v : (TextView) v.findViewById(mFieldId); } catch (ClassCastException e) { throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e); }
        if (tv == null) throw new RuntimeException("Failed to find view with ID " + mFieldId + " in item layout");
        T item = getItem(p); tv.setText(item instanceof CharSequence ? (CharSequence) item : String.valueOf(item));
        return v;
    }
    public Filter getFilter() { return new Filter() { protected FilterResults performFiltering(CharSequence c) { FilterResults r = new FilterResults(); r.values = mObjects; r.count = mObjects.size(); return r; } protected void publishResults(CharSequence c, FilterResults r) { notifyDataSetChanged(); } }; }
    public CharSequence[] getAutofillOptions() { return null; }
}
