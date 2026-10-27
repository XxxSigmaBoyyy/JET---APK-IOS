package android.view;
public abstract class ActionProvider {
    public interface VisibilityListener { void onActionProviderVisibilityChanged(boolean isVisible); }
    private final android.content.Context ctx; private VisibilityListener vl;
    public ActionProvider(android.content.Context c) { ctx = c; }
    @Deprecated public abstract View onCreateActionView();
    public View onCreateActionView(MenuItem forItem) { return onCreateActionView(); }
    public boolean overridesItemVisibility() { return false; }
    public boolean isVisible() { return true; }
    public void refreshVisibility() { if (vl != null && overridesItemVisibility()) vl.onActionProviderVisibilityChanged(isVisible()); }
    public boolean onPerformDefaultAction() { return false; }
    public boolean hasSubMenu() { return false; }
    public void onPrepareSubMenu(SubMenu subMenu) {}
    public void setVisibilityListener(VisibilityListener l) { vl = l; }
}
