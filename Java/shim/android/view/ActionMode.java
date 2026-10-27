package android.view;
public abstract class ActionMode {
    public interface Callback {
        boolean onCreateActionMode(ActionMode mode, Menu menu);
        boolean onPrepareActionMode(ActionMode mode, Menu menu);
        boolean onActionItemClicked(ActionMode mode, MenuItem item);
        void onDestroyActionMode(ActionMode mode);
    }
    public abstract void finish();
    public abstract View getCustomView();
    public abstract Menu getMenu();
    public abstract void setTitle(CharSequence title);
    public abstract void setTitle(int resId);
}
