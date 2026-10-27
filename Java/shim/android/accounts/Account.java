package android.accounts;
public class Account implements android.os.Parcelable {
    public final String name, type;
    public static final android.os.Parcelable.Creator<Account> CREATOR = null;
    public Account(String name, String type) { if (name == null || name.isEmpty()) throw new IllegalArgumentException("the name must not be empty: " + name); if (type == null || type.isEmpty()) throw new IllegalArgumentException("the type must not be empty: " + type); this.name = name; this.type = type; }
    public Account(android.os.Parcel in) { this("x", "x"); }
    @Override public boolean equals(Object o) { return o instanceof Account && name.equals(((Account) o).name) && type.equals(((Account) o).type); }
    @Override public int hashCode() { return 31 * (17 * 31 + name.hashCode()) + type.hashCode(); }
    @Override public String toString() { return "Account {name=" + name + ", type=" + type + "}"; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
