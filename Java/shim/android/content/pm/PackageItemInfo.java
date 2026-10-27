package android.content.pm;
public class PackageItemInfo {
    public android.os.Bundle metaData = new android.os.Bundle();
    public String name;
    public String packageName;
    public int labelRes, icon, logo, banner;
    public CharSequence nonLocalizedLabel;
    public PackageItemInfo() {}
    public PackageItemInfo(PackageItemInfo o) { if (o != null) { name = o.name; packageName = o.packageName; metaData = o.metaData; labelRes = o.labelRes; icon = o.icon; nonLocalizedLabel = o.nonLocalizedLabel; } }
    public CharSequence loadLabel(PackageManager pm) { return nonLocalizedLabel != null ? nonLocalizedLabel : (name != null ? name : packageName); }
    public android.graphics.drawable.Drawable loadIcon(PackageManager pm) { return null; }
    /** AOSP: metaData int resource -> pm.getXml(packageName, res, applicationInfo); null when absent. */
    public android.content.res.XmlResourceParser loadXmlMetaData(PackageManager pm, String name) {
        if (metaData != null) { int r = metaData.getInt(name); if (r != 0) return pm.getXml(packageName, r, null); }
        return null;
    }
}
