package jet.apk;

import org.w3c.dom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * On-device APK -> JET bundle (Java port of jetc/apk2bundle.py; same output layout, consumed by jet.runtime.ResTable):
 *   app.properties, values.xml, layout|xml|menu|anim|.../<id>.xml, drawable/, color/, raw/, assets/, icon.png, fwattrs.properties.
 * Framework resources (values-fw.xml, drawables-fw.xml, fwcolor/, fwdrawable/) come from {@code fwDir} (generated once from
 * android.jar by the same converter, see {@link #framework}); they are spliced in / linked exactly like apk2bundle does.
 */
public final class ApkBundler {
    static final int JET_SDK = Integer.getInteger("jet.sdk", 34);
    static final Set<String> XMLFILE_TYPES = new HashSet<>(Arrays.asList("layout", "xml", "menu", "anim", "animator", "interpolator", "transition", "navigation"));
    static final String A = Axml.ANDROID_NS;

    final ZipFile zip; final Arsc arsc; final File out; final String pkg;
    final Set<String> customViews = new TreeSet<>();

    ApkBundler(File apk, File out) throws IOException {
        zip = new ZipFile(apk); this.out = out;
        byte[] man = read("AndroidManifest.xml");
        manifest = dom(Axml.toXml(man));
        pkg = manifest.getDocumentElement().getAttribute("package");
        byte[] ar = readOpt("resources.arsc");
        arsc = ar == null ? new Arsc(new byte[8]) : new Arsc(ar, null);
    }
    final Document manifest;

    /** Build the bundle. Returns app.properties as written. */
    public static Properties bundle(File apk, File out, File fwDir) throws Exception {
        out.mkdirs(); new File(out, "layout").mkdirs();
        ApkBundler b = new ApkBundler(apk, out);
        try { return b.run(fwDir); } finally { b.zip.close(); }
    }

    byte[] read(String path) throws IOException {
        byte[] d = readOpt(path); if (d == null) throw new FileNotFoundException(path); return d;
    }
    byte[] readOpt(String path) throws IOException {
        ZipEntry e = zip.getEntry(path); if (e == null) return null;
        try (InputStream in = zip.getInputStream(e)) { return in.readAllBytes(); }
    }
    void write(String rel, byte[] data) throws IOException { File f = new File(out, rel); f.getParentFile().mkdirs(); Files.write(f.toPath(), data); }
    void write(String rel, String s) throws IOException { write(rel, s.getBytes(StandardCharsets.UTF_8)); }

    static Document dom(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static String q(String v) { return '"' + Axml.esc(v == null ? "" : v, true) + '"'; }
    static String hid(int id) { return String.format("0x%08x", id); }

    // ---- values.xml ----
    Properties run(File fwDir) throws Exception {
        StringBuilder L = new StringBuilder();
        L.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<values package=").append(q(pkg)).append(">\n");
        Map<Integer, String> ATTR = Axml.fwAttrs();
        List<Integer> ids = new ArrayList<>(arsc.pub.keySet()); Collections.sort(ids, (x, y) -> Long.compare(x & 0xffffffffL, y & 0xffffffffL));
        for (int rid : ids) {
            String t = arsc.pub.get(rid)[0], n = arsc.pub.get(rid)[1], h = hid(rid);
            Map.Entry<Arsc.Config, Arsc.Entry> ce = arsc.defaultCfg(rid, JET_SDK);
            if (ce == null) { L.append("<res id=\"").append(h).append("\" type=\"").append(t).append("\" name=\"").append(n).append("\"/>\n"); continue; }
            Arsc.Entry e = ce.getValue();
            if (t.equals("style") && !e.complex) { L.append("<res id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append("/>\n"); continue; }
            if (t.equals("style")) {
                L.append("<style id=\"").append(h).append("\" name=").append(q(n)).append(" parent=\"").append(hid(e.parent)).append("\">\n");
                for (int i = 0; i < e.bagKeys.length; i++) {
                    String an = ATTR.get(e.bagKeys[i]); if (an == null) an = hid(e.bagKeys[i]);
                    L.append("  <item name=").append(q(an)).append(" value=").append(q(e.fmtBag(i))).append("/>\n");
                }
                L.append("</style>\n");
            } else if (XMLFILE_TYPES.contains(t) && !e.complex && e.fmt().endsWith(".xml")) {
                String path = e.fmt(); String x = Axml.toXml(read(path));
                String fn = String.format("%s/%08x.xml", t, rid);
                write(fn, x);
                if (t.equals("layout")) { Matcher m = Pattern.compile("<([a-zA-Z_][\\w]*(?:\\.[\\w$]+)+)[\\s>/]").matcher(x); while (m.find()) customViews.add(m.group(1)); }
                L.append("<res id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" file=\"").append(fn).append("\"/>\n");
            } else if (t.equals("drawable") || t.equals("mipmap")) {
                int bestD = -1; String path = null;
                for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(rid)) {
                    int d = c.getKey().densQualifier();
                    if (path == null || d > bestD) { bestD = d; path = c.getValue().complex ? "@complex" : c.getValue().fmt(); }
                }
                String gray = "<drawable id=\"" + h + "\" name=" + q(n) + " kind=\"color\" value=\"#FF888888\"></drawable>\n";
                if (path == null || path.startsWith("@")) L.append(gray);
                else if (path.endsWith(".xml")) {
                    try { String[] kb = drawableXml(path, "drawable/"); L.append("<drawable id=\"").append(h).append("\" name=").append(q(n)).append(' ').append(kb[0]).append('>').append(kb[1]).append("</drawable>\n"); }
                    catch (Exception ex) { L.append(gray); }
                } else {
                    try {
                        byte[] data = read(path);
                        String ext = ext(path);
                        String fn = String.format("drawable/%08x%s", rid, ext.isEmpty() ? ".bin" : ext);
                        if (path.endsWith(".9.png")) {
                            String fn9 = fn.substring(0, fn.length() - 4) + ".9.png"; write(fn9, data);
                            L.append("<drawable id=\"").append(h).append("\" name=").append(q(n)).append(" kind=\"ninepatch\" file=\"").append(fn9).append("\" density=\"").append(bestD).append("\"/>\n");
                        } else {
                            write(fn, data);
                            L.append("<drawable id=\"").append(h).append("\" name=").append(q(n)).append(" kind=\"bitmap\" file=\"").append(fn).append("\" density=\"").append(bestD).append("\"/>\n");
                        }
                    } catch (Exception ex) { L.append(gray); }
                }
            } else if (t.equals("color") && !e.complex && e.fmt().endsWith(".xml")) {
                String path = e.fmt(); String x = null;
                try { x = Axml.toXml(read(path)); } catch (Exception ex) { x = null; }
                String fn = String.format("color/%08x.xml", rid), dflt = "#FF888888";
                if (x != null && !x.isEmpty()) { write(fn, x); dflt = cslDefault(x, dflt); } else x = null;
                L.append("<value id=\"").append(h).append("\" type=\"color\" name=").append(q(n)).append(" value=").append(q(dflt)).append(x != null ? " file=\"" + fn + "\"" : "").append("/>\n");
            } else if (t.equals("array") && e.complex) {
                L.append("<array id=\"").append(h).append("\" type=\"array\" name=").append(q(n)).append(">\n");
                for (int i = 0; i < e.bagKeys.length; i++) L.append("  <item value=").append(q(e.fmtBag(i))).append("/>\n");
                L.append("</array>\n");
            } else if (t.equals("plurals") && e.complex) {
                L.append("<array id=\"").append(h).append("\" type=\"plurals\" name=").append(q(n)).append(">\n");
                for (int i = 0; i < e.bagKeys.length; i++) L.append("  <item q=\"").append(plural(e.bagKeys[i])).append("\" value=").append(q(e.fmtBag(i))).append("/>\n");
                L.append("</array>\n");
            } else if (e.complex) {
                L.append("<res id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" complex=\"true\"/>\n");
            } else if (t.equals("raw") || t.equals("font") || fileLike(e.fmt())) {
                String path = e.fmt();
                try {
                    byte[] data = read(path); String ext = ext(path);
                    String fn = String.format("%s/%08x%s", (t.equals("raw") || t.equals("font")) ? t : "raw", rid, ext.isEmpty() ? ".bin" : ext);
                    write(fn, data);
                    L.append("<res id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" file=\"").append(fn).append("\"/>\n");
                } catch (Exception ex) {
                    L.append("<value id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" value=").append(q(path)).append("/>\n");
                }
            } else {
                L.append("<value id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" value=").append(q(e.fmt())).append("/>\n");
            }
        }
        if (fwDir != null && new File(fwDir, "values-fw.xml").exists() && !"1".equals(System.getProperty("jet.nofwres"))) {
            L.append(new String(Files.readAllBytes(new File(fwDir, "values-fw.xml").toPath()), StandardCharsets.UTF_8)).append('\n');
            link(new File(out, "fwcolor"), new File(fwDir, "fwcolor"));
            if (new File(fwDir, "drawables-fw.xml").exists()) {
                L.append(new String(Files.readAllBytes(new File(fwDir, "drawables-fw.xml").toPath()), StandardCharsets.UTF_8)).append('\n');
                link(new File(out, "fwdrawable"), new File(fwDir, "fwdrawable"));
            }
        }
        L.append("</values>");
        write("values.xml", L.toString());
        Properties props = manifestProps();
        // fwattrs.properties (attr id -> name)
        StringBuilder fa = new StringBuilder();
        for (Map.Entry<Integer, String> en : new TreeMap<>(Axml.fwAttrs()).entrySet()) fa.append(hid(en.getKey())).append('=').append(en.getValue()).append('\n');
        write("fwattrs.properties", fa.toString());
        icon();
        // reflect-config.json (parity with apk2bundle; lists the custom View classes the inflater instantiates)
        StringBuilder rc = new StringBuilder("[\n");
        for (String c : customViews) rc.append(" {\n  \"name\": ").append(js(c)).append(",\n  \"methods\": [\n   {\n    \"name\": \"<init>\",\n    \"parameterTypes\": [\n     \"android.content.Context\",\n     \"android.util.AttributeSet\"\n    ]\n   }\n  ]\n },\n");
        rc.append(" {\n  \"name\": ").append(js(props.getProperty("mainActivity"))).append(",\n  \"methods\": [\n   {\n    \"name\": \"<init>\",\n    \"parameterTypes\": []\n   }\n  ]\n }\n]");
        write("reflect-config.json", rc.toString());
        for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
            ZipEntry ze = en.nextElement();
            if (ze.getName().startsWith("assets/") && !ze.getName().endsWith("/") && !ze.getName().contains("..")) write(ze.getName(), read(ze.getName()));
        }
        return props;
    }

    static String js(String s) { StringBuilder b = new StringBuilder("\""); for (char c : s.toCharArray()) { if (c == '"' || c == '\\') b.append('\\'); if (c < 0x20 || c > 0x7e) b.append(String.format("\\u%04x", (int) c)); else b.append(c); } return b.append('"').toString(); }
    static void link(File l, File target) {
        try { if (!Files.exists(l.toPath(), LinkOption.NOFOLLOW_LINKS)) Files.createSymbolicLink(l.toPath(), target.getAbsoluteFile().toPath()); } catch (IOException ignore) {}
    }
    static String ext(String path) { String f = path.substring(path.lastIndexOf('/') + 1); int d = f.lastIndexOf('.'); return d <= 0 ? "" : f.substring(d); }
    static boolean fileLike(String k) {
        if (k == null) return false;
        if (k.contains("/")) return true;
        for (String s : new String[]{".tgv", ".json", ".glsl", ".xml", ".bin", ".png", ".jpg", ".webp", ".ttf", ".otf"}) if (k.endsWith(s)) return true;
        return false;
    }
    static String plural(int k) {
        switch (k) { case 0x01000005: return "zero"; case 0x01000006: return "one"; case 0x01000007: return "two"; case 0x01000008: return "few"; case 0x01000009: return "many"; default: return "other"; }
    }
    static final Pattern ITEM = Pattern.compile("<item ([^>]*)/?>"), ACOLOR = Pattern.compile("android:color=\"([^\"]+)\"");
    static String cslDefault(String x, String dflt) {
        List<String> items = new ArrayList<>(), plain = new ArrayList<>();
        Matcher m = ITEM.matcher(x); while (m.find()) items.add(m.group(1));
        for (String it : items) if (!it.contains("state_")) plain.add(it);
        if (plain.isEmpty()) plain = items;
        if (!plain.isEmpty()) { Matcher c = ACOLOR.matcher(plain.get(plain.size() - 1)); if (c.find()) dflt = c.group(1); }
        return dflt;
    }

    // ---- XML drawables (apk2bundle.drawable_xml) ----
    static Map<String, String> shapeAttrs(Element sh, String pre) {
        LinkedHashMap<String, String> d = new LinkedHashMap<>();
        put(d, "shape", sh.getAttributeNodeNS(A, "shape"));
        Element so = child(sh, "solid"); if (so != null) put(d, "solid", so.getAttributeNodeNS(A, "color"));
        Element co = child(sh, "corners");
        if (co != null) for (String k : new String[]{"radius", "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius"}) put(d, k, co.getAttributeNodeNS(A, k));
        Element st = child(sh, "stroke");
        if (st != null) for (String k : new String[]{"width", "color", "dashWidth", "dashGap"}) put(d, "stroke_" + k, st.getAttributeNodeNS(A, k));
        Element gr = child(sh, "gradient");
        if (gr != null) for (String k : new String[]{"startColor", "centerColor", "endColor", "angle", "type", "gradientRadius", "centerX", "centerY"}) put(d, "g_" + k, gr.getAttributeNodeNS(A, k));
        Element sz = child(sh, "size");
        if (sz != null) for (String k : new String[]{"width", "height"}) put(d, "size_" + k, sz.getAttributeNodeNS(A, k));
        for (String k : new String[]{"innerRadius", "thickness", "innerRadiusRatio", "thicknessRatio"}) put(d, k, sh.getAttributeNodeNS(A, k));
        LinkedHashMap<String, String> r = new LinkedHashMap<>(); for (Map.Entry<String, String> e : d.entrySet()) r.put(pre + e.getKey(), e.getValue()); return r;
    }
    static void put(Map<String, String> d, String k, Attr a) { if (a != null && !a.getValue().isEmpty()) d.put(k, a.getValue()); }
    static Element child(Element e, String tag) { for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element && tag.equals(((Element) n).getTagName())) return (Element) n; return null; }
    static List<Element> children(Element e, String tag) { List<Element> l = new ArrayList<>(); for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element && (tag == null || tag.equals(((Element) n).getTagName()))) l.add((Element) n); return l; }
    static String attrs(Map<String, String> m) { StringBuilder sb = new StringBuilder(); for (Map.Entry<String, String> e : m.entrySet()) { if (sb.length() > 0) sb.append(' '); sb.append(e.getKey()).append('=').append(q(e.getValue())); } return sb.toString(); }

    static final Pattern ROOT = Pattern.compile("<([\\w-]+)[\\s>]");
    /** @return {kindAttrs, body} */
    String[] drawableXml(String path, String dpre) throws Exception {
        String x = Axml.toXml(read(path));
        int st = x.contains("?>") ? x.indexOf("?>") + 2 : 0;
        Matcher rm = ROOT.matcher(x.substring(Math.max(0, x.indexOf('<', st))));
        String root = rm.find() ? rm.group(1) : "";
        if (root.equals("inset")) {
            Element el; try { el = dom(x).getDocumentElement(); } catch (Exception e) { return new String[]{"kind=\"unsupported\"", ""}; }
            LinkedHashMap<String, String> at = new LinkedHashMap<>();
            String ins = el.hasAttributeNS(A, "inset") ? el.getAttributeNS(A, "inset") : null;
            String[][] ks = {{"left", "insetLeft"}, {"top", "insetTop"}, {"right", "insetRight"}, {"bottom", "insetBottom"}};
            for (String[] k : ks) { String v = el.hasAttributeNS(A, k[1]) ? el.getAttributeNS(A, k[1]) : ins; if (v != null && !v.isEmpty()) at.put(k[0], v); }
            if (el.hasAttributeNS(A, "drawable") && !el.getAttributeNS(A, "drawable").isEmpty()) at.put("drawable", el.getAttributeNS(A, "drawable"));
            else { Element sh = child(el, "shape"); if (sh == null) return new String[]{"kind=\"unsupported\"", ""}; at.putAll(shapeAttrs(sh, "s.")); at.put("inline", "shape"); }
            return new String[]{"kind=\"layer-list\"", "<layer " + attrs(at) + "/>"};
        }
        if (root.equals("ripple")) {
            x = x.replaceAll("<item[^>]*android:id=\"@(android:id/mask|0102002[eE])\"[^>]*/>", "");
            x = Pattern.compile("<item[^>]*android:id=\"@(android:id/mask|0102002[eE])\"[^>]*>.*?</item>", Pattern.DOTALL).matcher(x).replaceAll("");
            root = "layer-list";
        }
        if (root.equals("layer-list")) {
            Element el; try { el = dom(x).getDocumentElement(); } catch (Exception e) { return new String[]{"kind=\"unsupported\"", ""}; }
            StringBuilder items = new StringBuilder(); int ni = 0;
            for (Element it : children(el, "item")) {
                LinkedHashMap<String, String> at = new LinkedHashMap<>();
                for (String k : new String[]{"id", "drawable", "left", "top", "right", "bottom", "start", "end", "width", "height", "gravity"})
                    if (it.hasAttributeNS(A, k) && !it.getAttributeNS(A, k).isEmpty()) at.put(k, it.getAttributeNS(A, k));
                if (!at.containsKey("drawable")) {
                    Element sh = child(it, "shape"); List<Element> ks = children(it, null);
                    if (sh != null) { at.putAll(shapeAttrs(sh, "s.")); at.put("inline", "shape"); }
                    else if (!ks.isEmpty() && ks.get(0).getTagName().equals("vector")) {
                        String fn = dpre + path.replace("/", "_") + "_l" + ni + ".xml";
                        write(fn, serialize(ks.get(0)));
                        at.put("vector", fn);
                    }
                }
                items.append("<layer ").append(attrs(at)).append("/>"); ni++;
            }
            return new String[]{"kind=\"layer-list\"", items.toString()};
        }
        if (root.equals("shape")) {
            try { return new String[]{"kind=\"shape\" " + attrs(shapeAttrs(dom(x).getDocumentElement(), "")), ""}; } catch (Exception ignore) {}
        }
        if (x.contains("<selector")) {
            StringBuilder items = new StringBuilder();
            Matcher m = Pattern.compile("<item ([^>]*)/>").matcher(x);
            Pattern ap = Pattern.compile("android:([\\w_]+)=\"([^\"]*)\"");
            while (m.find()) {
                LinkedHashMap<String, String> a = new LinkedHashMap<>();
                Matcher am = ap.matcher(m.group(1)); while (am.find()) a.put(am.group(1), am.group(2));
                StringBuilder states = new StringBuilder();
                for (Map.Entry<String, String> e : a.entrySet())
                    if (e.getKey().startsWith("state_") && (e.getValue().equals("true") || e.getValue().equals("false"))) {
                        if (states.length() > 0) states.append(','); states.append(e.getValue().equals("true") ? e.getKey() : "!" + e.getKey());
                    }
                String dr = a.getOrDefault("drawable", "");
                items.append("<state when=").append(q(states.toString())).append(" drawable=").append(q(dr)).append("/>");
            }
            return new String[]{"kind=\"selector\"", items.toString()};
        }
        if (x.contains("<vector")) {
            String fn = dpre + path.replace("/", "_");
            write(fn, x);
            return new String[]{"kind=\"vector\" file=" + q(fn), ""};
        }
        Matcher m = Pattern.compile("<shape[^>]*>.*?<solid android:color=\"([^\"]+)\"", Pattern.DOTALL).matcher(x);
        if (m.find()) return new String[]{"kind=\"color\" value=" + q(m.group(1)), ""};
        return new String[]{"kind=\"unsupported\"", ""};
    }

    /** Element -> XML text with android: prefixes (the shim reads attributes by their android:* qualified names). */
    static String serialize(Element e) {
        StringBuilder sb = new StringBuilder(); ser(e, sb, true); return sb.toString();
    }
    static void ser(Element e, StringBuilder sb, boolean top) {
        sb.append('<').append(e.getTagName());
        if (top) sb.append(" xmlns:android=\"").append(A).append('"');
        NamedNodeMap at = e.getAttributes();
        for (int i = 0; i < at.getLength(); i++) {
            Attr a = (Attr) at.item(i);
            if (a.getName().startsWith("xmlns")) continue;
            String nm = A.equals(a.getNamespaceURI()) ? "android:" + a.getLocalName() : a.getName();
            sb.append(' ').append(nm).append("=").append(q(a.getValue()));
        }
        List<Element> ks = children(e, null);
        if (ks.isEmpty()) { sb.append("/>"); return; }
        sb.append('>'); for (Element k : ks) ser(k, sb, false); sb.append("</").append(e.getTagName()).append('>');
    }

    // ---- manifest ----
    String an(Element e, String n) { return e.hasAttributeNS(A, n) ? e.getAttributeNS(A, n) : (e.hasAttribute(n) ? e.getAttribute(n) : null); }
    String fmtName(String v) { if (v != null && !pkg.isEmpty()) { int d = v.indexOf('.'); if (d == 0) v = pkg + v; else if (d == -1) v = pkg + "." + v; } return v; }
    List<Element> tags(String t) { List<Element> l = new ArrayList<>(); NodeList nl = manifest.getElementsByTagName(t); for (int i = 0; i < nl.getLength(); i++) l.add((Element) nl.item(i)); return l; }
    String firstAttr(String tag, String attr) { for (Element e : tags(tag)) { String v = an(e, attr); if (v != null) return v; } return null; }

    String mainActivity() {
        Set<String> x = new HashSet<>(), y = new HashSet<>();
        List<Element> acts = new ArrayList<>(tags("activity")); acts.addAll(tags("activity-alias"));
        for (Element it : acts) {
            if ("false".equals(an(it, "enabled"))) continue;
            String nm = an(it, "name");
            NodeList as = it.getElementsByTagName("action");
            for (int i = 0; i < as.getLength(); i++) if ("android.intent.action.MAIN".equals(an((Element) as.item(i), "name")) && nm != null) x.add(nm);
            NodeList cs = it.getElementsByTagName("category");
            for (int i = 0; i < cs.getLength(); i++) if ("android.intent.category.LAUNCHER".equals(an((Element) cs.item(i), "name")) && nm != null) y.add(nm);
        }
        x.retainAll(y);
        if (x.isEmpty()) return null;
        if (x.size() == 1) return fmtName(x.iterator().next());
        TreeSet<String> mains = new TreeSet<>(); for (String s : x) mains.add(fmtName(s));
        Set<String> actNames = new HashSet<>(); for (Element e : tags("activity")) { String v = an(e, "name"); if (v != null) actNames.add(fmtName(v)); }
        for (String s : mains) if (actNames.contains(s)) return s;
        return mains.first();
    }

    String resolveString(String v) {
        if (v == null || !v.startsWith("@")) return v;
        try {
            String h = v.substring(1); if (h.startsWith("android:")) return v;
            int id = (int) Long.parseLong(h, 16);
            for (int hop = 0; hop < 8; hop++) {
                Arsc.Entry e = null;
                for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(id)) if (c.getKey().versionOnly() && c.getKey().sdkVersion == 0) { e = c.getValue(); break; }
                if (e == null) for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(id)) { e = c.getValue(); break; }
                if (e == null || e.complex) return v;
                if (e.type == ResChunks.TYPE_REFERENCE) { id = e.data; continue; }
                return e.fmt();
            }
        } catch (Exception ignore) {}
        return v;
    }

    Properties manifestProps() throws IOException {
        LinkedHashMap<String, String> p = new LinkedHashMap<>();
        Element root = manifest.getDocumentElement();
        String label = firstAttr("application", "label");
        String main = mainActivity();
        if (label == null && main != null) for (Element a : tags("activity")) if (main.equals(an(a, "name"))) { label = an(a, "label"); break; }
        label = label == null ? "" : resolveString(label);
        String mainAct = main;
        for (Element al : tags("activity-alias")) {
            String nm = an(al, "name"), tg = an(al, "targetActivity");
            if (nm != null && fmtName(nm).equals(main) && tg != null) { mainAct = fmtName(tg); break; }
        }
        p.put("package", pkg); p.put("label", label); p.put("mainActivity", String.valueOf(mainAct));
        p.put("versionName", String.valueOf(an(root, "versionName"))); p.put("versionCode", String.valueOf(an(root, "versionCode")));
        p.put("minSdk", String.valueOf(firstAttr("uses-sdk", "minSdkVersion"))); p.put("targetSdk", String.valueOf(firstAttr("uses-sdk", "targetSdkVersion")));
        List<Element> apps = tags("application"); Element appl = apps.isEmpty() ? null : apps.get(0);
        p.put("appTheme", appl != null && an(appl, "theme") != null ? an(appl, "theme") : "");
        String anm = appl != null && an(appl, "name") != null ? an(appl, "name") : "";
        if (anm.startsWith(".")) anm = pkg + anm; else if (!anm.isEmpty() && !anm.contains(".")) anm = pkg + "." + anm;
        p.put("application", anm);
        String ma = String.valueOf(mainAct);
        for (Element a : tags("activity")) {
            String n = an(a, "name") == null ? "" : an(a, "name");
            if (n.equals(ma) || stripDots(n).equals(ma.replace(pkg + ".", ""))) p.put("mainActivityTheme", an(a, "theme") == null ? "" : an(a, "theme"));
        }
        for (Element a : tags("activity")) {
            String n = an(a, "name") == null ? "" : an(a, "name"); if (n.startsWith(".")) n = pkg + n;
            if (nz(an(a, "theme"))) p.put("theme." + n, an(a, "theme"));
            if (nz(an(a, "screenOrientation"))) p.put("orientation." + n, an(a, "screenOrientation"));
            if (nz(an(a, "configChanges"))) p.put("configChanges." + n, an(a, "configChanges"));
        }
        for (Element al : tags("activity-alias")) {
            String n = an(al, "name") == null ? "" : an(al, "name"), tg = an(al, "targetActivity") == null ? "" : an(al, "targetActivity");
            if (n.startsWith(".")) n = pkg + n; if (tg.startsWith(".")) tg = pkg + tg;
            p.put("alias." + n, tg);
        }
        List<String> provs = new ArrayList<>();
        for (Element pv : tags("provider")) {
            String pn = an(pv, "name") == null ? "" : an(pv, "name");
            if (pn.startsWith(".")) pn = pkg + pn; else if (!pn.isEmpty() && !pn.contains(".")) pn = pkg + "." + pn;
            if (pn.isEmpty() || "false".equals(an(pv, "enabled") == null ? "true" : an(pv, "enabled"))) continue;
            provs.add(pn);
            p.put("provider." + pn + ".authorities", or(an(pv, "authorities"), ""));
            p.put("provider." + pn + ".exported", or(an(pv, "exported"), "false"));
            p.put("provider." + pn + ".grantUriPermissions", or(an(pv, "grantUriPermissions"), "false"));
            for (Element md : desc(pv, "meta-data")) { String k = an(md, "name"); String v = or(an(md, "value"), or(an(md, "resource"), "")); if (nz(k)) p.put("provider." + pn + ".meta." + k, v); }
        }
        p.put("providers", String.join(",", provs));
        for (Element sv : tags("service")) {
            String sn = an(sv, "name") == null ? "" : an(sv, "name");
            if (sn.startsWith(".")) sn = pkg + sn; else if (!sn.isEmpty() && !sn.contains(".")) sn = pkg + "." + sn;
            for (Element md : desc(sv, "meta-data")) { String k = an(md, "name"); String v = or(an(md, "value"), or(an(md, "resource"), "")); if (!sn.isEmpty() && nz(k)) p.put("service." + sn + ".meta." + k, v); }
        }
        for (Element ap : apps) for (Element md : children(ap, "meta-data")) { String k = an(md, "name"); String v = or(an(md, "value"), or(an(md, "resource"), "")); if (nz(k)) p.put("appmeta." + k, v); }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : p.entrySet())
            sb.append(e.getKey().replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=").replace(" ", "\\ ")).append('=').append(e.getValue()).append('\n');
        write("app.properties", sb.toString());
        Properties pr = new Properties(); pr.load(new StringReader(sb.toString())); return pr;
    }
    static String stripDots(String s) { int i = 0; while (i < s.length() && s.charAt(i) == '.') i++; return s.substring(i); }
    static boolean nz(String s) { return s != null && !s.isEmpty(); }
    static String or(String a, String b) { return nz(a) ? a : b; }
    static List<Element> desc(Element e, String tag) { List<Element> l = new ArrayList<>(); NodeList nl = e.getElementsByTagName(tag); for (int i = 0; i < nl.getLength(); i++) l.add((Element) nl.item(i)); return l; }

    void icon() throws IOException {
        String ia = firstAttr("application", "icon");
        if (ia == null || !ia.startsWith("@")) return;
        int id; try { id = (int) Long.parseLong(ia.substring(1), 16); } catch (Exception e) { return; }
        int bd = -1; String bp = null;
        for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(id)) {
            if (c.getValue().complex) continue;
            String p = c.getValue().fmt(); int d = c.getKey().densQualifier();
            if (p.endsWith(".png") && (bp == null || d > bd)) { bd = d; bp = p; }
        }
        if (bp != null) { byte[] d = readOpt(bp); if (d != null) write("icon.png", d); }
    }

    // ---- framework-res (gen_framework_res.py + gen_framework_drawables.py) ----
    /** android.jar (framework resources.arsc + res/) -> fwDir/{values-fw.xml, fwcolor/, drawables-fw.xml, fwdrawable/}. Build-time (CI) step. */
    public static void framework(File androidJar, File fwDir) throws Exception {
        fwDir.mkdirs();
        ApkBundler b = new ApkBundler(androidJar, fwDir, true);
        try { b.fw(); } finally { b.zip.close(); }
    }
    ApkBundler(File jar, File out, boolean fw) throws IOException {
        zip = new ZipFile(jar); this.out = out; manifest = null; pkg = "android";
        arsc = new Arsc(read("resources.arsc"), "android");
    }
    void fw() throws Exception {
        int sdkV = Integer.getInteger("jet.fw.sdk", 28);
        Map<Integer, String> ATTR = Axml.fwAttrs();
        List<Integer> ids = new ArrayList<>(arsc.pub.keySet()); Collections.sort(ids, (x, y) -> Long.compare(x & 0xffffffffL, y & 0xffffffffL));
        Set<String> KEEP = new HashSet<>(Arrays.asList("style", "color", "dimen", "bool", "integer", "string", "fraction"));
        StringBuilder L = new StringBuilder();
        for (int rid : ids) {
            String t = arsc.pub.get(rid)[0], n = arsc.pub.get(rid)[1];
            if (!KEEP.contains(t)) continue;
            Arsc.Entry e = null; int bv = -1;
            for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(rid)) if (c.getKey().versionOnly() && c.getKey().sdkVersion <= sdkV && c.getKey().sdkVersion > bv) { bv = c.getKey().sdkVersion; e = c.getValue(); }
            if (e == null) continue;
            String h = hid(rid);
            try {
                if (t.equals("style")) {
                    if (!e.complex) continue;
                    L.append("<style id=\"").append(h).append("\" name=").append(q(n)).append(" parent=\"").append(hid(e.parent)).append("\">\n");
                    for (int i = 0; i < e.bagKeys.length; i++) { String an = ATTR.get(e.bagKeys[i]); if (an == null) an = hid(e.bagKeys[i]); L.append("  <item name=").append(q(an)).append(" value=").append(q(e.fmtBag(i))).append("/>\n"); }
                    L.append("</style>\n");
                } else if (t.equals("color") && !e.complex && e.fmt().endsWith(".xml")) {
                    String x = null; try { x = Axml.toXml(read(e.fmt())); } catch (Exception ignore) {}
                    String fn = String.format("fwcolor/%08x.xml", rid), dflt = "#FF888888";
                    if (x != null && !x.isEmpty()) { write(fn, x); dflt = cslDefault(x, dflt); } else x = null;
                    L.append("<value id=\"").append(h).append("\" type=\"color\" name=").append(q(n)).append(" value=").append(q(dflt)).append(x != null ? " file=\"" + fn + "\"" : "").append("/>\n");
                } else if (!e.complex) {
                    L.append("<value id=\"").append(h).append("\" type=\"").append(t).append("\" name=").append(q(n)).append(" value=").append(q(e.fmt())).append("/>\n");
                }
            } catch (Exception ex) { System.err.println("fw skip " + h + " " + ex); }
        }
        write("values-fw.xml", L.toString());
        // drawables: no non-version/density qualifiers; version <= JET_SDK; density closest to xhdpi
        StringBuilder D = new StringBuilder();
        for (int rid : ids) {
            if (!arsc.pub.get(rid)[0].equals("drawable")) continue;
            String name = arsc.pub.get(rid)[1], h = hid(rid);
            double[] bs = null; Arsc.Entry be = null; int bdens = 0;
            for (Map.Entry<Arsc.Config, Arsc.Entry> c : arsc.configs(rid)) {
                Arsc.Config k = c.getKey();
                if (!densVersionOnly(k) || k.sdkVersion > JET_SDK) continue;
                int d = k.density == 0 ? 160 : k.densQualifier();
                if (k.density != 0 && d == 160 && k.density != 160) continue;   // unknown density qualifier (e.g. tvdpi) -> not ok, as in python
                double dd = d == 1 ? 1 : (d == 0 ? 0 : (d == 320 ? 0 : (d > 320 ? 1 + (d - 320) / 1000.0 : 2 + (320 - d) / 1000.0)));
                double[] s = {dd, -k.sdkVersion};
                if (bs == null || s[0] < bs[0] || (s[0] == bs[0] && s[1] < bs[1])) { bs = s; be = c.getValue(); bdens = d; }
            }
            if (be == null || be.complex) continue;
            String path = be.fmt();
            if (path.startsWith("#")) { D.append("<drawable id=\"").append(h).append("\" name=").append(q(name)).append(" kind=\"color\" value=").append(q(path)).append("></drawable>\n"); continue; }
            if (path.startsWith("@")) { D.append("<drawable id=\"").append(h).append("\" name=").append(q(name)).append(" kind=\"layer-list\"><layer drawable=").append(q(path)).append("/></drawable>\n"); continue; }
            try {
                if (path.endsWith(".xml")) {
                    String[] kb = drawableXml(path, "fwdrawable/fw_");
                    D.append("<drawable id=\"").append(h).append("\" name=").append(q(name)).append(' ').append(kb[0]).append('>').append(kb[1]).append("</drawable>\n");
                } else {
                    byte[] data = read(path); String ext = path.endsWith(".9.png") ? ".9.png" : ext(path);
                    String fn = String.format("fwdrawable/%08x%s", rid, ext); write(fn, data);
                    D.append("<drawable id=\"").append(h).append("\" name=").append(q(name)).append(" kind=\"").append(ext.equals(".9.png") ? "ninepatch" : "bitmap").append("\" file=\"").append(fn).append("\" density=\"").append(bdens).append("\"/>\n");
                }
            } catch (Exception ex) { System.err.println("fw drawable skip " + h + " " + ex); }
        }
        write("drawables-fw.xml", D.toString());
    }
    static boolean densVersionOnly(Arsc.Config k) {
        byte[] r = k.raw;
        for (int i = 0; i < r.length; i++) { int o = i + 4; if ((o >= 24 && o < 28) || o == 14 || o == 15) continue; if (r[i] != 0) return false; }
        return true;
    }

    public static void main(String[] a) throws Exception {
        if (a.length >= 3 && a[0].equals("--framework")) { framework(new File(a[1]), new File(a[2])); System.out.println("framework-res -> " + a[2]); return; }
        long t0 = System.nanoTime();
        Properties p = bundle(new File(a[0]), new File(a[1]), a.length > 2 ? new File(a[2]) : null);
        System.out.printf("bundle %s (%s) -> %s in %.0f ms%n", p.getProperty("package"), p.getProperty("mainActivity"), a[1], (System.nanoTime() - t0) / 1e6);
    }
}
