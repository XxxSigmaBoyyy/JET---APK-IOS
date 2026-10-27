package android.content.res;
import java.util.ArrayList;
import java.util.Arrays;
/** Real state-matching ColorStateList (selector XML inflated with theme attribute resolution). */
public class ColorStateList implements android.os.Parcelable {
    private final int[][] mStateSpecs; private final int[] mColors; private final int mDefaultColor;
    private static final int[][] EMPTY = { new int[0] };
    public ColorStateList(int[][] states, int[] colors) {
        mStateSpecs = states == null ? EMPTY : states; mColors = colors == null ? new int[] { 0 } : colors;
        // AOSP: default = first item with empty state spec (the catch-all). Material button selectors put
        // state_enabled first and the catch-all (disabled/onSurface@12%) last — getDefaultColor() then returns the
        // near-invisible disabled colour, and setTintList/MaterialShapeDrawable paint magenta/empty. Prefer the first
        // colour when the only empty-spec item is not first (enabled-primary selectors).
        int def = mColors.length > 0 ? mColors[0] : 0; int emptyAt = -1;
        for (int i = 0; i < mStateSpecs.length && i < mColors.length; i++) if (mStateSpecs[i].length == 0) { emptyAt = i; break; }
        if (emptyAt == 0) def = mColors[0];
        else if (emptyAt < 0) { /* keep first */ }
        else def = mColors[0]; // non-leading catch-all: use the primary (first) colour as the "default" tint
        mDefaultColor = def;
    }
    public static ColorStateList valueOf(int c) { return new ColorStateList(EMPTY, new int[] { c }); }
    public int getDefaultColor() { return mDefaultColor; }
    public int getColorForState(int[] state, int def) {
        for (int i = 0; i < mStateSpecs.length && i < mColors.length; i++) if (stateSetMatches(mStateSpecs[i], state)) return mColors[i];
        return def;
    }
    static boolean stateSetMatches(int[] spec, int[] state) {
        if (state == null) state = new int[0];
        for (int s : spec) {
            if (s == 0) break;
            boolean want = s > 0; int a = want ? s : -s; boolean has = false;
            for (int x : state) if (x == a) { has = true; break; }
            if (has != want) return false;
        }
        return true;
    }
    public boolean isStateful() { for (int[] s : mStateSpecs) if (s.length > 0) return true; return false; }
    public boolean isOpaque() { for (int c : mColors) if ((c >>> 24) != 0xFF) return false; return true; }
    public ColorStateList withAlpha(int alpha) { int[] c = mColors.clone(); for (int i = 0; i < c.length; i++) c[i] = (c[i] & 0xFFFFFF) | (alpha << 24); return new ColorStateList(mStateSpecs, c); }
    public ColorStateList withLStar(float l) { return this; }
    public int[][] getStates() { return mStateSpecs; } public int[] getColors() { return mColors; }
    public boolean hasState(int s) { for (int[] sp : mStateSpecs) for (int x : sp) if (x == s || x == -s) return true; return false; }
    public int getChangingConfigurations() { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
    @Override public String toString() { return "ColorStateList{mColors=" + Arrays.toString(mColors) + "}"; }

    private static final String[] NAMES = { "state_above_anchor", "state_accelerated", "state_activated", "state_active", "state_checkable", "state_checked", "state_drag_can_accept", "state_drag_hovered", "state_empty", "state_enabled", "state_expanded", "state_first", "state_focused", "state_hovered", "state_last", "state_long_pressable", "state_middle", "state_multiline", "state_pressed", "state_selected", "state_single", "state_window_focused" };
    private static final int[] IDS = { 16842922, 16843547, 16843518, 16842914, 16842911, 16842912, 16843624, 16843625, 16842921, 16842910, 16842920, 16842916, 16842908, 16843623, 16842918, 16843324, 16842917, 16843597, 16842919, 16842913, 16842915, 16842909 };

    /** Inflate a &lt;selector&gt; element. Unresolvable items are skipped. */
    public static ColorStateList jetInflate(Resources r, org.w3c.dom.Element sel, Resources.Theme th) {
        jet.runtime.ResTable t = r.table(); ArrayList<int[]> specs = new ArrayList<>(); ArrayList<Integer> cols = new ArrayList<>();
        org.w3c.dom.NodeList items = sel.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            org.w3c.dom.Element e = (org.w3c.dom.Element) items.item(i);
            org.w3c.dom.NamedNodeMap m = e.getAttributes(); ArrayList<Integer> st = new ArrayList<>(); String color = null, alpha = null;
            for (int k = 0; k < m.getLength(); k++) {
                String n = m.item(k).getNodeName(), v = m.item(k).getNodeValue(); String local = n.substring(n.indexOf(':') + 1);
                if (local.equals("color")) color = v; else if (local.equals("alpha")) alpha = v;
                else if (local.startsWith("state_")) {
                    int id = 0; boolean fw = n.startsWith("android:");
                    if (fw) { int ix = Arrays.asList(NAMES).indexOf(local); if (ix >= 0) id = IDS[ix]; }
                    if (id == 0) { Integer a = t.idByName("attr", local); if (a != null) id = a; }
                    if (id != 0) st.add("true".equals(v) || "1".equals(v) || "0xffffffff".equalsIgnoreCase(v) ? id : -id);
                }
            }
            Integer c = color == null ? null : resolveColor(r, t, color, th);
            if (c == null) continue;
            if (alpha != null) { Float f = resolveFloat(t, alpha, th); if (f != null) { int a0 = c >>> 24; c = (c & 0xFFFFFF) | (Math.max(0, Math.min(255, Math.round(a0 * f))) << 24); } }
            int[] spec = new int[st.size()]; for (int k = 0; k < spec.length; k++) spec[k] = st.get(k);
            specs.add(spec); cols.add(c);
        }
        if (cols.isEmpty()) return null;
        int[] ca = new int[cols.size()]; for (int k = 0; k < ca.length; k++) ca[k] = cols.get(k);
        return new ColorStateList(specs.toArray(new int[0][]), ca);
    }
    static Integer resolveColor(Resources r, jet.runtime.ResTable t, String v, Resources.Theme th) {
        try {
            for (int guard = 0; guard < 8 && v != null; guard++) {
                if (v.startsWith("#")) return jet.runtime.ResTable.parseColor(v);
                if (v.startsWith("?")) {
                    if (th == null) th = Resources.jetLastTheme;
                    if (th == null) return null;
                    int aid = jet.runtime.ResTable.refId(v);
                    String nv = th.raw(aid);
                    if (nv == null && Resources.jetLastTheme != null && Resources.jetLastTheme != th)
                        nv = Resources.jetLastTheme.raw(aid);
                    v = nv; continue;
                }
                if (v.startsWith("@")) { int id = jet.runtime.ResTable.refId(v); if (id == 0) return null; if (t.hasXmlFile(id)) { ColorStateList c = r.getColorStateList(id, th); return c == null ? null : c.getDefaultColor(); } String nv = t.resolve("@" + Integer.toHexString(id)); if (nv == null || nv.equals(v)) return r.getColor(id); v = nv; continue; }
                return jet.runtime.ResTable.parseColor(v);
            }
        } catch (Throwable ignored) {}
        return null;
    }
    static Float resolveFloat(jet.runtime.ResTable t, String v, Resources.Theme th) {
        try {
            if (v.startsWith("?")) { if (th == null) th = Resources.jetLastTheme; v = th == null ? null : th.raw(jet.runtime.ResTable.refId(v)); }
            if (v != null && v.startsWith("@")) v = t.resolve(v);
            return v == null ? null : Float.parseFloat(v);
        } catch (Throwable e) { return null; }
    }
}
