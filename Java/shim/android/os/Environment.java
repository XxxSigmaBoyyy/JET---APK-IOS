package android.os;
import java.io.File;
/** Host storage layout under -Djet.data.dir: <data>/sdcard is the emulated external storage (Linux-only path; iOS maps it to the app container). */
public class Environment {
    public static final String DIRECTORY_MUSIC = "Music";
    public static final String DIRECTORY_PODCASTS = "Podcasts";
    public static final String DIRECTORY_RINGTONES = "Ringtones";
    public static final String DIRECTORY_ALARMS = "Alarms";
    public static final String DIRECTORY_NOTIFICATIONS = "Notifications";
    public static final String DIRECTORY_PICTURES = "Pictures";
    public static final String DIRECTORY_MOVIES = "Movies";
    public static final String DIRECTORY_DOWNLOADS = "Download";
    public static final String DIRECTORY_DCIM = "DCIM";
    public static final String DIRECTORY_DOCUMENTS = "Documents";
    public static final String DIRECTORY_SCREENSHOTS = "Screenshots";
    public static final String DIRECTORY_AUDIOBOOKS = "Audiobooks";
    public static final String DIRECTORY_RECORDINGS = "Recordings";

    public static final java.lang.String MEDIA_BAD_REMOVAL = "bad_removal";
    public static final java.lang.String MEDIA_CHECKING = "checking";
    public static final java.lang.String MEDIA_EJECTING = "ejecting";
    public static final java.lang.String MEDIA_MOUNTED = "mounted";
    public static final java.lang.String MEDIA_MOUNTED_READ_ONLY = "mounted_ro";
    public static final java.lang.String MEDIA_NOFS = "nofs";
    public static final java.lang.String MEDIA_REMOVED = "removed";
    public static final java.lang.String MEDIA_SHARED = "shared";
    public static final java.lang.String MEDIA_UNKNOWN = "unknown";
    public static final java.lang.String MEDIA_UNMOUNTABLE = "unmountable";
    public static final java.lang.String MEDIA_UNMOUNTED = "unmounted";

    private static File base() { File b = new File(System.getProperty("jet.data.dir", System.getProperty("java.io.tmpdir") + "/jet-data")); b.mkdirs(); return b; }
    private static File d(File f) { f.mkdirs(); return f; }
    public static File getExternalStorageDirectory() { return d(new File(base(), "sdcard")); }
    public static File getExternalStoragePublicDirectory(String type) { return d(new File(getExternalStorageDirectory(), type == null ? "" : type)); }
    public static File getDataDirectory() { return d(new File(base(), "data")); }
    public static File getRootDirectory() { return d(new File(base(), "system")); }
    public static File getDownloadCacheDirectory() { return d(new File(base(), "cache")); }
    public static File getStorageDirectory() { return d(new File(base(), "storage")); }
    public static String getExternalStorageState() { return MEDIA_MOUNTED; }
    public static String getExternalStorageState(File path) { return MEDIA_MOUNTED; }
    @Deprecated public static String getStorageState(File path) { return MEDIA_MOUNTED; }
    public static boolean isExternalStorageRemovable() { return false; } public static boolean isExternalStorageRemovable(File p) { return false; }
    public static boolean isExternalStorageEmulated() { return true; } public static boolean isExternalStorageEmulated(File p) { return true; }
    public static boolean isExternalStorageLegacy() { return false; } public static boolean isExternalStorageLegacy(File p) { return false; }
    public static boolean isExternalStorageManager() { return false; } public static boolean isExternalStorageManager(File p) { return false; }
    public Environment() {}
}
