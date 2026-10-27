package jet.runtime;
import android.graphics.drawable.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/** Loads a JET bundle (output of jetc/apk2bundle.py) and resolves resource values. */
public final class ResTable {
    public final String pkg;
    public final float density;   // px per dp (2.0 = xhdpi; iPhone @2x)
    public final float scaledDensity;
    private final File dir;
    private final Map<Integer, String[]> names = new HashMap<>();   // id -> {type,name}
    private final Map<Integer, String> values = new HashMap<>();
    private final Map<Integer, String[]> arrays = new HashMap<>();
    /** First concrete (non-?attr) value seen per attr key across styles — Theme.raw ultimate fallback. */
    /** Raw array items (may be @refs); null if not an array resource. */
    public String[] array(int id) { return arrays.get(id); }
    private final java.util.HashMap<Integer, java.util.Map<String, String>> plurals = new java.util.HashMap<>();
    /** Plural form (English-style rule: 1 -> "one", else "other"; falls back to "other" then any). */
    public String plural(int id, int qty) {
        java.util.Map<String, String> m = plurals.get(id); if (m == null) return null;
        String v = m.get(qty == 1 ? "one" : (qty == 0 && m.containsKey("zero") ? "zero" : "other"));
        if (v == null) v = m.get("other"); if (v == null && !m.isEmpty()) v = m.values().iterator().next(); return v;
    }
    private final Map<Integer, Integer> styleParent = new HashMap<>();
    private final Map<Integer, LinkedHashMap<String, String>> styleItems = new HashMap<>();
    private final Map<Integer, Element> drawables = new HashMap<>();
    private final Map<Integer, String> layoutFiles = new HashMap<>();
    private final Map<Integer, Drawable> drawableCache = new HashMap<>();

    public ResTable(File dir, float density) throws Exception {
        this.dir = dir; this.density = density; this.scaledDensity = density;
        Element root = parse(new File(dir, "values.xml")).getDocumentElement();
        FrameworkValues.fill(values, names);
        pkg = root.getAttribute("package");
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (!(nl.item(i) instanceof Element)) continue;
            Element e = (Element) nl.item(i);
            int id = (int) Long.parseLong(e.getAttribute("id").substring(2), 16);
            String type = e.getTagName().equals("style") ? "style" : e.getTagName().equals("drawable") ? "drawable" : e.getAttribute("type");
            names.put(id, new String[]{type, e.getAttribute("name")});
            switch (e.getTagName()) {
                case "array": { NodeList it = e.getElementsByTagName("item"); String[] arr = new String[it.getLength()]; for (int k = 0; k < arr.length; k++) arr[k] = ((Element) it.item(k)).getAttribute("value"); arrays.put(id, arr); if ("plurals".equals(e.getAttribute("type"))) { java.util.HashMap<String, String> pm = new java.util.HashMap<>(); for (int k = 0; k < arr.length; k++) pm.put(((Element) it.item(k)).getAttribute("q"), arr[k]); plurals.put(id, pm); } break; }
                case "value": values.put(id, e.getAttribute("value")); if (e.hasAttribute("file")) layoutFiles.put(id, e.getAttribute("file"));
                    else { String vv = e.getAttribute("value"); if (vv != null && (vv.startsWith("res/") || vv.startsWith("raw/") || vv.startsWith("drawable/") || vv.startsWith("font/"))) layoutFiles.put(id, vv); }
                    break;
                case "drawable": drawables.put(id, e); if (e.getAttribute("file").endsWith(".xml")) layoutFiles.put(id, e.getAttribute("file")); break; // Resources.getXml(drawable) = its XML, as on Android (Coil/Compose vector loaders)
                case "style": {
                    styleParent.put(id, (int) Long.parseLong(e.getAttribute("parent").substring(2), 16));
                    LinkedHashMap<String, String> items = new LinkedHashMap<>();
                    NodeList its = e.getElementsByTagName("item");
                    for (int j = 0; j < its.getLength(); j++) {
                        Element it = (Element) its.item(j);
                        items.put(it.getAttribute("name"), it.getAttribute("value"));
                    }
                    styleItems.put(id, items);
                    break;
                }
                case "res": if (e.hasAttribute("file")) layoutFiles.put(id, e.getAttribute("file")); break;
            }
        }
    }

    private Map<Integer, String> fwAttrs;
    /** Normalise a style-item name: aapt2 sometimes emits the attr id as "0x7f04014e" instead of "colorPrimary".
     *  Theme.raw / TypedArray look up by attrKey (the human name), so hex keys would never match. */
    private String styleItemKey(String name) {
        if (name != null && name.length() > 3 && name.charAt(0) == '0' && (name.charAt(1) == 'x' || name.charAt(1) == 'X')) {
            try { int a = (int) Long.parseLong(name.substring(2), 16); String k = attrKey(a); if (k != null && !k.startsWith("0x")) return k; } catch (NumberFormatException ignored) {}
        }
        return name;
    }
    /** Key under which a style item for attribute {@code id} is stored in values.xml. */
    public String attrKey(int id) {
        // Style items and theme bags are keyed by attribute *name* ("colorPrimary"), never by the hex id.
        // Framework names come from fwattrs.properties; app names from values.xml. Fall back to 0x%08x when
        // unknown OR when the decoder stripped the name ("(name removed)") — otherwise every app attr
        // collapses to one bag key and TypedArray/theme resolution cross-wires values (e.g. colour into
        // autoSizePresetSizes).
        if ((id >>> 24) == 0x01) {
            if (fwAttrs == null) {
                fwAttrs = new HashMap<>();
                File f = new File(dir, "fwattrs.properties");
                if (f.exists()) try (java.io.Reader r = new java.io.FileReader(f)) {
                    Properties p = new Properties(); p.load(r);
                    for (String k : p.stringPropertyNames()) fwAttrs.put((int) Long.parseLong(k.substring(2), 16), p.getProperty(k));
                } catch (Exception ignored) {}
            }
            String n = fwAttrs.get(id);
            if (n != null && !n.isEmpty()) return n;
        } else {
            String n = name(id);
            if (n != null && !n.isEmpty() && !"(name removed)".equals(n)) return n;
        }
        return String.format("0x%08x", id);
    }
    public boolean isStyle(int id) { return styleItems.containsKey(id); }
    /** "@7F120000" / "@android:0106000D" / "?7F04..." -> id (0 if not a ref). */
    public static int refId(String v) {
        if (v == null || v.length() < 2 || (v.charAt(0) != '@' && v.charAt(0) != '?')) return 0;
        String h = v.substring(1); int c = h.indexOf(':'); if (c >= 0) h = h.substring(c + 1);
        try { return (int) Long.parseLong(h, 16); } catch (NumberFormatException e) { return 0; }
    }

    public static Document parse(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        return dbf.newDocumentBuilder().parse(f);
    }

    /** Backing file of a file-based resource (raw/xml/drawable...), or null. */
    public java.io.File bundleDir() { return dir; }
    public java.io.File resFile(int id) { String f = layoutFiles.get(id); return f == null ? null : new java.io.File(dir, f); }
    public boolean hasXmlFile(int id) { return layoutFiles.containsKey(id) || FrameworkLayouts.get(id) != null; }
    public DomPullParser xmlParser(int id) { return new DomPullParser(layout(id), this); }
    public Element layout(int id) {
        String f = layoutFiles.get(id);
        if (f == null && FrameworkLayouts.get(id) != null) {
            try { return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(FrameworkLayouts.get(id)))).getDocumentElement(); } catch (Exception e) { throw new RuntimeException(e); }
        }
        if (f == null) {
            if (id == 0) {
                try {
                    return javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new java.io.ByteArrayInputStream(
                            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .getDocumentElement();
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            throw new RuntimeException("layout not found 0x" + Integer.toHexString(id));
        }
        try { return parse(new File(dir, f)).getDocumentElement(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public String name(int id) { String[] n = names.get(id); return n == null ? null : n[1]; }
    public String fullName(int id) { String[] n = names.get(id); return n == null ? "0x" + Integer.toHexString(id) : ((id >>> 24) == 1 ? "android" : pkg) + ":" + n[0] + "/" + n[1]; }
    public Integer idByName(String type, String name) {
        for (Map.Entry<Integer, String[]> e : names.entrySet()) if (e.getValue()[0].equals(type) && e.getValue()[1].equals(name)) return e.getKey();
        return null;
    }

    /** Flattened style attributes along the parent chain (self-referential ?attr/X→X items skipped). */
    public Map<String, String> style(int id) {
        LinkedList<Map<String, String>> chain = new LinkedList<>();
        Integer cur = id;
        while (cur != null && styleItems.containsKey(cur)) { chain.addFirst(styleItems.get(cur)); cur = styleParent.get(cur); }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map<String, String> m : chain) for (Map.Entry<String, String> e : m.entrySet()) {
            String key = styleItemKey(e.getKey()), v = e.getValue();
            // AOSP bag merge: ?attr/<same attr> is a no-op (keeps inherited). aapt2 emits these for AppCompat/Material
            // bridge themes; putAll would overwrite Theme.Material's concrete @color → Theme.raw self-ref → magenta.
            if (v != null && v.startsWith("?")) {
                int a = refId(v); String ak = attrKey(a);
                if (key.equals(ak) || key.equals(String.format("0x%08x", a)) || key.equals(name(a))) continue;
            }
            out.put(key, v);
        }
        return out;
    }

    public static int ref(String v) { return refId(v); }

    /** Follow @ references to a primitive value string. */
    public String resolve(String v) {
        int guard = 0;
        while (v != null && v.startsWith("@") && guard++ < 10) {
            int id = ref(v);
            if (values.containsKey(id)) v = values.get(id); else return v;
        }
        return v;
    }
    public String string(int id) { return values.get(id); }
    public int color(int id) {
        String v = resolve("@" + Integer.toHexString(id));
        if (v != null && v.startsWith("?") && android.content.res.Resources.jetLastTheme != null) {
            String r = android.content.res.Resources.jetLastTheme.raw(refId(v));
            if (r != null) v = r;
            // follow @ after theme
            if (v != null && v.startsWith("@")) v = resolve(v);
        }
        if (v == null || !v.startsWith("#")) return 0;
        return parseColor(v);
    }
    public float dimension(int id) { return dimen(values.get(id)); }

    public static int parseColor(String s) {
        s = s.substring(1);
        long v = Long.parseLong(s, 16);
        if (s.length() <= 6) v |= 0xFF000000L;
        return (int) v;
    }
    /** "10.000000dip" / "40sp" / "-1" -> px (negative layout constants preserved). */
    public float dimen(String s) {
        if (s != null && s.startsWith("?") && android.content.res.Resources.jetLastTheme != null) { String r = android.content.res.Resources.jetLastTheme.raw(refId(s)); if (r != null) s = r; }
        s = resolve(s);
        if (s == null) return 0;
        if (s.startsWith("?")) { System.err.println("[RES] unresolved theme attr in dimension: " + s); return 0; }
        if (s.startsWith("#")) return 0; // colour leaked into a dimension attr (bad theme bag) — not a size
        if (s.endsWith("dip")) return Float.parseFloat(s.substring(0, s.length() - 3)) * density;
        if (s.endsWith("dp")) return Float.parseFloat(s.substring(0, s.length() - 2)) * density;
        if (s.endsWith("sp")) return Float.parseFloat(s.substring(0, s.length() - 2)) * scaledDensity;
        if (s.endsWith("px")) return Float.parseFloat(s.substring(0, s.length() - 2));
        return Float.parseFloat(s);
    }
    public static int intVal(String s) {
        if (s == null) return 0;
        if (s.startsWith("0x")) return (int) Long.parseLong(s.substring(2), 16);
        return (int) Float.parseFloat(s);
    }

    public Drawable drawable(String v) {
        if (v == null) return null;
        if (v.startsWith("#")) return new ColorDrawable(parseColor(v));
        if (!v.startsWith("@")) return null;
        int id = ref(v);
        // Like Resources: cache the ConstantState, hand every caller its own Drawable (setColor/setState/bounds are per instance).
        Drawable.ConstantState cached = drawableCS.get(id);
        if (cached != null) return cached.newDrawable();
        Drawable d = null;
        Element e = drawables.get(id);
        if (e == null) {
            String r = resolve(v);
            if (r != null && r.startsWith("#")) d = new ColorDrawable(parseColor(r));
        } else switch (e.getAttribute("kind")) {
            case "color": d = drawable(resolveColorish(e.getAttribute("value"))); break;
            case "selector": {
                StateListDrawable s = new StateListDrawable();
                NodeList st = e.getElementsByTagName("state");
                for (int i = 0; i < st.getLength(); i++) {
                    Element x = (Element) st.item(i);
                    // AOSP StateSet: full attr-id state set; "!state_x" (state_x="false") is the negated attr id.
                    java.util.List<Integer> set = new java.util.ArrayList<>();
                    for (String w : x.getAttribute("when").split(",")) {
                        if (w.isEmpty()) continue;
                        boolean neg = w.startsWith("!"); int aid = stateAttr(neg ? w.substring(1) : w);
                        if (aid != 0) set.add(neg ? -aid : aid);
                    }
                    int[] arr = new int[set.size()]; for (int k = 0; k < arr.length; k++) arr[k] = set.get(k);
                    s.addState(arr, drawable(x.getAttribute("drawable")));
                }
                d = s; break;
            }
            case "shape": d = buildShape(e, ""); break;
            case "layer-list": {
                NodeList ls = e.getElementsByTagName("layer");
                java.util.List<Drawable> kids = new java.util.ArrayList<>(); java.util.List<Element> els = new java.util.ArrayList<>();
                for (int i = 0; i < ls.getLength(); i++) {
                    Element x = (Element) ls.item(i); Drawable k = null;
                    if (!x.getAttribute("drawable").isEmpty()) k = drawable(x.getAttribute("drawable"));
                    else if ("shape".equals(x.getAttribute("inline"))) k = buildShape(x, "s.");
                    else if (!x.getAttribute("color").isEmpty()) { String c = x.getAttribute("color"); String r = c.startsWith("#") ? c : resolve(c); k = new ColorDrawable(r != null && r.startsWith("#") ? parseColor(r) : 0); }
                    else if (!x.getAttribute("vector").isEmpty()) { try { k = android.graphics.drawable.VectorDrawable.parse(parse(new File(dir, x.getAttribute("vector"))).getDocumentElement(), this); } catch (Exception ex) { k = null; } }
                    if (k != null && drawableCache.containsValue(k)) k = k.getConstantState() != null ? k.getConstantState().newDrawable() : k;
                    kids.add(k); els.add(x);
                }
                android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(kids.toArray(new Drawable[0]));
                for (int i = 0; i < els.size(); i++) {
                    Element x = els.get(i);
                    if (!x.getAttribute("id").isEmpty()) ld.setId(i, ref(x.getAttribute("id")));
                    int l = x.getAttribute("left").isEmpty() ? 0 : (int) dimen(x.getAttribute("left")), t = x.getAttribute("top").isEmpty() ? 0 : (int) dimen(x.getAttribute("top"));
                    int r = x.getAttribute("right").isEmpty() ? 0 : (int) dimen(x.getAttribute("right")), b = x.getAttribute("bottom").isEmpty() ? 0 : (int) dimen(x.getAttribute("bottom"));
                    if ((l | t | r | b) != 0) ld.setLayerInset(i, l, t, r, b);
                    if (!x.getAttribute("width").isEmpty()) ld.setLayerWidth(i, (int) dimen(x.getAttribute("width")));
                    if (!x.getAttribute("height").isEmpty()) ld.setLayerHeight(i, (int) dimen(x.getAttribute("height")));
                    if (!x.getAttribute("gravity").isEmpty()) ld.setLayerGravity(i, intVal(x.getAttribute("gravity")));
                }
                d = ld; break;
            }
            case "vector":
                try { d = android.graphics.drawable.VectorDrawable.parse(parse(new File(dir, e.getAttribute("file"))).getDocumentElement(), this); } catch (Exception ex) { d = new android.graphics.drawable.VectorDrawable(); }
                break;
            case "ninepatch":
                try {
                    int dens = e.getAttribute("density").isEmpty() ? 160 : Integer.parseInt(e.getAttribute("density")); if (dens == 1) dens = 160;
                    d = android.graphics.drawable.NinePatchDrawable.fromCompiledPng(new File(dir, e.getAttribute("file")), dens, 320);
                } catch (Exception ex) { d = null; }
                break;
            case "bitmap":
                try {
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inDensity = e.getAttribute("density").isEmpty() ? 160 : Integer.parseInt(e.getAttribute("density"));
                    if (o.inDensity == 1) o.inDensity = 160; // anydpi
                    o.inTargetDensity = 320; o.inScaled = o.inDensity != 0;
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(new File(dir, e.getAttribute("file")).getPath(), o);
                    d = bm == null ? null : new BitmapDrawable(bm);
                } catch (Exception ex) { d = null; }
                break;
        }
        if (d != null && d.getConstantState() != null) { Drawable.ConstantState cs = d.getConstantState(); drawableCS.put(id, cs); return cs.newDrawable(); } // template stays pristine
        return d;
    }
    /** android.R.attr ids of the drawable-state attributes used in <selector> items. */
    static int stateAttr(String n) {
        switch (n) {
            case "state_window_focused": return 16842909; case "state_focused": return 16842908; case "state_enabled": return 16842910;
            case "state_checkable": return 16842911; case "state_checked": return 16842912; case "state_selected": return 16842913;
            case "state_pressed": return 16842919; case "state_activated": return 16843518; case "state_hovered": return 16843623;
            case "state_active": return 16842914; case "state_single": return 16842915; case "state_first": return 16842916;
            case "state_middle": return 16842917; case "state_last": return 16842918; case "state_accelerated": return 16843547;
            case "state_drag_can_accept": return 16843624; case "state_drag_hovered": return 16843625;
            default: return 0;
        }
    }
    private final Map<Integer, Drawable.ConstantState> drawableCS = new HashMap<>();
    /** Density (dpi) the bundle recorded for a bitmap drawable resource, 160 if unknown, 0 for nodpi. */
    public int bitmapDensity(int id) {
        Element e = drawables.get(id);
        if (e == null || e.getAttribute("density").isEmpty()) return 160;
        int d = Integer.parseInt(e.getAttribute("density")); return d == 1 ? 160 : d;
    }
    private int colorOf(String v) { if (v == null || v.isEmpty()) return 0; String r = v.startsWith("#") ? v : resolve(v); return r != null && r.startsWith("#") ? parseColor(r) : 0; }
    private android.content.res.Resources mCslRes;
    private android.content.res.ColorStateList cslOf(String v) {
        if (v == null || v.isEmpty()) return null;
        // A colour reference may name a <selector> colour-state-list file (AOSP: <solid android:color="@color/csl"/>);
        // keep the full state list so the shape's colour follows the host view's drawable state.
        if (v.startsWith("@") && !v.startsWith("@android:")) {
            try {
                int id = (int) Long.parseLong(v.substring(1), 16);
                if (hasXmlFile(id)) {
                    if (mCslRes == null) mCslRes = new android.content.res.Resources(this);
                    android.content.res.ColorStateList c = mCslRes.getColorStateList(id, null);
                    if (c != null) return c;
                }
            } catch (RuntimeException ignore) {}
        }
        return android.content.res.ColorStateList.valueOf(colorOf(v));
    }
    /** <shape> attributes (flattened by apk2bundle) -> GradientDrawable. */
    android.graphics.drawable.GradientDrawable buildShape(Element e, String p) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        String v;
        if (!(v = e.getAttribute(p + "shape")).isEmpty()) g.setShape(intVal(v));
        if (!(v = e.getAttribute(p + "solid")).isEmpty()) { android.content.res.ColorStateList c = cslOf(v); if (c != null) g.setColor(c); }
        if (!(v = e.getAttribute(p + "radius")).isEmpty()) g.setCornerRadius(dimen(v));
        String tl = e.getAttribute(p + "topLeftRadius"), tr = e.getAttribute(p + "topRightRadius"), bl = e.getAttribute(p + "bottomLeftRadius"), br = e.getAttribute(p + "bottomRightRadius");
        if (!(tl + tr + bl + br).isEmpty()) {
            float base = g.getCornerRadius();
            float a = tl.isEmpty() ? base : dimen(tl), b2 = tr.isEmpty() ? base : dimen(tr), c = br.isEmpty() ? base : dimen(br), d2 = bl.isEmpty() ? base : dimen(bl);
            g.setCornerRadii(new float[]{a, a, b2, b2, c, c, d2, d2});
        }
        if (!(v = e.getAttribute(p + "stroke_width")).isEmpty()) {
            android.content.res.ColorStateList sc = cslOf(e.getAttribute(p + "stroke_color"));
            String dw = e.getAttribute(p + "stroke_dashWidth"), dg = e.getAttribute(p + "stroke_dashGap");
            g.setStroke(Math.max(1, Math.round(dimen(v))), sc == null ? android.content.res.ColorStateList.valueOf(0) : sc, dw.isEmpty() ? 0 : dimen(dw), dg.isEmpty() ? 0 : dimen(dg));
        }
        String sc0 = e.getAttribute(p + "g_startColor"), ec = e.getAttribute(p + "g_endColor"), cc = e.getAttribute(p + "g_centerColor");
        if (!sc0.isEmpty() || !ec.isEmpty()) {
            g.setColors(cc.isEmpty() ? new int[]{colorOf(sc0), colorOf(ec)} : new int[]{colorOf(sc0), colorOf(cc), colorOf(ec)});
            if (!(v = e.getAttribute(p + "g_type")).isEmpty()) g.setGradientType(intVal(v));
            if (!(v = e.getAttribute(p + "g_gradientRadius")).isEmpty()) { try { g.setGradientRadius(dimen(v)); } catch (Throwable ignored) {} }
            int ang = (v = e.getAttribute(p + "g_angle")).isEmpty() ? 0 : (((int) Float.parseFloat(v)) % 360 + 360) % 360;
            android.graphics.drawable.GradientDrawable.Orientation[] os = { android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP, android.graphics.drawable.GradientDrawable.Orientation.BR_TL, android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT,
                android.graphics.drawable.GradientDrawable.Orientation.TR_BL, android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, android.graphics.drawable.GradientDrawable.Orientation.TL_BR };
            g.setOrientation(os[(ang / 45) % 8]);
        }
        String sw = e.getAttribute(p + "size_width"), sh = e.getAttribute(p + "size_height");
        if (!sw.isEmpty() || !sh.isEmpty()) g.setSize(sw.isEmpty() ? -1 : Math.round(dimen(sw)), sh.isEmpty() ? -1 : Math.round(dimen(sh)));
        return g;
    }
    private String resolveColorish(String v) { String r = resolve(v); return r; }
}
