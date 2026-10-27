package android.widget;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
/** ResourceCursorAdapter: inflates layoutResId for new rows. */
public abstract class ResourceCursorAdapter extends CursorAdapter {
    private int mLayout, mDropDownLayout;
    private LayoutInflater mInflater;
    public ResourceCursorAdapter(Context ctx, int layout, Cursor c) { this(ctx, layout, c, true); }
    public ResourceCursorAdapter(Context ctx, int layout, Cursor c, boolean autoRequery) {
        super(ctx, c, autoRequery); mLayout = mDropDownLayout = layout; mInflater = LayoutInflater.from(ctx);
    }
    public ResourceCursorAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, c, flags); mLayout = mDropDownLayout = layout; mInflater = LayoutInflater.from(ctx);
    }
    public void setViewResource(int layout) { mLayout = layout; }
    public void setDropDownViewResource(int layout) { mDropDownLayout = layout; }
    @Override public View newView(Context ctx, Cursor c, ViewGroup parent) {
        return mInflater.inflate(mLayout, parent, false);
    }
    @Override public View newDropDownView(Context ctx, Cursor c, ViewGroup parent) {
        return mInflater.inflate(mDropDownLayout, parent, false);
    }
}
