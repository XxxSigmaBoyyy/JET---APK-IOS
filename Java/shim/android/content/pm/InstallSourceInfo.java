package android.content.pm;
/** JET: API 30 install-source record. The translated app is "sideloaded" (no installer), like an F-Droid-less install. */
public final class InstallSourceInfo implements android.os.Parcelable {
    private final String installer;
    public InstallSourceInfo(String installer) { this.installer = installer; }
    public String getInstallingPackageName() { return installer; }
    public String getInitiatingPackageName() { return installer; }
    public String getOriginatingPackageName() { return null; }
    public String getUpdateOwnerPackageName() { return null; }
    public SigningInfo getInitiatingPackageSigningInfo() { return null; }
    public int getPackageSource() { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) { p.writeString(installer); }
}
