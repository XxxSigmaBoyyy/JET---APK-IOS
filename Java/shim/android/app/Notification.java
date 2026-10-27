package android.app;
/** Notification with AOSP public fields (Telegram LoginActivity reflects several). */
public class Notification implements java.io.Serializable {
    public static final int DEFAULT_ALL = -1, DEFAULT_SOUND = 1, DEFAULT_VIBRATE = 2, DEFAULT_LIGHTS = 4;
    public static final int FLAG_SHOW_LIGHTS = 1, FLAG_ONGOING_EVENT = 2, FLAG_INSISTENT = 4, FLAG_ONLY_ALERT_ONCE = 8, FLAG_AUTO_CANCEL = 16, FLAG_NO_CLEAR = 32, FLAG_FOREGROUND_SERVICE = 64;
    public static final int STREAM_DEFAULT = -1;
    public static final android.media.AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build();
    public long when;
    public long creationTime;
    public int icon;
    public int iconLevel;
    public int number;
    public CharSequence tickerText;
    public int defaults;
    public int flags;
    public int color;
    public int visibility;
    public int priority;
    public int audioStreamType = STREAM_DEFAULT;
    public android.widget.RemoteViews contentView;
    public android.widget.RemoteViews bigContentView;
    public android.widget.RemoteViews headsUpContentView;
    public android.widget.RemoteViews tickerView;
    public int ledARGB, ledOnMS, ledOffMS;
    public android.net.Uri sound;
    public long[] vibrate;
    public String category;
    public String group;
    public String sortKey;
    public String channelId;
    public String channelGroupId;
    public PendingIntent contentIntent, deleteIntent, fullScreenIntent;
    public android.os.Bundle extras = new android.os.Bundle();
    public android.graphics.Bitmap largeIcon;
    public Notification publicVersion;
    public android.media.AudioAttributes audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
    public String getChannelId() { return channelId; } public String getGroup() { return group; } public String getSortKey() { return sortKey; }
    public long getTimeoutAfter() { return jetTimeout; } long jetTimeout; String jetShortcut; int jetBadge, jetGroupAlert;
    public String getShortcutId() { return jetShortcut; } public int getBadgeIconType() { return jetBadge; } public int getGroupAlertBehavior() { return jetGroupAlert; }
    public Action[] actions;
    BubbleMetadata jetBubble; android.content.LocusId jetLocus;
    public BubbleMetadata getBubbleMetadata() { return jetBubble; }
    public android.content.LocusId getLocusId() { return jetLocus; }
    public android.graphics.drawable.Icon getSmallIcon() { return null; }
    public android.graphics.drawable.Icon getLargeIcon() { return null; }
    public CharSequence getSettingsText() { return extras.getCharSequence("android.settingsText"); }
    public java.util.List<Action> getContextualActions() { return java.util.Collections.emptyList(); }
    public boolean getAllowSystemGeneratedContextualActions() { return false; }
    public android.util.Pair<android.app.RemoteInput, Action> findRemoteInputActionPair(boolean requiresFreeform) { return null; }
    public boolean hasImage() { return largeIcon != null || extras.containsKey("android.picture"); }
    @Override public Notification clone() { Notification c = new Notification(this); c.extras = new android.os.Bundle(extras); c.contentIntent = contentIntent; c.deleteIntent = deleteIntent; c.fullScreenIntent = fullScreenIntent; c.category = category; c.group = group; c.sortKey = sortKey; c.color = color; c.visibility = visibility; c.priority = priority; c.actions = actions == null ? null : actions.clone(); c.jetBubble = jetBubble; c.jetLocus = jetLocus; c.largeIcon = largeIcon; return c; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int flags) { }
    @Override public String toString() { return "Notification(channel=" + channelId + " pri=" + priority + " contentView=null vibrate=" + (vibrate == null ? "null" : "custom") + " sound=" + (sound == null ? "null" : "custom") + " defaults=0x" + Integer.toHexString(defaults) + " flags=0x" + Integer.toHexString(flags) + " color=0x" + String.format("%08x", color) + " vis=" + visibility + ")"; }
    public Notification() { when = creationTime = System.currentTimeMillis(); }
    public Notification(int icon, CharSequence tickerText, long when) { this(); this.icon = icon; this.tickerText = tickerText; this.when = when; }
    public Notification(Notification n) { this(); if (n != null) { when = n.when; icon = n.icon; number = n.number; tickerText = n.tickerText; defaults = n.defaults; flags = n.flags; audioStreamType = n.audioStreamType; sound = n.sound; vibrate = n.vibrate; channelId = n.channelId; } }
    public interface Extender { Builder extend(Builder builder); }
    /** AOSP Notification.Style: binds to a Builder; subclasses (BigTextStyle, MessagingStyle...) add extras. */
    public abstract static class Style {
        protected Builder mBuilder;
        public Style() {}
        public void setBuilder(Builder b) { if (mBuilder != b) { mBuilder = b; if (b != null) b.setStyle(this); } }
        public Notification build() { return mBuilder == null ? null : mBuilder.build(); }
    }
    public static class Action implements android.os.Parcelable {
        public static final int SEMANTIC_ACTION_NONE = 0, SEMANTIC_ACTION_REPLY = 1, SEMANTIC_ACTION_MARK_AS_READ = 2, SEMANTIC_ACTION_MARK_AS_UNREAD = 3, SEMANTIC_ACTION_DELETE = 4, SEMANTIC_ACTION_ARCHIVE = 5, SEMANTIC_ACTION_MUTE = 6, SEMANTIC_ACTION_UNMUTE = 7, SEMANTIC_ACTION_THUMBS_UP = 8, SEMANTIC_ACTION_THUMBS_DOWN = 9, SEMANTIC_ACTION_CALL = 10;
        public int icon; public CharSequence title; public PendingIntent actionIntent;
        android.graphics.drawable.Icon jetIcon; android.os.Bundle jetExtras = new android.os.Bundle(); boolean jetGenReplies = true, jetContextual, jetAuth; int jetSemantic;
        final java.util.ArrayList<Object> jetRemoteInputs = new java.util.ArrayList<>();
        public Action(int icon, CharSequence title, PendingIntent intent) { this.icon = icon; this.title = title; this.actionIntent = intent; }
        public android.graphics.drawable.Icon getIcon() { return jetIcon; }
        public android.os.Bundle getExtras() { return jetExtras; }
        public boolean getAllowGeneratedReplies() { return jetGenReplies; }
        public android.app.RemoteInput[] getRemoteInputs() { return null; }
        public android.app.RemoteInput[] getDataOnlyRemoteInputs() { return null; }
        public int getSemanticAction() { return jetSemantic; }
        public boolean isContextual() { return jetContextual; }
        public boolean isAuthenticationRequired() { return jetAuth; }
        @Override public Action clone() { Action a = new Action(icon, title, actionIntent); a.jetIcon = jetIcon; a.jetExtras = new android.os.Bundle(jetExtras); a.jetGenReplies = jetGenReplies; a.jetContextual = jetContextual; a.jetAuth = jetAuth; a.jetSemantic = jetSemantic; a.jetRemoteInputs.addAll(jetRemoteInputs); return a; }
        public int describeContents() { return 0; }
        public void writeToParcel(android.os.Parcel out, int flags) { out.writeInt(icon); out.writeCharSequence(title); out.writeBundle(jetExtras); out.writeInt(jetSemantic); }
        public interface Extender { Action.Builder extend(Action.Builder builder); }
        public static final class Builder {
            private final Action a;
            public Builder(int icon, CharSequence title, PendingIntent intent) { a = new Action(icon, title, intent); }
            public Builder(android.graphics.drawable.Icon icon, CharSequence title, PendingIntent intent) { a = new Action(0, title, intent); a.jetIcon = icon; }
            public Builder(Action src) { a = src.clone(); }
            public Builder addExtras(android.os.Bundle b) { if (b != null) a.jetExtras.putAll(b); return this; }
            public android.os.Bundle getExtras() { return a.jetExtras; }
            public Builder addRemoteInput(android.app.RemoteInput r) { a.jetRemoteInputs.add(r); return this; }
            public Builder setAllowGeneratedReplies(boolean b) { a.jetGenReplies = b; return this; }
            public Builder setSemanticAction(int s) { a.jetSemantic = s; return this; }
            public Builder setContextual(boolean b) { a.jetContextual = b; return this; }
            public Builder setAuthenticationRequired(boolean b) { a.jetAuth = b; return this; }
            public Builder extend(Extender e) { Builder r = e == null ? null : e.extend(this); return r == null ? this : r; }
            public Action build() { return a.clone(); }
        }
    }
    public static final class BubbleMetadata implements android.os.Parcelable {
        String shortcutId; PendingIntent intent, deleteIntent; android.graphics.drawable.Icon icon; int height, heightRes; boolean autoExpand, suppressNotif, suppressable;
        public String getShortcutId() { return shortcutId; } public PendingIntent getIntent() { return intent; } public PendingIntent getDeleteIntent() { return deleteIntent; }
        public android.graphics.drawable.Icon getIcon() { return icon; } public int getDesiredHeight() { return height; } public int getDesiredHeightResId() { return heightRes; }
        public boolean getAutoExpandBubble() { return autoExpand; } public boolean isNotificationSuppressed() { return suppressNotif; } public boolean isBubbleSuppressable() { return suppressable; } public boolean isBubbleSuppressed() { return false; }
        public int describeContents() { return 0; }
        public void writeToParcel(android.os.Parcel out, int flags) { out.writeString(shortcutId); out.writeInt(height); out.writeInt(heightRes); out.writeInt(autoExpand ? 1 : 0); out.writeInt(suppressNotif ? 1 : 0); }
        public static final class Builder {
            private final BubbleMetadata m = new BubbleMetadata();
            @Deprecated public Builder() {}
            public Builder(String shortcutId) { if (shortcutId == null || shortcutId.isEmpty()) throw new NullPointerException("Bubble requires a non-null shortcut id"); m.shortcutId = shortcutId; }
            public Builder(PendingIntent intent, android.graphics.drawable.Icon icon) { if (intent == null) throw new NullPointerException("Bubble requires non-null pending intent"); if (icon == null) throw new NullPointerException("Bubbles require non-null icon"); m.intent = intent; m.icon = icon; }
            public Builder setIntent(PendingIntent p) { m.intent = p; return this; }
            public Builder setIcon(android.graphics.drawable.Icon i) { m.icon = i; return this; }
            public Builder setDesiredHeight(int h) { m.height = Math.max(h, 0); m.heightRes = 0; return this; }
            public Builder setDesiredHeightResId(int r) { m.heightRes = r; m.height = 0; return this; }
            public Builder setAutoExpandBubble(boolean b) { m.autoExpand = b; return this; }
            public Builder setSuppressNotification(boolean b) { m.suppressNotif = b; return this; }
            public Builder setSuppressableBubble(boolean b) { m.suppressable = b; return this; }
            public Builder setDeleteIntent(PendingIntent p) { m.deleteIntent = p; return this; }
            public BubbleMetadata build() { if (m.shortcutId == null && m.intent == null) throw new NullPointerException("Must supply pending intent or shortcut to bubble"); return m; }
        }
    }
    public static class Builder {
        private final Notification n = new Notification();
        public Builder(android.content.Context c) {}
        public Builder(android.content.Context c, String channelId) { n.channelId = channelId; }
        private Builder flag(int f, boolean on) { if (on) n.flags |= f; else n.flags &= ~f; return this; }
        private Builder ex(String k, Object v) { if (v instanceof CharSequence) n.extras.putCharSequence(k, (CharSequence) v); else if (v instanceof Boolean) n.extras.putBoolean(k, (Boolean) v); else if (v instanceof Integer) n.extras.putInt(k, (Integer) v); return this; }
        public Builder setShortcutId(String s) { n.jetShortcut = s; return this; }
        public Builder setBadgeIconType(int t) { n.jetBadge = t; return this; }
        public Builder setGroupAlertBehavior(int b) { n.jetGroupAlert = b; return this; }
        public Builder setChannelId(String id) { n.channelId = id; return this; }
        public Builder setTimeoutAfter(long ms) { n.jetTimeout = ms; return this; }
        public Builder setWhen(long w) { n.when = w; return this; }
        public Builder setShowWhen(boolean b) { return ex("android.showWhen", b); }
        public Builder setUsesChronometer(boolean b) { return ex("android.showChronometer", b); }
        public Builder setChronometerCountDown(boolean b) { return ex("android.chronometerCountDown", b); }
        public Builder setSmallIcon(int id) { n.icon = id; return this; }
        public Builder setSmallIcon(int id, int level) { n.icon = id; n.iconLevel = level; return this; }
        public Builder setSmallIcon(android.graphics.drawable.Icon i) { return this; }
        public Builder setContentTitle(CharSequence t) { return ex("android.title", t); }
        public Builder setContentText(CharSequence t) { return ex("android.text", t); }
        public Builder setSubText(CharSequence t) { return ex("android.subText", t); }
        public Builder setSettingsText(CharSequence t) { return ex("android.settingsText", t); }
        public Builder setRemoteInputHistory(CharSequence[] t) { return this; }
        public Builder setNumber(int x) { n.number = x; return this; }
        public Builder setContentInfo(CharSequence t) { return ex("android.infoText", t); }
        public Builder setProgress(int max, int progress, boolean ind) { n.extras.putInt("android.progress", progress); n.extras.putInt("android.progressMax", max); n.extras.putBoolean("android.progressIndeterminate", ind); return this; }
        public Builder setContent(android.widget.RemoteViews v) { n.contentView = v; return this; }
        public Builder setCustomContentView(android.widget.RemoteViews v) { n.contentView = v; return this; }
        public Builder setCustomBigContentView(android.widget.RemoteViews v) { n.bigContentView = v; return this; }
        public Builder setCustomHeadsUpContentView(android.widget.RemoteViews v) { n.headsUpContentView = v; return this; }
        public Builder setContentIntent(PendingIntent p) { n.contentIntent = p; return this; }
        public Builder setDeleteIntent(PendingIntent p) { n.deleteIntent = p; return this; }
        public Builder setFullScreenIntent(PendingIntent p, boolean hp) { n.fullScreenIntent = p; return flag(128, hp); }
        public Builder setTicker(CharSequence t) { n.tickerText = t; return this; }
        public Builder setTicker(CharSequence t, android.widget.RemoteViews rv) { n.tickerText = t; n.tickerView = rv; return this; }
        public Builder setLargeIcon(android.graphics.Bitmap b) { n.largeIcon = b; return this; }
        public Builder setLargeIcon(android.graphics.drawable.Icon i) { return this; }
        public Builder setSound(android.net.Uri u) { n.sound = u; n.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT; return this; }
        public Builder setSound(android.net.Uri u, int streamType) { n.sound = u; n.audioStreamType = streamType; return this; }
        public Builder setSound(android.net.Uri u, android.media.AudioAttributes a) { n.sound = u; n.audioAttributes = a; return this; }
        public Builder setVibrate(long[] pattern) { n.vibrate = pattern; return this; }
        public Builder setLights(int argb, int onMs, int offMs) { n.ledARGB = argb; n.ledOnMS = onMs; n.ledOffMS = offMs; return flag(FLAG_SHOW_LIGHTS, onMs != 0 && offMs != 0); }
        public Builder setOngoing(boolean o) { return flag(FLAG_ONGOING_EVENT, o); }
        public Builder setColorized(boolean b) { return ex("android.colorized", b); }
        public Builder setOnlyAlertOnce(boolean b) { return flag(FLAG_ONLY_ALERT_ONCE, b); }
        public Builder setForegroundServiceBehavior(int b) { return this; }
        public Builder setAutoCancel(boolean b) { return flag(FLAG_AUTO_CANCEL, b); }
        public Builder setLocalOnly(boolean b) { return flag(256, b); }
        public Builder setDefaults(int d) { n.defaults = d; return this; }
        public Builder setPriority(int p) { n.priority = p; return this; }
        public Builder setCategory(String c) { n.category = c; return this; }
        public Builder addPerson(String uri) { return this; }
        public Builder setGroup(String g) { n.group = g; return this; }
        public Builder setGroupSummary(boolean b) { return flag(512, b); }
        public Builder setSortKey(String k) { n.sortKey = k; return this; }
        public Builder addExtras(android.os.Bundle b) { if (b != null) n.extras.putAll(b); return this; }
        public Builder setExtras(android.os.Bundle b) { n.extras = b == null ? new android.os.Bundle() : new android.os.Bundle(b); return this; }
        public android.os.Bundle getExtras() { return n.extras; }
        public Builder addAction(int icon, CharSequence title, PendingIntent p) { jetActions.add(new Action(icon, title, p)); return this; }
        public Builder addAction(Action a) { if (a != null) jetActions.add(a); return this; }
        public Builder setActions(Action... as) { jetActions.clear(); if (as != null) for (Action a : as) if (a != null) jetActions.add(a); return this; }
        private final java.util.ArrayList<Action> jetActions = new java.util.ArrayList<>();
        private Style jetStyle;
        public Builder setStyle(Style s) { if (jetStyle != s) { jetStyle = s; if (s != null) s.setBuilder(this); } return this; }
        public Style getStyle() { return jetStyle; }
        public Builder extend(Extender e) { Builder r = e == null ? null : e.extend(this); return r == null ? this : r; }
        public Builder setBubbleMetadata(BubbleMetadata d) { n.jetBubble = d; return this; }
        public Builder setLocusId(android.content.LocusId id) { n.jetLocus = id; return this; }
        public Builder setVisibility(int v) { n.visibility = v; return this; }
        public Builder setPublicVersion(Notification p) { n.publicVersion = p; return this; }
        public Builder setFlag(int f, boolean on) { return flag(f, on); }
        public Builder setColor(int c) { n.color = c; return this; }
        public Builder setAllowSystemGeneratedContextualActions(boolean b) { return this; }
        public android.widget.RemoteViews createContentView() { return n.contentView; }
        public android.widget.RemoteViews createBigContentView() { return n.bigContentView; }
        public android.widget.RemoteViews createHeadsUpContentView() { return n.headsUpContentView; }
        public static Builder recoverBuilder(android.content.Context c, Notification src) { Builder b = new Builder(c, src.channelId); Notification d = b.n; d.when = src.when; d.icon = src.icon; d.flags = src.flags; d.extras = new android.os.Bundle(src.extras); d.contentIntent = src.contentIntent; d.sound = src.sound; d.vibrate = src.vibrate; return b; }
        @Deprecated public Notification getNotification() { return build(); }
        public Notification build() { n.actions = jetActions.isEmpty() ? null : jetActions.toArray(new Action[0]); return n; }
    }
}
