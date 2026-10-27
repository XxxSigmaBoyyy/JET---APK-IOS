package android.app;
import java.util.*;
/** Headless NotificationManager: channels/groups and posted notifications are kept in-process (nothing is shown). */
public class NotificationManager {
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final String ACTION_APP_BLOCK_STATE_CHANGED = "android.app.action.APP_BLOCK_STATE_CHANGED";
    public static final String ACTION_AUTOMATIC_ZEN_RULE = "android.app.action.AUTOMATIC_ZEN_RULE";
    public static final String ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED = "android.app.action.AUTOMATIC_ZEN_RULE_STATUS_CHANGED";
    public static final String ACTION_INTERRUPTION_FILTER_CHANGED = "android.app.action.INTERRUPTION_FILTER_CHANGED";
    public static final String ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED = "android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED = "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_CHANGED = "android.app.action.NOTIFICATION_POLICY_CHANGED";
    public static final int AUTOMATIC_RULE_STATUS_DISABLED = 2;
    public static final int AUTOMATIC_RULE_STATUS_ENABLED = 1;
    public static final int AUTOMATIC_RULE_STATUS_REMOVED = 3;
    public static final int AUTOMATIC_RULE_STATUS_UNKNOWN = -1;
    public static final int BUBBLE_PREFERENCE_ALL = 1;
    public static final int BUBBLE_PREFERENCE_NONE = 0;
    public static final int BUBBLE_PREFERENCE_SELECTED = 2;
    public static final String EXTRA_AUTOMATIC_RULE_ID = "android.app.extra.AUTOMATIC_RULE_ID";
    public static final String EXTRA_AUTOMATIC_ZEN_RULE_ID = "android.app.extra.AUTOMATIC_ZEN_RULE_ID";
    public static final String EXTRA_AUTOMATIC_ZEN_RULE_STATUS = "android.app.extra.AUTOMATIC_ZEN_RULE_STATUS";
    public static final String EXTRA_BLOCKED_STATE = "android.app.extra.BLOCKED_STATE";
    public static final String EXTRA_NOTIFICATION_CHANNEL_GROUP_ID = "android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID";
    public static final String EXTRA_NOTIFICATION_CHANNEL_ID = "android.app.extra.NOTIFICATION_CHANNEL_ID";
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_MAX = 5;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
    public static final String META_DATA_AUTOMATIC_RULE_TYPE = "android.service.zen.automatic.ruleType";
    public static final String META_DATA_RULE_INSTANCE_LIMIT = "android.service.zen.automatic.ruleInstanceLimit";
    private static final Map<String, NotificationChannel> CHANNELS = new LinkedHashMap<>();
    private static final Map<String, NotificationChannelGroup> GROUPS = new LinkedHashMap<>();
    private static final Map<String, Notification> ACTIVE = new LinkedHashMap<>();
    NotificationManager() {}
    public static NotificationManager from(android.content.Context c) { return (NotificationManager) c.getSystemService("notification"); }
    private static String key(String tag, int id) { return tag + "#" + id; }
    public void notify(int id, Notification n) { notify(null, id, n); }
    public void notify(String tag, int id, Notification n) { synchronized (ACTIVE) { ACTIVE.put(key(tag, id), n); } System.err.println("[jet-notify] post " + key(tag, id) + " channel=" + (n == null ? null : n.channelId)); }
    public void notifyAsPackage(String pkg, String tag, int id, Notification n) { notify(tag, id, n); }
    public void cancel(int id) { cancel(null, id); }
    public void cancel(String tag, int id) { synchronized (ACTIVE) { ACTIVE.remove(key(tag, id)); } }
    public void cancelAll() { synchronized (ACTIVE) { ACTIVE.clear(); } }
    public void cancelAsPackage(String pkg, String tag, int id) { cancel(tag, id); }
    public android.service.notification.StatusBarNotification[] getActiveNotifications() { synchronized (ACTIVE) { List<android.service.notification.StatusBarNotification> r = new ArrayList<>(); String pkg = android.app.Application.jetCurrent == null ? "" : android.app.Application.jetCurrent.getPackageName(); for (Map.Entry<String, Notification> e : ACTIVE.entrySet()) { int h = e.getKey().lastIndexOf('#'); String tag = e.getKey().substring(0, h); r.add(new android.service.notification.StatusBarNotification(pkg, pkg, Integer.parseInt(e.getKey().substring(h + 1)), "null".equals(tag) ? null : tag, 0, 0, e.getValue(), null, null, e.getValue().when)); } return r.toArray(new android.service.notification.StatusBarNotification[0]); } }
    public void createNotificationChannel(NotificationChannel c) { synchronized (CHANNELS) { NotificationChannel old = CHANNELS.get(c.getId()); if (old == null) CHANNELS.put(c.getId(), c); else { old.setName(c.getName()); old.setDescription(c.getDescription()); if (c.getImportance() < old.getImportance()) old.setImportance(c.getImportance()); } } }
    public void createNotificationChannels(List<NotificationChannel> l) { for (NotificationChannel c : l) createNotificationChannel(c); }
    public NotificationChannel getNotificationChannel(String id) { synchronized (CHANNELS) { return CHANNELS.get(id); } }
    public NotificationChannel getNotificationChannel(String parent, String conv) { synchronized (CHANNELS) { for (NotificationChannel c : CHANNELS.values()) if (parent.equals(c.getParentChannelId()) && conv.equals(c.getConversationId())) return c; return CHANNELS.get(parent); } }
    public List<NotificationChannel> getNotificationChannels() { synchronized (CHANNELS) { return new ArrayList<>(CHANNELS.values()); } }
    public void deleteNotificationChannel(String id) { synchronized (CHANNELS) { CHANNELS.remove(id); } }
    public void createNotificationChannelGroup(NotificationChannelGroup g) { synchronized (GROUPS) { GROUPS.put(g.getId(), g); } }
    public void createNotificationChannelGroups(List<NotificationChannelGroup> l) { for (NotificationChannelGroup g : l) createNotificationChannelGroup(g); }
    public NotificationChannelGroup getNotificationChannelGroup(String id) { synchronized (GROUPS) { NotificationChannelGroup g = GROUPS.get(id); if (g == null) return null; NotificationChannelGroup r = g.clone(); for (NotificationChannel c : getNotificationChannels()) if (id.equals(c.getGroup())) r.jetChannels.add(c); return r; } }
    public List<NotificationChannelGroup> getNotificationChannelGroups() { List<NotificationChannelGroup> r = new ArrayList<>(); synchronized (GROUPS) { for (String id : GROUPS.keySet()) r.add(getNotificationChannelGroup(id)); } return r; }
    public void deleteNotificationChannelGroup(String id) { synchronized (GROUPS) { GROUPS.remove(id); } synchronized (CHANNELS) { CHANNELS.values().removeIf(c -> id.equals(c.getGroup())); } }
    public boolean areNotificationsEnabled() { return true; }
    public boolean areNotificationsPaused() { return false; }
    public boolean areBubblesAllowed() { return false; } public boolean areBubblesEnabled() { return false; }
    public int getBubblePreference() { return 0; }
    public int getImportance() { return IMPORTANCE_DEFAULT; }
    public int getCurrentInterruptionFilter() { return INTERRUPTION_FILTER_ALL; }
    public boolean isNotificationPolicyAccessGranted() { return false; }
    public boolean canNotifyAsPackage(String p) { return false; }
    public boolean canUseFullScreenIntent() { return true; }
    public boolean canPostPromotedNotifications() { return false; }
    public void setNotificationDelegate(String d) {} public String getNotificationDelegate() { return null; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public java.util.Map getAutomaticZenRules() { jet.runtime.ShimStubs.hit("android.app.NotificationManager.getAutomaticZenRules()Ljava/util/Map;"); return null; }
    public boolean removeAutomaticZenRule(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.app.NotificationManager.removeAutomaticZenRule(Ljava/lang/String;)Z"); return false; }
    public boolean isNotificationListenerAccessGranted(android.content.ComponentName p0) { jet.runtime.ShimStubs.hit("android.app.NotificationManager.isNotificationListenerAccessGranted(Landroid/content/ComponentName;)Z"); return false; }
    public boolean shouldHideSilentStatusBarIcons() { jet.runtime.ShimStubs.hit("android.app.NotificationManager.shouldHideSilentStatusBarIcons()Z"); return false; }
    public void setInterruptionFilter(int p0) { jet.runtime.ShimStubs.hit("android.app.NotificationManager.setInterruptionFilter(I)V"); }
    public boolean matchesCallFilter(android.net.Uri p0) { jet.runtime.ShimStubs.hit("android.app.NotificationManager.matchesCallFilter(Landroid/net/Uri;)Z"); return false; }
    // ---- END generated SDK surface ----
}
