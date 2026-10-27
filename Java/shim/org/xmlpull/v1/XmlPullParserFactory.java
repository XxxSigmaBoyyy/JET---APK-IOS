package org.xmlpull.v1;
public class XmlPullParserFactory {
    private boolean ns;
    protected XmlPullParserFactory() {}
    public static XmlPullParserFactory newInstance() { return new XmlPullParserFactory(); }
    public static XmlPullParserFactory newInstance(String classNames, Class<?> context) { return new XmlPullParserFactory(); }
    public XmlPullParser newPullParser() { jet.runtime.DomPullParser p = new jet.runtime.DomPullParser(); p.setNamespaceAware(ns); return p; }
    public void setNamespaceAware(boolean a) { ns = a; } public boolean isNamespaceAware() { return ns; }
    public void setValidating(boolean v) {} public boolean isValidating() { return false; }
    public void setFeature(String n, boolean s) { if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(n)) ns = s; } public boolean getFeature(String n) { return XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(n) && ns; }
    public XmlSerializer newSerializer() { throw new UnsupportedOperationException("JET: XmlSerializer not shimmed yet"); }
}
