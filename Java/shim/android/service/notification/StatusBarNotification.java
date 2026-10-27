package android.service.notification;
public class StatusBarNotification implements android.os.Parcelable {
    private final String mPkg, mTag; private final int mId; private final android.app.Notification mN; private final long mPost;
    public StatusBarNotification(String pkg, String opPkg, int id, String tag, int uid, int initialPid, android.app.Notification n, android.os.UserHandle user, String overrideGroupKey, long postTime) { mPkg = pkg; mTag = tag; mId = id; mN = n; mPost = postTime; }
    public String getPackageName() { return mPkg; } public String getOpPkg() { return mPkg; } public String getTag() { return mTag; } public int getId() { return mId; }
    public android.app.Notification getNotification() { return mN; } public long getPostTime() { return mPost; }
    public String getKey() { return "0|" + mPkg + "|" + mId + "|" + mTag + "|" + android.os.Process.myUid(); }
    public String getGroupKey() { return getKey(); } public boolean isGroup() { return false; } public boolean isOngoing() { return mN != null && (mN.flags & android.app.Notification.FLAG_ONGOING_EVENT) != 0; }
    public boolean isClearable() { return !isOngoing(); } public int getUid() { return android.os.Process.myUid(); } public android.os.UserHandle getUser() { return android.os.Process.myUserHandle(); }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private java.lang.String jet_OverrideGroupKey;
    public boolean isAppGroup() { jet.runtime.ShimStubs.hit("android.service.notification.StatusBarNotification.isAppGroup()Z"); return false; }
    public android.service.notification.StatusBarNotification clone() { jet.runtime.ShimStubs.hit("android.service.notification.StatusBarNotification.clone()Landroid/service/notification/StatusBarNotification;"); return null; }
    public int getUserId() { jet.runtime.ShimStubs.hit("android.service.notification.StatusBarNotification.getUserId()I"); return 0; }
    public void setOverrideGroupKey(java.lang.String p0) { jet_OverrideGroupKey = p0; }
    public java.lang.String getOverrideGroupKey() { return jet_OverrideGroupKey; }
    // ---- END generated SDK surface ----
}
