package android.app;
/** AOSP RemoteInput (inline-reply key/value). Minimal shim: notifications never fire here, so only identity is needed. */
public final class RemoteInput implements android.os.Parcelable {
    private final String resultKey; private final CharSequence label; private final boolean allowFreeform; private final android.os.Bundle extras;
    private RemoteInput(String rk, CharSequence l, boolean f, android.os.Bundle e) { resultKey = rk; label = l; allowFreeform = f; extras = e == null ? new android.os.Bundle() : e; }
    public String getResultKey() { return resultKey; }
    public CharSequence getLabel() { return label; }
    public boolean getAllowFreeFormInput() { return allowFreeform; }
    public android.os.Bundle getExtras() { return extras; }
    public CharSequence[] getChoices() { return null; }
    public int getEditChoicesBeforeSending() { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel out, int flags) { out.writeString(resultKey); out.writeCharSequence(label); out.writeInt(allowFreeform ? 1 : 0); out.writeBundle(extras); }
    public static final android.os.Parcelable.Creator<RemoteInput> CREATOR = new android.os.Parcelable.Creator<RemoteInput>() {
        public RemoteInput createFromParcel(android.os.Parcel in) { return new RemoteInput(in.readString(), in.readCharSequence(), in.readInt() != 0, in.readBundle()); }
        public RemoteInput[] newArray(int n) { return new RemoteInput[n]; }
    };
    public static final class Builder {
        private final String resultKey; private CharSequence label; private boolean allowFreeform = true; private final android.os.Bundle extras = new android.os.Bundle();
        public Builder(String resultKey) { if (resultKey == null) throw new IllegalArgumentException("Result key can't be null"); this.resultKey = resultKey; }
        public Builder setLabel(CharSequence l) { label = l; return this; }
        public Builder setAllowFreeFormInput(boolean f) { allowFreeform = f; return this; }
        public Builder setChoices(CharSequence[] c) { return this; }
        public Builder setEditChoicesBeforeSending(int m) { return this; }
        public Builder addExtras(android.os.Bundle b) { if (b != null) extras.putAll(b); return this; }
        public android.os.Bundle getExtras() { return extras; }
        public RemoteInput build() { return new RemoteInput(resultKey, label, allowFreeform, extras); }
    }
}
