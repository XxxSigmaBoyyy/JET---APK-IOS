package android.app;
import java.util.*;
/** PendingIntent token. Identity follows AOSP: same kind + requestCode + Intent.filterEquals() returns the same token
 *  (FLAG_NO_CREATE returns null when absent, FLAG_CANCEL_CURRENT replaces, FLAG_UPDATE_CURRENT updates extras). send() is recorded, not delivered. */
public final class PendingIntent implements android.os.Parcelable {
    public static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT = 16777216;
    public static final int FLAG_CANCEL_CURRENT = 268435456;
    public static final int FLAG_IMMUTABLE = 67108864;
    public static final int FLAG_MUTABLE = 33554432;
    public static final int FLAG_NO_CREATE = 536870912;
    public static final int FLAG_ONE_SHOT = 1073741824;
    public static final int FLAG_UPDATE_CURRENT = 134217728;
    public static final android.os.Parcelable.Creator<PendingIntent> CREATOR = null;
    private static final List<PendingIntent> ALL = new ArrayList<>();
    private final int mKind, mCode, mFlags; private android.content.Intent mIntent; private boolean mCancelled; private final String mPkg;
    private PendingIntent(int kind, int code, android.content.Intent i, int flags, String pkg) { mKind = kind; mCode = code; mIntent = i; mFlags = flags; mPkg = pkg; }
    public static class CanceledException extends android.util.AndroidException { public CanceledException() {} public CanceledException(String s) { super(s); } public CanceledException(Exception e) { super(e); } }
    public interface OnFinished { void onSendFinished(PendingIntent pi, android.content.Intent intent, int resultCode, String resultData, android.os.Bundle resultExtras); }
    private static PendingIntent obtain(int kind, android.content.Context c, int code, android.content.Intent i, int flags) {
        String pkg = c == null ? null : c.getPackageName();
        synchronized (ALL) {
            for (Iterator<PendingIntent> it = ALL.iterator(); it.hasNext();) {
                PendingIntent p = it.next();
                if (p.mKind == kind && p.mCode == code && Objects.equals(p.mPkg, pkg) && (p.mIntent == null ? i == null : i != null && p.mIntent.filterEquals(i))) {
                    if ((flags & FLAG_CANCEL_CURRENT) != 0) { p.mCancelled = true; it.remove(); break; }
                    if ((flags & FLAG_UPDATE_CURRENT) != 0 && i != null) p.mIntent = new android.content.Intent(i);
                    return p;
                }
            }
            if ((flags & FLAG_NO_CREATE) != 0) return null;
            PendingIntent p = new PendingIntent(kind, code, i == null ? null : new android.content.Intent(i), flags, pkg);
            if ((flags & FLAG_ONE_SHOT) == 0) ALL.add(p);
            return p;
        }
    }
    public static PendingIntent getActivity(android.content.Context c, int code, android.content.Intent i, int flags) { return obtain(1, c, code, i, flags); }
    public static PendingIntent getActivity(android.content.Context c, int code, android.content.Intent i, int flags, android.os.Bundle o) { return obtain(1, c, code, i, flags); }
    public static PendingIntent getActivities(android.content.Context c, int code, android.content.Intent[] i, int flags) { return obtain(1, c, code, i.length == 0 ? null : i[i.length - 1], flags); }
    public static PendingIntent getActivities(android.content.Context c, int code, android.content.Intent[] i, int flags, android.os.Bundle o) { return getActivities(c, code, i, flags); }
    public static PendingIntent getBroadcast(android.content.Context c, int code, android.content.Intent i, int flags) { return obtain(2, c, code, i, flags); }
    public static PendingIntent getService(android.content.Context c, int code, android.content.Intent i, int flags) { return obtain(3, c, code, i, flags); }
    public static PendingIntent getForegroundService(android.content.Context c, int code, android.content.Intent i, int flags) { return obtain(4, c, code, i, flags); }
    public void cancel() { mCancelled = true; synchronized (ALL) { ALL.remove(this); } }
    public void send() throws CanceledException { send(null, 0, null, null, null, null, null); }
    public void send(int code) throws CanceledException { send(null, code, null, null, null, null, null); }
    public void send(android.content.Context c, int code, android.content.Intent i) throws CanceledException { send(c, code, i, null, null, null, null); }
    public void send(int code, OnFinished f, android.os.Handler h) throws CanceledException { send(null, code, null, f, h, null, null); }
    public void send(android.content.Context c, int code, android.content.Intent i, OnFinished f, android.os.Handler h) throws CanceledException { send(c, code, i, f, h, null, null); }
    public void send(android.content.Context c, int code, android.content.Intent i, OnFinished f, android.os.Handler h, String perm) throws CanceledException { send(c, code, i, f, h, perm, null); }
    public void send(android.content.Context c, int code, android.content.Intent i, OnFinished f, android.os.Handler h, String perm, android.os.Bundle o) throws CanceledException {
        if (mCancelled) throw new CanceledException();
        System.err.println("[jet-pendingintent] send kind=" + mKind + " " + mIntent + " (recorded, not delivered)");
        if ((mFlags & FLAG_ONE_SHOT) != 0) mCancelled = true;
    }
    public String getCreatorPackage() { return mPkg; } public String getTargetPackage() { return mPkg; }
    public int getCreatorUid() { return android.os.Process.myUid(); } public android.os.UserHandle getCreatorUserHandle() { return android.os.Process.myUserHandle(); }
    public boolean isImmutable() { return (mFlags & FLAG_IMMUTABLE) != 0; }
    public boolean isActivity() { return mKind == 1; } public boolean isBroadcast() { return mKind == 2; } public boolean isService() { return mKind == 3; } public boolean isForegroundService() { return mKind == 4; }
    public android.content.IntentSender getIntentSender() { return null; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    public static void writePendingIntentOrNullToParcel(PendingIntent p, android.os.Parcel out) {}
    public static PendingIntent readPendingIntentOrNullFromParcel(android.os.Parcel in) { return null; }
    @Override public String toString() { return "PendingIntent{" + Integer.toHexString(System.identityHashCode(this)) + ": " + mIntent + "}"; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public void send(android.os.Bundle p0) { jet.runtime.ShimStubs.hit("android.app.PendingIntent.send(Landroid/os/Bundle;)V"); }
    // ---- END generated SDK surface ----
}
