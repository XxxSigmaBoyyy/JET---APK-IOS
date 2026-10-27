package android.accounts;
public class AuthenticatorDescription implements android.os.Parcelable {
    public final String type, packageName; public final int labelId, iconId, smallIconId, accountPreferencesId; public final boolean customTokens;
    public static final android.os.Parcelable.Creator<AuthenticatorDescription> CREATOR = null;
    public AuthenticatorDescription(String type, String packageName, int labelId, int iconId, int smallIconId, int prefId, boolean customTokens) { this.type = type; this.packageName = packageName; this.labelId = labelId; this.iconId = iconId; this.smallIconId = smallIconId; this.accountPreferencesId = prefId; this.customTokens = customTokens; }
    public AuthenticatorDescription(String type, String packageName, int labelId, int iconId, int smallIconId, int prefId) { this(type, packageName, labelId, iconId, smallIconId, prefId, false); }
    public static AuthenticatorDescription newKey(String type) { return new AuthenticatorDescription(type, null, 0, 0, 0, 0); }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
}
