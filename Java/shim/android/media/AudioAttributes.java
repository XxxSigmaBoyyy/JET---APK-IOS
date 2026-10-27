package android.media;
public class AudioAttributes {
    public static final int CONTENT_TYPE_UNKNOWN = 0, CONTENT_TYPE_SPEECH = 1, CONTENT_TYPE_MUSIC = 2, CONTENT_TYPE_MOVIE = 3, CONTENT_TYPE_SONIFICATION = 4;
    public static final int USAGE_UNKNOWN = 0, USAGE_MEDIA = 1, USAGE_VOICE_COMMUNICATION = 2, USAGE_ALARM = 4, USAGE_NOTIFICATION = 5, USAGE_NOTIFICATION_RINGTONE = 6;
    private final int usage, contentType, flags;
    AudioAttributes(int usage, int contentType, int flags) { this.usage = usage; this.contentType = contentType; this.flags = flags; }
    public int getUsage() { return usage; } public int getContentType() { return contentType; } public int getFlags() { return flags; }
    public static class Builder {
        private int usage = USAGE_UNKNOWN, contentType = CONTENT_TYPE_UNKNOWN, flags;
        public Builder setUsage(int u) { usage = u; return this; }
        public Builder setContentType(int c) { contentType = c; return this; }
        public Builder setFlags(int f) { flags = f; return this; }
        public AudioAttributes build() { return new AudioAttributes(usage, contentType, flags); }
    }
}
