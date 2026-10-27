package jet.runtime;
import org.w3c.dom.*;
import org.xmlpull.v1.*;
import java.util.*;
/** XmlPullParser / XmlResourceParser over a parsed DOM (decoded resource XML or setInput stream). */
public final class DomPullParser implements android.content.res.XmlResourceParser {
    private static final class Ev { final int type; final Node node; final int depth; Ev(int t, Node n, int d) { type = t; node = n; depth = d; } }
    private final ArrayList<Ev> evs = new ArrayList<>(); private int pos = -1; private final ResTable table; private boolean nsAware;
    public DomPullParser() { table = null; }
    public DomPullParser(Element root, ResTable t) { table = t; load(root); }
    public void setNamespaceAware(boolean b) { nsAware = b; }
    private void load(Element root) { evs.clear(); evs.add(new Ev(START_DOCUMENT, null, 0)); walk(root, 1); evs.add(new Ev(END_DOCUMENT, null, 0)); pos = 0; }
    private void walk(Node n, int d) {
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            evs.add(new Ev(START_TAG, n, d));
            for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) walk(c, d + 1);
            evs.add(new Ev(END_TAG, n, d));
        } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) evs.add(new Ev(TEXT, n, d - 1));
    }
    private void parse(org.xml.sax.InputSource src) throws XmlPullParserException {
        try { javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance(); f.setNamespaceAware(nsAware);
            load(f.newDocumentBuilder().parse(src).getDocumentElement()); } catch (Exception e) { throw new XmlPullParserException(e.toString()); }
    }
    public void setInput(java.io.Reader in) throws XmlPullParserException { parse(new org.xml.sax.InputSource(in)); }
    public void setInput(java.io.InputStream in, String enc) throws XmlPullParserException { org.xml.sax.InputSource s = new org.xml.sax.InputSource(in); if (enc != null) s.setEncoding(enc); parse(s); }
    private Ev cur() { return evs.get(Math.max(0, Math.min(pos, evs.size() - 1))); }
    public int getEventType() { return cur().type; }
    public int next() throws XmlPullParserException { if (pos < 0) throw new XmlPullParserException("no input"); if (pos < evs.size() - 1) pos++; return cur().type; }
    public int nextToken() throws XmlPullParserException { return next(); }
    public int nextTag() throws XmlPullParserException { int t = next(); if (t == TEXT && isWhitespace()) t = next(); if (t != START_TAG && t != END_TAG) throw new XmlPullParserException("expected start or end tag, got " + TYPES[t]); return t; }
    public String nextText() throws XmlPullParserException { if (getEventType() != START_TAG) throw new XmlPullParserException("not START_TAG"); StringBuilder b = new StringBuilder(); int t; while ((t = next()) == TEXT) b.append(getText()); if (t != END_TAG) throw new XmlPullParserException("expected END_TAG"); return b.toString(); }
    public void require(int type, String ns, String name) throws XmlPullParserException { if (type != getEventType() || (name != null && !name.equals(getName()))) throw new XmlPullParserException("expected " + TYPES[type] + " " + name + " got " + TYPES[getEventType()] + " " + getName()); }
    public int getDepth() { Ev e = cur(); return e.type == START_TAG || e.type == END_TAG ? e.depth : e.type == TEXT ? e.depth : 0; }
    private static String local(String n) { int c = n.indexOf(':'); return c >= 0 ? n.substring(c + 1) : n; }
    public String getName() { Ev e = cur(); return (e.type == START_TAG || e.type == END_TAG) ? (nsAware && e.node.getLocalName() != null ? e.node.getLocalName() : e.node.getNodeName()) : null; }
    public String getText() { Ev e = cur(); return e.type == TEXT ? e.node.getNodeValue() : null; }
    public char[] getTextCharacters(int[] h) { String t = getText(); if (t == null) return null; h[0] = 0; h[1] = t.length(); return t.toCharArray(); }
    public boolean isWhitespace() { String t = getText(); return t != null && t.trim().isEmpty(); }
    public String getNamespace() { Ev e = cur(); return e.node != null && e.node.getNamespaceURI() != null ? e.node.getNamespaceURI() : ""; }
    public String getPrefix() { Ev e = cur(); return e.node == null ? null : e.node.getPrefix(); }
    public boolean isEmptyElementTag() { Ev e = cur(); return e.type == START_TAG && !e.node.hasChildNodes(); }
    private List<Attr> attrs() { ArrayList<Attr> l = new ArrayList<>(); Ev e = cur(); if (e.type != START_TAG) return l; NamedNodeMap m = e.node.getAttributes();
        for (int i = 0; i < m.getLength(); i++) { Attr a = (Attr) m.item(i); if (!a.getName().startsWith("xmlns")) l.add(a); } return l; }
    public int getAttributeCount() { return cur().type == START_TAG ? attrs().size() : -1; }
    public String getAttributeName(int i) { return local(attrs().get(i).getName()); }
    public String getAttributeValue(int i) { return attrs().get(i).getValue(); }
    public String getAttributePrefix(int i) { String n = attrs().get(i).getName(); int c = n.indexOf(':'); return c >= 0 ? n.substring(0, c) : null; }
    public String getAttributeNamespace(int i) { Attr a = attrs().get(i); if (a.getNamespaceURI() != null) return a.getNamespaceURI(); String p = getAttributePrefix(i); return p == null ? "" : "android".equals(p) ? "http://schemas.android.com/apk/res/android" : "http://schemas.android.com/apk/res-auto"; }
    public String getAttributeType(int i) { return "CDATA"; } public boolean isAttributeDefault(int i) { return false; }
    public String getAttributeValue(String ns, String name) {
        String hit = null;
        for (Attr a : attrs()) {
            if (!local(a.getName()).equals(name)) continue;
            boolean androidPrefixed = a.getName().startsWith("android:");
            String aNs = a.getNamespaceURI();
            boolean androidNs = androidPrefixed || (aNs != null && aNs.endsWith("/android"));
            boolean wantAndroid = ns != null && ns.endsWith("/android");
            if (ns == null || ns.isEmpty()) {
                if (!a.getName().contains(":") || hit == null) return a.getValue();
            } else if (androidNs == wantAndroid || (aNs != null && aNs.equals(ns))) {
                return a.getValue();
            }
            if (hit == null) hit = a.getValue();
        }
        // Namespace-aware DOM may store android attrs as local-name + URI with no "android:" prefix.
        // Fall back to first local-name match so Compose vector fillType="1" is visible to hasAttribute.
        return hit;
    }
    /** Bridge for TypedArray/View: the current START_TAG as JET XmlAttrs. */
    public XmlAttrs jetAttrs() { Ev e = cur(); return e.type == START_TAG && table != null ? new XmlAttrs((Element) e.node, table) : null; }
    public String getPositionDescription() { return TYPES[getEventType()] + " " + getName() + " @" + pos; }
    public int getLineNumber() { return -1; } public int getColumnNumber() { return -1; }
    public void setFeature(String n, boolean s) { if (FEATURE_PROCESS_NAMESPACES.equals(n)) nsAware = s; } public boolean getFeature(String n) { return FEATURE_PROCESS_NAMESPACES.equals(n) && nsAware; }
    public void setProperty(String n, Object v) {} public Object getProperty(String n) { return null; } public String getInputEncoding() { return "UTF-8"; }
    public void defineEntityReplacementText(String a, String b) {}
    public int getNamespaceCount(int d) { return 0; } public String getNamespacePrefix(int p) { return null; } public String getNamespaceUri(int p) { return null; } public String getNamespace(String prefix) { return null; }
    public void close() {}
}
