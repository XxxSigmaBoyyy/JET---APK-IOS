package android.content.pm;
public class ProviderInfo extends ComponentInfo {
    public String authority, readPermission, writePermission;
    public boolean grantUriPermissions, multiprocess, forceUriPermissions;
    public int initOrder, flags;
    public ProviderInfo() {}
    public ProviderInfo(ProviderInfo o) { super(o); if (o != null) { authority = o.authority; initOrder = o.initOrder; } }
}
