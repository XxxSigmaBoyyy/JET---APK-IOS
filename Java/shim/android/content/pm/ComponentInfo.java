package android.content.pm;
public class ComponentInfo extends PackageItemInfo {
    public ApplicationInfo applicationInfo;
    public String processName;
    public boolean enabled = true, exported, directBootAware;
    public ComponentInfo() {}
    public ComponentInfo(ComponentInfo o) { super(o); if (o != null) { applicationInfo = o.applicationInfo; processName = o.processName; enabled = o.enabled; exported = o.exported; } }
    public boolean isEnabled() { return enabled; }
}
