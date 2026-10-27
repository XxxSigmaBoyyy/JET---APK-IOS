package android.widget;
public abstract class Filter {
    protected static class FilterResults { public Object values; public int count; public FilterResults() {} }
    public interface FilterListener { void onFilterComplete(int count); }
    public Filter() {}
    protected abstract FilterResults performFiltering(CharSequence constraint);
    protected abstract void publishResults(CharSequence constraint, FilterResults results);
    public final void filter(CharSequence c) { filter(c, null); }
    public final void filter(CharSequence c, FilterListener l) { FilterResults r = performFiltering(c); publishResults(c, r == null ? new FilterResults() : r); if (l != null) l.onFilterComplete(r == null ? 0 : r.count); }
    public CharSequence convertResultToString(Object o) { return o == null ? "" : o.toString(); }
}
