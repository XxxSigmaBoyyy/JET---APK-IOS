package android.media;
import android.content.Context;
import android.net.Uri;
/** android.media.RingtoneManager: AOSP default-sound URIs (Settings.System content URIs); no ringtone catalogue on the host. */
public class RingtoneManager {
    public static final int TYPE_RINGTONE = 1, TYPE_NOTIFICATION = 2, TYPE_ALARM = 4, TYPE_ALL = 7;
    public static final int ID_COLUMN_INDEX = 0, TITLE_COLUMN_INDEX = 1, URI_COLUMN_INDEX = 2;
    public static final String ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER";
    public static final String EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE", EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE",
        EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI", EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI",
        EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT", EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT",
        EXTRA_RINGTONE_DEFAULT_URI = "android.intent.extra.ringtone.DEFAULT_URI";
    private final Context ctx; private int type = TYPE_RINGTONE;
    public RingtoneManager(Context c) { ctx = c; }
    public RingtoneManager(android.app.Activity a) { ctx = a; }
    public void setType(int t) { type = t; }
    public int inferStreamType() { return type == TYPE_ALARM ? AudioManager.STREAM_ALARM : type == TYPE_NOTIFICATION ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_RING; }
    public void setStopPreviousRingtone(boolean b) {} public boolean getStopPreviousRingtone() { return true; } public void stopPreviousRingtone() {}
    public boolean getIncludeDrm() { return false; } public void setIncludeDrm(boolean b) {}
    public android.database.Cursor getCursor() { return new android.database.MatrixCursor(new String[] { "_id", "title", "uri" }); }
    public Ringtone getRingtone(int pos) { return null; }
    public Uri getRingtoneUri(int pos) { return null; }
    public int getRingtonePosition(Uri u) { return -1; }
    public static Uri getDefaultUri(int type) {
        String k = (type & TYPE_RINGTONE) != 0 ? "ringtone" : (type & TYPE_NOTIFICATION) != 0 ? "notification_sound" : (type & TYPE_ALARM) != 0 ? "alarm_alert" : null;
        return k == null ? null : Uri.parse("content://settings/system/" + k);
    }
    public static int getDefaultType(Uri u) { if (u == null) return -1; String s = u.toString(); return s.endsWith("/ringtone") ? TYPE_RINGTONE : s.endsWith("/notification_sound") ? TYPE_NOTIFICATION : s.endsWith("/alarm_alert") ? TYPE_ALARM : -1; }
    public static boolean isDefault(Uri u) { return getDefaultType(u) != -1; }
    public static Uri getActualDefaultRingtoneUri(Context c, int type) { return getDefaultUri(type); }
    public static void setActualDefaultRingtoneUri(Context c, int type, Uri u) {}
    public static Uri getValidRingtoneUri(Context c) { return getDefaultUri(TYPE_RINGTONE); }
    public static Ringtone getRingtone(Context c, Uri u) { return new Ringtone(c, u); }
}
