package android.content;
/** AOSP LocusId: an opaque app-chosen id for a UI shortcut/bubble. */
public final class LocusId implements android.os.Parcelable {
    private final String id;
    public LocusId(String id) { if (id == null || id.isEmpty()) throw new IllegalArgumentException("id cannot be empty"); this.id = id; }
    public String getId() { return id; }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public boolean equals(Object o) { return o instanceof LocusId && id.equals(((LocusId) o).id); }
    @Override public String toString() { return "LocusId[" + (id.length() > 10 ? id.substring(0, 10) + "..." : id) + "]"; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel out, int flags) { out.writeString(id); }
    public static final android.os.Parcelable.Creator<LocusId> CREATOR = new android.os.Parcelable.Creator<LocusId>() {
        public LocusId createFromParcel(android.os.Parcel in) { return new LocusId(in.readString()); }
        public LocusId[] newArray(int n) { return new LocusId[n]; }
    };
}
