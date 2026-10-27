package jet.runtime;
import android.util.AttributeSet;
import org.w3c.dom.*;
import java.util.*;
/** AttributeSet built from a decoded layout element, merged with its style. */
public final class XmlAttrs implements AttributeSet {
    private final LinkedHashMap<String, String> own;
    private final Map<String, String> style;
    private Map<String, String> def = Collections.emptyMap();
    private android.content.res.Resources.Theme theme;
    /** Resolve ?attr values through this theme in get(). */
    public XmlAttrs themed(android.content.res.Resources.Theme th) {
        Map<String, String> st = style;
        // style="?android:attr/spinnerItemStyle" (theme-attribute style reference, as in AOSP simple_spinner_item)
        String s = own.get("style");
        if (st.isEmpty() && s != null && s.startsWith("?") && th != null) {
            try { String r = th.raw(ResTable.refId(s)); if (r != null && r.startsWith("@")) st = th.getResources().table().style(ResTable.refId(r)); } catch (Throwable ignored) {}
        }
        XmlAttrs x = new XmlAttrs(own, st, def); x.theme = th; return x;
    }
    private XmlAttrs(LinkedHashMap<String, String> own, Map<String, String> style, Map<String, String> def) { this.own = own; this.style = style; this.def = def; }
    /** Same attributes with a defStyleAttr/defStyleRes layer below the explicit style (Android precedence). */
    public XmlAttrs withDefaults(Map<String, String> d) { if (d == null || d.isEmpty()) return this; XmlAttrs x = new XmlAttrs(own, style, d); x.theme = theme; return x; }
    public static XmlAttrs defaultsOnly(Map<String, String> d) { return new XmlAttrs(new LinkedHashMap<>(), Collections.emptyMap(), d); }
    /** Theme default style for defStyleAttr (or defStyleRes), as a merged item map. */
    public static Map<String, String> defStyle(android.content.Context c, int defStyleAttr, int defStyleRes) {
        try {
            ResTable t = c.getResources().table(); int id = 0;
            if (defStyleAttr != 0 && c.getTheme() != null) { String v = c.getTheme().raw(defStyleAttr); if (v != null && v.startsWith("@")) id = ResTable.refId(v); }
            if (id == 0) id = defStyleRes;
            return id == 0 ? Collections.emptyMap() : t.style(id);
        } catch (Throwable e) { return Collections.emptyMap(); }
    }
    public XmlAttrs(Element e, ResTable t) {
        own = new LinkedHashMap<>();
        NamedNodeMap m = e.getAttributes();
        for (int i = 0; i < m.getLength(); i++) {
            org.w3c.dom.Attr at = (org.w3c.dom.Attr) m.item(i);
            String n = at.getNodeName();
            if (n.startsWith("xmlns")) continue;
            // Namespace-aware DOM: nodeName may be local (fillType) with URI .../android.
            String uri = at.getNamespaceURI();
            if (n.startsWith("android:")) n = n.substring(8);
            else if (n.startsWith("app:")) n = n.substring(4);
            else if (uri != null && uri.endsWith("/android") && n.contains(":") == false) { /* keep local name */ }
            else if (at.getLocalName() != null && !at.getLocalName().isEmpty() && uri != null) n = at.getLocalName();
            // androguard emits android_UNKNOWN_SYSTEM_ATTRIBUTE_<hex> for app attrs missing from
            // public.json (e.g. ConstraintLayout layout_constraint*). TypedArray looks up by
            // ResTable.attrKey → "0x########" when names are stripped — alias both forms.
            String v = at.getNodeValue();
            String unk = normalizeUnknownAttr(n);
            if (unk != null) {
                own.put(unk, v);           // 0x7f040459
                own.put(n, v);             // keep original for debugging
                String human = t.attrKey((int) Long.parseLong(unk.substring(2), 16));
                if (human != null && !human.equals(unk)) own.put(human, v);
            } else {
                own.put(n, v);
                // Also index hex form when the name is a real attr id we can resolve
                if (t != null) {
                    Integer id = t.idByName("attr", n);
                    if (id != null) own.put(String.format("0x%08x", id), v);
                }
            }
        }
        String s = own.get("style");
        style = s != null && s.startsWith("@") ? t.style(ResTable.ref(s)) : Collections.emptyMap();
    }
    /** "android_UNKNOWN_SYSTEM_ATTRIBUTE_7f040459" / "UNKNOWN_SYSTEM_ATTRIBUTE_7f040459" → "0x7f040459". */
    private static String normalizeUnknownAttr(String n) {
        if (n == null) return null;
        String p = "android_UNKNOWN_SYSTEM_ATTRIBUTE_";
        String p2 = "UNKNOWN_SYSTEM_ATTRIBUTE_";
        String hex = null;
        if (n.startsWith(p)) hex = n.substring(p.length());
        else if (n.startsWith(p2)) hex = n.substring(p2.length());
        if (hex == null || hex.isEmpty()) return null;
        try { Long.parseLong(hex, 16); return "0x" + hex.toLowerCase(); } catch (NumberFormatException e) { return null; }
    }
    /** Unwrap any JET AttributeSet (XmlAttrs or DomPullParser at a START_TAG). */
    public static XmlAttrs of(AttributeSet a) { return a instanceof XmlAttrs ? (XmlAttrs) a : a instanceof DomPullParser ? ((DomPullParser) a).jetAttrs() : null; }
    /** Element attribute first, then style. */
    public String get(String name) { String v = own.get(name); if (v == null) v = style.get(name); if (v == null) v = def.get(name); if (v != null && theme != null && v.startsWith("?")) { String r = theme.raw(ResTable.refId(v)); if (r != null) v = r; }
        return v == null || v.startsWith("<") || "@0".equals(v) || "@null".equals(v) ? null : v; }
    public int getAttributeCount() { return own.size(); }
    public String getAttributeName(int i) { return new ArrayList<>(own.keySet()).get(i); }
    public String getAttributeValue(int i) { return new ArrayList<>(own.values()).get(i); }
    public String getAttributeValue(String ns, String name) { return get(name); }
}
