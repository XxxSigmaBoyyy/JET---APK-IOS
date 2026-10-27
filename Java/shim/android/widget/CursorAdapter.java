package android.widget;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
/** Minimal CursorAdapter for FOSS apps (NoNonsense Notes ResourceCursorAdapter). */
public abstract class CursorAdapter extends BaseAdapter implements Filterable {
    protected Context mContext;
    protected Cursor mCursor;
    protected boolean mDataValid;
    protected int mRowIDColumn = -1;
    public static final int FLAG_AUTO_REQUERY = 0x01;
    public static final int FLAG_REGISTER_CONTENT_OBSERVER = 0x02;
    public CursorAdapter(Context ctx, Cursor c) { this(ctx, c, FLAG_AUTO_REQUERY); }
    public CursorAdapter(Context ctx, Cursor c, boolean autoRequery) { this(ctx, c, autoRequery ? FLAG_AUTO_REQUERY : FLAG_REGISTER_CONTENT_OBSERVER); }
    public CursorAdapter(Context ctx, Cursor c, int flags) { mContext = ctx; init(c); }
    void init(Cursor c) {
        mCursor = c; mDataValid = c != null;
        mRowIDColumn = mDataValid ? c.getColumnIndexOrThrow("_id") : -1;
    }
    public Cursor getCursor() { return mCursor; }
    @Override public int getCount() { return mDataValid && mCursor != null ? mCursor.getCount() : 0; }
    @Override public Object getItem(int pos) { if (mDataValid && mCursor != null) { mCursor.moveToPosition(pos); return mCursor; } return null; }
    @Override public long getItemId(int pos) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(pos) && mRowIDColumn >= 0) return mCursor.getLong(mRowIDColumn);
        return 0;
    }
    @Override public View getView(int pos, View convert, ViewGroup parent) {
        if (!mDataValid || mCursor == null) throw new IllegalStateException("no cursor");
        if (!mCursor.moveToPosition(pos)) throw new IllegalStateException("bad position " + pos);
        View v = convert == null ? newView(mContext, mCursor, parent) : convert;
        bindView(v, mContext, mCursor);
        return v;
    }
    public abstract View newView(Context ctx, Cursor c, ViewGroup parent);
    public abstract void bindView(View v, Context ctx, Cursor c);
    public View newDropDownView(Context ctx, Cursor c, ViewGroup parent) { return newView(ctx, c, parent); }
    public void changeCursor(Cursor c) { Cursor old = swapCursor(c); if (old != null) old.close(); }
    public Cursor swapCursor(Cursor c) {
        if (c == mCursor) return null;
        Cursor old = mCursor; init(c); notifyDataSetChanged(); return old;
    }
    public Filter getFilter() { return new Filter() {
        @Override protected FilterResults performFiltering(CharSequence constraint) { FilterResults r = new FilterResults(); r.count = getCount(); r.values = mCursor; return r; }
        @Override protected void publishResults(CharSequence constraint, FilterResults results) { notifyDataSetChanged(); }
    }; }
    public CharSequence convertToString(Cursor c) { return c == null ? "" : c.toString(); }
}
