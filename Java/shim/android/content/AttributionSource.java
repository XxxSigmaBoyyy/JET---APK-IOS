package android.content;
/** Identity of this app process for permission checks (uid/pid/package, optional attribution tag; no chain). */
public final class AttributionSource implements android.os.Parcelable {
    private final int mUid, mPid; private final String mPkg, mTag; private final AttributionSource mNext;
    public static final android.os.Parcelable.Creator<AttributionSource> CREATOR = null;
    AttributionSource(int uid, int pid, String pkg, String tag, AttributionSource next) { mUid = uid; mPid = pid; mPkg = pkg; mTag = tag; mNext = next; }
    public static AttributionSource myAttributionSource() { android.app.Application a = android.app.Application.jetCurrent; return new AttributionSource(android.os.Process.myUid(), android.os.Process.myPid(), a == null ? null : a.getPackageName(), null, null); }
    public int getUid() { return mUid; } public int getPid() { return mPid; } public String getPackageName() { return mPkg; } public String getAttributionTag() { return mTag; }
    public AttributionSource getNext() { return mNext; } public boolean isTrusted(Context c) { return mUid == android.os.Process.myUid(); }
    public void enforceCallingUid() { if (mUid != android.os.Process.myUid()) throw new SecurityException("Calling uid does not match"); }
    public boolean checkCallingUid() { return mUid == android.os.Process.myUid(); }
    public java.util.Set<String> getRenouncedPermissions() { return java.util.Collections.emptySet(); }
    public int getDeviceId() { return 0; }
    public AttributionSource withNextAttributionSource(AttributionSource next) { return new AttributionSource(mUid, mPid, mPkg, mTag, next); }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    @Override public boolean equals(Object o) { if (!(o instanceof AttributionSource)) return false; AttributionSource a = (AttributionSource) o; return mUid == a.mUid && java.util.Objects.equals(mPkg, a.mPkg) && java.util.Objects.equals(mTag, a.mTag); }
    @Override public int hashCode() { return java.util.Objects.hash(mUid, mPkg, mTag); }
    @Override public String toString() { return "AttributionSource { uid = " + mUid + ", packageName = " + mPkg + ", attributionTag = " + mTag + " }"; }
    public static final class Builder {
        private int mUid, mPid = -1; private String mPkg, mTag; private AttributionSource mNext;
        public Builder(int uid) { mUid = uid; } public Builder(AttributionSource s) { mUid = s.mUid; mPid = s.mPid; mPkg = s.mPkg; mTag = s.mTag; mNext = s.mNext; }
        public Builder setPid(int p) { mPid = p; return this; } public Builder setPackageName(String p) { mPkg = p; return this; } public Builder setAttributionTag(String t) { mTag = t; return this; }
        public Builder setNext(AttributionSource n) { mNext = n; return this; } public Builder setNextAttributionSource(AttributionSource n) { mNext = n; return this; }
        public Builder setRenouncedPermissions(java.util.Set<String> p) { return this; } public Builder setDeviceId(int d) { return this; }
        public AttributionSource build() { return new AttributionSource(mUid, mPid, mPkg, mTag, mNext); }
    }
}
