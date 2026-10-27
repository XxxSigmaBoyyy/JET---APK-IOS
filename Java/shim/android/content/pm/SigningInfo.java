package android.content.pm;
/** JET: API 28 SigningInfo (single, unrotated signer from the APK). */
public final class SigningInfo implements android.os.Parcelable {
    private final Signature[] sigs;
    public SigningInfo() { this(new Signature[0]); }
    public SigningInfo(Signature[] s) { sigs = s; }
    public boolean hasMultipleSigners() { return sigs.length > 1; }
    public boolean hasPastSigningCertificates() { return false; }
    public Signature[] getSigningCertificateHistory() { return sigs; }
    public Signature[] getApkContentsSigners() { return sigs; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
