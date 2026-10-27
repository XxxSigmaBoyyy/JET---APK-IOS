package android.util;
public class Xml {
    public static final String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
    /** Android: the parser itself if it is an AttributeSet (XmlResourceParser / JET DomPullParser), else a live view of its current tag. */
    public static AttributeSet asAttributeSet(final org.xmlpull.v1.XmlPullParser p) {
        if (p instanceof AttributeSet) return (AttributeSet) p;
        return new AttributeSet() {
            public int getAttributeCount() { return p.getAttributeCount(); }
            public String getAttributeName(int i) { return p.getAttributeName(i); }
            public String getAttributeValue(int i) { return p.getAttributeValue(i); }
            public String getAttributeValue(String ns, String name) { return p.getAttributeValue(ns, name); }
            public String getAttributeNamespace(int i) { return p.getAttributeNamespace(i); }
            public String getPositionDescription() { return p.getPositionDescription(); }
        };
    }
    public static org.xmlpull.v1.XmlPullParser newPullParser() { try { org.xmlpull.v1.XmlPullParserFactory f = org.xmlpull.v1.XmlPullParserFactory.newInstance(); f.setNamespaceAware(true); return f.newPullParser(); } catch (Exception e) { throw new RuntimeException(e); } }
}
