package android.content;
public class IntentFilter {
    public static final int SYSTEM_HIGH_PRIORITY = 1000, SYSTEM_LOW_PRIORITY = -1000;
    private final java.util.ArrayList<String> actions = new java.util.ArrayList<>(), cats = new java.util.ArrayList<>(), schemes = new java.util.ArrayList<>(); private int prio;
    public IntentFilter() {} public IntentFilter(String a) { addAction(a); } public IntentFilter(String a, String type) { addAction(a); } public IntentFilter(IntentFilter o) { actions.addAll(o.actions); cats.addAll(o.cats); }
    public final void addAction(String a) { actions.add(a); } public final int countActions() { return actions.size(); } public final String getAction(int i) { return actions.get(i); } public final boolean hasAction(String a) { return actions.contains(a); }
    public final java.util.Iterator<String> actionsIterator() { return actions.iterator(); }
    public final void addCategory(String c) { cats.add(c); } public final boolean hasCategory(String c) { return cats.contains(c); } public final void addDataScheme(String s) { schemes.add(s); }
    public final void addDataType(String t) {} public final void addDataAuthority(String h, String p) {} public final void addDataPath(String p, int t) {}
    public final void setPriority(int p) { prio = p; } public final int getPriority() { return prio; }
    public final boolean matchAction(String a) { return actions.contains(a); }
}
