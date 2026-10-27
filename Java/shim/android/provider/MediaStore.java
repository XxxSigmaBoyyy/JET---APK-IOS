package android.provider;
import android.net.Uri;
/** MediaStore contract constants (AOSP values). Queries go through ContentResolver, which on the host has no media provider (empty results). */
public final class MediaStore {
    public static final String AUTHORITY = "media";
    public static final Uri AUTHORITY_URI = Uri.parse("content://media");
    public static final String VOLUME_EXTERNAL = "external", VOLUME_EXTERNAL_PRIMARY = "external_primary", VOLUME_INTERNAL = "internal";
    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE", ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE", EXTRA_OUTPUT = "output";
    public static final String MEDIA_SCANNER_VOLUME = "volume", UNKNOWN_STRING = "<unknown>";
    private MediaStore() {}
    public static Uri getMediaScannerUri() { return Uri.parse("content://media/none/media_scanner"); }
    public static String getVersion(android.content.Context c) { return "jet"; }
    public static java.util.Set<String> getExternalVolumeNames(android.content.Context c) { return java.util.Collections.singleton(VOLUME_EXTERNAL_PRIMARY); }
    public interface MediaColumns extends BaseColumns {
        String DATA = "_data", SIZE = "_size", DISPLAY_NAME = "_display_name", TITLE = "title", DATE_ADDED = "date_added", DATE_MODIFIED = "date_modified", DATE_TAKEN = "datetaken",
            MIME_TYPE = "mime_type", WIDTH = "width", HEIGHT = "height", ORIENTATION = "orientation", DURATION = "duration", BUCKET_ID = "bucket_id", BUCKET_DISPLAY_NAME = "bucket_display_name",
            RELATIVE_PATH = "relative_path", VOLUME_NAME = "volume_name", IS_PENDING = "is_pending", IS_TRASHED = "is_trashed", IS_FAVORITE = "is_favorite", OWNER_PACKAGE_NAME = "owner_package_name";
    }
    private static Uri content(String volume, String path) { return Uri.parse("content://media/" + volume + "/" + path); }
    public static final class Files { public interface FileColumns extends MediaColumns { String MEDIA_TYPE = "media_type", PARENT = "parent"; int MEDIA_TYPE_NONE = 0, MEDIA_TYPE_IMAGE = 1, MEDIA_TYPE_AUDIO = 2, MEDIA_TYPE_VIDEO = 3, MEDIA_TYPE_PLAYLIST = 4, MEDIA_TYPE_SUBTITLE = 5, MEDIA_TYPE_DOCUMENT = 6; }
        public static Uri getContentUri(String volume) { return content(volume, "file"); } public static Uri getContentUri(String volume, long id) { return Uri.withAppendedPath(getContentUri(volume), String.valueOf(id)); } }
    public static final class Images {
        public interface ImageColumns extends MediaColumns { String DESCRIPTION = "description", PICASA_ID = "picasa_id", IS_PRIVATE = "isprivate", LATITUDE = "latitude", LONGITUDE = "longitude", MINI_THUMB_MAGIC = "mini_thumb_magic"; }
        public static final class Media implements ImageColumns {
            public static final Uri INTERNAL_CONTENT_URI = content(VOLUME_INTERNAL, "images/media"), EXTERNAL_CONTENT_URI = content(VOLUME_EXTERNAL, "images/media");
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image", DEFAULT_SORT_ORDER = "bucket_display_name";
            public static Uri getContentUri(String volume) { return content(volume, "images/media"); }
        }
        public static final class Thumbnails implements BaseColumns { public static final Uri EXTERNAL_CONTENT_URI = content(VOLUME_EXTERNAL, "images/thumbnails"); public static final String DATA = "_data", IMAGE_ID = "image_id", KIND = "kind"; public static final int MINI_KIND = 1, FULL_SCREEN_KIND = 2, MICRO_KIND = 3; }
    }
    public static final class Video {
        public interface VideoColumns extends MediaColumns { String RESOLUTION = "resolution", DESCRIPTION = "description"; }
        public static final class Media implements VideoColumns {
            public static final Uri INTERNAL_CONTENT_URI = content(VOLUME_INTERNAL, "video/media"), EXTERNAL_CONTENT_URI = content(VOLUME_EXTERNAL, "video/media");
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
            public static Uri getContentUri(String volume) { return content(volume, "video/media"); }
        }
    }
    public static final class Audio {
        public interface AudioColumns extends MediaColumns { String ARTIST = "artist", ALBUM = "album", ALBUM_ID = "album_id", ARTIST_ID = "artist_id", IS_MUSIC = "is_music", TRACK = "track", YEAR = "year"; }
        public static final class Media implements AudioColumns {
            public static final Uri INTERNAL_CONTENT_URI = content(VOLUME_INTERNAL, "audio/media"), EXTERNAL_CONTENT_URI = content(VOLUME_EXTERNAL, "audio/media");
            public static Uri getContentUri(String volume) { return content(volume, "audio/media"); }
        }
    }
    // API 31+: media-management special access is not granted to a sandboxed app (user must enable it in Settings)
    public static boolean canManageMedia(android.content.Context c) { return false; }
    public static boolean isCurrentCloudMediaProviderAuthority(android.content.ContentResolver r, String a) { return false; }
    public static int getPickImagesMaxLimit() { return 100; }
    public static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";
    public static final String EXTRA_PICK_IMAGES_MAX = "android.provider.extra.PICK_IMAGES_MAX";
}
