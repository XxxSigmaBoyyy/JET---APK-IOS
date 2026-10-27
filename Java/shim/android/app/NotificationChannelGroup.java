package android.app;
public final class NotificationChannelGroup implements android.os.Parcelable {
    private final String mId; private CharSequence mName; private String mDesc; private boolean mBlocked;
    final java.util.List<NotificationChannel> jetChannels = new java.util.ArrayList<>();
    public static final android.os.Parcelable.Creator<NotificationChannelGroup> CREATOR = null;
    public NotificationChannelGroup(String id, CharSequence name) { mId = id; mName = name; }
    public String getId() { return mId; } public CharSequence getName() { return mName; }
    public String getDescription() { return mDesc; } public void setDescription(String d) { mDesc = d; }
    public boolean isBlocked() { return mBlocked; }
    public java.util.List<NotificationChannel> getChannels() { return new java.util.ArrayList<>(jetChannels); }
    public NotificationChannelGroup clone() { NotificationChannelGroup g = new NotificationChannelGroup(mId, mName); g.mDesc = mDesc; return g; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
