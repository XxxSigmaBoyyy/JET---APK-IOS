package jet.apk;

import java.nio.ByteBuffer;
import java.util.*;

/** Binary XML (AXML) -> XML text, mirroring androguard AXMLPrinter + lxml pretty printing as used by jetc/apk2bundle.py
 *  (system attribute names from the resource map, Res_value formatting, android: prefixes). */
public final class Axml {
    private Axml() {}
    static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    static final class Node {
        String ns, name, text; final List<String[]> attrs = new ArrayList<>();   // {nsUri, name, value}
        final List<Node> kids = new ArrayList<>(); Map<String, String> nsDecl = new LinkedHashMap<>();   // uri -> prefix
    }

    /** Framework attr id -> name (androguard public.json "attr"), loaded lazily from fwattrs.properties on the classpath or a file. */
    static volatile Map<Integer, String> FW_ATTRS;
    public static void setFrameworkAttrs(Map<Integer, String> m) { FW_ATTRS = m; }
    static Map<Integer, String> fwAttrs() {
        Map<Integer, String> m = FW_ATTRS;
        if (m != null) return m;
        m = new HashMap<>();
        try (java.io.InputStream in = Axml.class.getResourceAsStream("/jet/apk/fwattrs.properties")) {
            if (in != null) {
                Properties p = new Properties(); p.load(in);
                for (String k : p.stringPropertyNames()) m.put((int) Long.parseLong(k.substring(2), 16), p.getProperty(k));
            }
        } catch (Exception ignore) {}
        return FW_ATTRS = m;
    }

    public static String toXml(byte[] data) {
        Node root = parse(data);
        if (root == null) return "";
        StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='utf-8'?>\n");
        Map<String, String> inScope = new HashMap<>();
        write(root, sb, 0, inScope, true);
        return sb.toString();
    }

    static Node parse(byte[] data) {
        ByteBuffer b = ResChunks.le(data);
        if (data.length < 8) return null;
        int p = b.getShort(2) & 0xffff, end = Math.min(data.length, b.getInt(4));
        ResChunks.StringPool sp = null; int[] resMap = new int[0];
        Deque<Node> stack = new ArrayDeque<>(); Node root = null;
        LinkedHashMap<String, String> pending = new LinkedHashMap<>();   // uri -> prefix declared before next element
        Map<String, String> nsmap = new LinkedHashMap<>();                 // prefix -> uri (active)
        while (p + 8 <= end) {
            int type = b.getShort(p) & 0xffff, hsz = b.getShort(p + 2) & 0xffff, size = b.getInt(p + 4);
            if (size < 8) break;
            int ext = p + hsz;
            switch (type) {
                case ResChunks.RES_STRING_POOL: if (sp == null) sp = new ResChunks.StringPool(b, p); break;
                case ResChunks.XML_RESOURCE_MAP: { int n = (size - hsz) / 4; resMap = new int[n]; for (int i = 0; i < n; i++) resMap[i] = b.getInt(ext + 4 * i); break; }
                case ResChunks.XML_START_NS: {
                    String pre = str(sp, b.getInt(ext)), uri = str(sp, b.getInt(ext + 4));
                    nsmap.put(pre, uri); pending.put(uri, pre); break;
                }
                case ResChunks.XML_END_NS: { String pre = str(sp, b.getInt(ext)); nsmap.remove(pre); break; }
                case ResChunks.XML_START_ELEM: {
                    Node n = new Node();
                    n.ns = str(sp, b.getInt(ext)); n.name = str(sp, b.getInt(ext + 4));
                    if (n.name.isEmpty()) break;
                    int aStart = b.getShort(ext + 8) & 0xffff, aSize = b.getShort(ext + 10) & 0xffff, aCount = b.getShort(ext + 12) & 0xffff;
                    for (int i = 0; i < aCount; i++) {
                        int a = ext + aStart + i * aSize;
                        int ansIdx = b.getInt(a), anameIdx = b.getInt(a + 4), raw = b.getInt(a + 8);
                        int vtype = b.get(a + 15) & 0xff, vdata = b.getInt(a + 16);
                        String ans = ansIdx == -1 ? "" : str(sp, ansIdx);
                        String an = str(sp, anameIdx);
                        if (anameIdx >= 0 && anameIdx < resMap.length) {
                            String pub = fwAttrs().get(resMap[anameIdx]);
                            if (pub != null) an = pub;
                        }
                        if (an.isEmpty() || an.equals(":")) an = String.format("android:UNKNOWN_SYSTEM_ATTRIBUTE_%08x", anameIdx >= 0 && anameIdx < resMap.length ? resMap[anameIdx] : 0);
                        // androguard _fix_name: embedded "prefix:" with empty namespace
                        if (ans.isEmpty() && an.indexOf(':') > 0) {
                            String ep = an.substring(0, an.indexOf(':'));
                            if (nsmap.containsKey(ep)) { ans = nsmap.get(ep); an = an.substring(ep.length() + 1); }
                        }
                        if (!Character.isLetter(an.charAt(0)) && an.charAt(0) != '_') an = "_" + an;
                        an = an.replaceAll("[^a-zA-Z0-9._-]", "_");
                        String v = vtype == ResChunks.TYPE_STRING ? (raw == -1 ? str(sp, vdata) : str(sp, raw)) : ResChunks.format(vtype, vdata, null);
                        if (vtype == ResChunks.TYPE_STRING) v = fixValue(v);
                        n.attrs.add(new String[]{ans, an, v});
                    }
                    n.nsDecl.putAll(pending); pending.clear();
                    if (stack.isEmpty()) { if (root == null) root = n; else break; }
                    else stack.peek().kids.add(n);
                    stack.push(n); break;
                }
                case ResChunks.XML_END_ELEM: if (!stack.isEmpty()) stack.pop(); break;
                case ResChunks.XML_CDATA: if (!stack.isEmpty()) stack.peek().text = str(sp, b.getInt(ext)); break;
                default: break;
            }
            p += size;
        }
        return root;
    }

    static String str(ResChunks.StringPool sp, int i) { return sp == null || i < 0 ? "" : sp.get(i); }

    static String fixValue(String v) {
        int z = v.indexOf('\0'); if (z >= 0) v = v.substring(0, z);
        StringBuilder sb = null;
        for (int i = 0; i < v.length(); ) {
            int c = v.codePointAt(i); int n = Character.charCount(c);
            boolean ok = c == 9 || c == 10 || c == 13 || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF);
            if (!ok && sb == null) { sb = new StringBuilder(v.substring(0, i)); }
            if (sb != null) { if (ok) sb.appendCodePoint(c); else sb.append('_'); }
            i += n;
        }
        return sb == null ? v : sb.toString();
    }

    static void write(Node n, StringBuilder sb, int depth, Map<String, String> scope, boolean pretty) {
        Map<String, String> sc = new HashMap<>(scope);
        StringBuilder decl = new StringBuilder();
        for (Map.Entry<String, String> d : n.nsDecl.entrySet()) {
            if (d.getValue().equals(sc.get(d.getKey()))) continue;
            sc.put(d.getKey(), d.getValue());
            decl.append(d.getValue().isEmpty() ? " xmlns" : " xmlns:" + d.getValue()).append("=\"").append(esc(d.getKey(), true)).append('"');
        }
        // attributes / element in a namespace without a declared prefix get ns0, ns1, ... (lxml)
        int[] gen = {0};
        String tag = qname(n.ns, n.name, sc, decl, gen);
        StringBuilder at = new StringBuilder();
        for (String[] a : n.attrs) at.append(' ').append(qname(a[0], a[1], sc, decl, gen)).append("=\"").append(esc(a[2], true)).append('"');
        indent(sb, depth, pretty);
        sb.append('<').append(tag).append(decl).append(at);
        if (n.kids.isEmpty() && n.text == null) { sb.append("/>\n"); return; }
        sb.append('>');
        if (n.text != null) sb.append(esc(n.text, false));
        if (!n.kids.isEmpty()) {
            sb.append('\n');
            for (Node k : n.kids) write(k, sb, depth + 1, sc, pretty);
            indent(sb, depth, pretty);
        }
        sb.append("</").append(tag).append(">\n");
    }

    static String qname(String uri, String name, Map<String, String> sc, StringBuilder decl, int[] gen) {
        if (uri == null || uri.isEmpty()) return name;
        String pre = sc.get(uri);
        if (pre == null) {
            pre = "ns" + gen[0]++; sc.put(uri, pre);
            decl.append(" xmlns:").append(pre).append("=\"").append(esc(uri, true)).append('"');
        }
        return pre.isEmpty() ? name : pre + ":" + name;
    }

    static void indent(StringBuilder sb, int depth, boolean pretty) { if (pretty) for (int i = 0; i < depth; i++) sb.append("  "); }

    static String esc(String s, boolean attr) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': if (attr) sb.append("&quot;"); else sb.append(c); break;
                case '\n': if (attr) sb.append("&#10;"); else sb.append(c); break;
                case '\r': sb.append("&#13;"); break;
                case '\t': if (attr) sb.append("&#9;"); else sb.append(c); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
