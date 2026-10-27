package android.content.pm;

import java.util.ArrayList;
import java.util.List;

/** In-memory ShortcutManager (dynamic + pinned lists; launcher integration is out of scope on the host). */
public class ShortcutManager {
    private final List<ShortcutInfo> mDynamic = new ArrayList<>(), mPinned = new ArrayList<>();
    public static final int FLAG_MATCH_MANIFEST = 1, FLAG_MATCH_DYNAMIC = 2, FLAG_MATCH_PINNED = 4, FLAG_MATCH_CACHED = 8;
    public ShortcutManager() {}
    private static void put(List<ShortcutInfo> l, ShortcutInfo s) { l.removeIf(x -> x.getId().equals(s.getId())); l.add(s); }
    public synchronized boolean setDynamicShortcuts(List<ShortcutInfo> l) { mDynamic.clear(); for (ShortcutInfo s : l) put(mDynamic, s); return true; }
    public synchronized boolean addDynamicShortcuts(List<ShortcutInfo> l) { for (ShortcutInfo s : l) put(mDynamic, s); return true; }
    public synchronized void pushDynamicShortcut(ShortcutInfo s) { put(mDynamic, s); }
    public synchronized boolean updateShortcuts(List<ShortcutInfo> l) { for (ShortcutInfo s : l) { for (int i = 0; i < mDynamic.size(); i++) if (mDynamic.get(i).getId().equals(s.getId())) mDynamic.set(i, s); } return true; }
    public synchronized void removeDynamicShortcuts(List<String> ids) { mDynamic.removeIf(s -> ids.contains(s.getId())); }
    public synchronized void removeAllDynamicShortcuts() { mDynamic.clear(); }
    public synchronized void removeLongLivedShortcuts(List<String> ids) { removeDynamicShortcuts(ids); }
    public synchronized List<ShortcutInfo> getDynamicShortcuts() { return new ArrayList<>(mDynamic); }
    public synchronized List<ShortcutInfo> getPinnedShortcuts() { return new ArrayList<>(mPinned); }
    public List<ShortcutInfo> getManifestShortcuts() { return new ArrayList<>(); }
    public synchronized List<ShortcutInfo> getShortcuts(int flags) { List<ShortcutInfo> r = new ArrayList<>(); if ((flags & FLAG_MATCH_DYNAMIC) != 0) r.addAll(mDynamic); if ((flags & FLAG_MATCH_PINNED) != 0) r.addAll(mPinned); return r; }
    public void disableShortcuts(List<String> ids) {} public void disableShortcuts(List<String> ids, CharSequence m) {} public void enableShortcuts(List<String> ids) {}
    public int getMaxShortcutCountPerActivity() { return 15; }
    public boolean isRateLimitingActive() { return false; }
    public int getIconMaxWidth() { return 192; } public int getIconMaxHeight() { return 192; }
    public void reportShortcutUsed(String id) {}
    public boolean isRequestPinShortcutSupported() { return false; }
    public boolean requestPinShortcut(ShortcutInfo s, android.content.IntentSender cb) { return false; }
    public android.content.Intent createShortcutResultIntent(ShortcutInfo s) { return new android.content.Intent(); }
}
