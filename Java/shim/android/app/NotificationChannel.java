package android.app;
/** Notification channel: a plain value object, as on Android (the system copy lives in NotificationManager). */
public final class NotificationChannel implements android.os.Parcelable {
    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";
    public static final String EDIT_CONVERSATION = "conversation";
    public static final String EDIT_IMPORTANCE = "importance";
    public static final String EDIT_LAUNCHER = "launcher";
    public static final String EDIT_LOCKED_DEVICE = "locked";
    public static final String EDIT_SOUND = "sound";
    public static final String EDIT_VIBRATION = "vibration";
    public static final String EDIT_ZEN = "zen";
    private final String mId; private CharSequence mName; private int mImportance; private String mDesc, mGroup, mConvParent, mConvId;
    private android.net.Uri mSound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI; private android.media.AudioAttributes mAttrs = Notification.AUDIO_ATTRIBUTES_DEFAULT;
    private boolean mLights, mVibration, mShowBadge = true, mBypassDnd, mBubbles; private int mLightColor, mLockscreen = -1000; private long[] mPattern;
    public static final android.os.Parcelable.Creator<NotificationChannel> CREATOR = null;
    public NotificationChannel(String id, CharSequence name, int importance) { mId = id; mName = name; mImportance = importance; }
    public String getId() { return mId; }
    public CharSequence getName() { return mName; } public void setName(CharSequence n) { mName = n; }
    public int getImportance() { return mImportance; } public void setImportance(int i) { mImportance = i; }
    public String getDescription() { return mDesc; } public void setDescription(String d) { mDesc = d; }
    public String getGroup() { return mGroup; } public void setGroup(String g) { mGroup = g; }
    public android.net.Uri getSound() { return mSound; } public android.media.AudioAttributes getAudioAttributes() { return mAttrs; }
    public void setSound(android.net.Uri s, android.media.AudioAttributes a) { mSound = s; mAttrs = a; }
    public boolean shouldShowLights() { return mLights; } public void enableLights(boolean b) { mLights = b; }
    public int getLightColor() { return mLightColor; } public void setLightColor(int c) { mLightColor = c; }
    public boolean shouldVibrate() { return mVibration; } public void enableVibration(boolean b) { mVibration = b; }
    public long[] getVibrationPattern() { return mPattern; } public void setVibrationPattern(long[] p) { mPattern = p; mVibration = p != null && p.length > 0; }
    public boolean canShowBadge() { return mShowBadge; } public void setShowBadge(boolean b) { mShowBadge = b; }
    public boolean canBypassDnd() { return mBypassDnd; } public void setBypassDnd(boolean b) { mBypassDnd = b; }
    public int getLockscreenVisibility() { return mLockscreen; } public void setLockscreenVisibility(int v) { mLockscreen = v; }
    public boolean canBubble() { return mBubbles; } public void setAllowBubbles(boolean b) { mBubbles = b; }
    public void setConversationId(String parent, String conv) { mConvParent = parent; mConvId = conv; }
    public String getParentChannelId() { return mConvParent; } public String getConversationId() { return mConvId; }
    public boolean isImportantConversation() { return false; } public boolean isConversation() { return mConvId != null; }
    public boolean hasUserSetImportance() { return false; } public boolean hasUserSetSound() { return false; } public boolean isBlockable() { return false; } public boolean isDemoted() { return false; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    @Override public boolean equals(Object o) { return o instanceof NotificationChannel && java.util.Objects.equals(mId, ((NotificationChannel) o).mId) && java.util.Objects.equals(String.valueOf(mName), String.valueOf(((NotificationChannel) o).mName)) && mImportance == ((NotificationChannel) o).mImportance; }
    @Override public int hashCode() { return java.util.Objects.hashCode(mId); }
    @Override public String toString() { return "NotificationChannel{mId='" + mId + "', mName=" + mName + ", mImportance=" + mImportance + "}"; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public void setBlockable(boolean p0) { jet.runtime.ShimStubs.hit("android.app.NotificationChannel.setBlockable(Z)V"); }
    // ---- END generated SDK surface ----
}
