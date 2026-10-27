package android.util;
/** Same shape as Android's interface; JET's implementation is jet.runtime.XmlAttrs (defaults derive from getAttributeValue). */
public interface AttributeSet {
    int getAttributeCount();
    String getAttributeName(int index);
    String getAttributeValue(int index);
    String getAttributeValue(String namespace, String name);
    default String getAttributeNamespace(int index) { return "http://schemas.android.com/apk/res/android"; }
    default String getPositionDescription() { return "<jet>"; }
    default int getAttributeNameResource(int index) { return 0; }
    default int getAttributeListValue(String ns, String a, String[] opts, int def) { String v = getAttributeValue(ns, a); if (v != null) for (int i = 0; i < opts.length; i++) if (opts[i].equals(v)) return i; return def; }
    default boolean getAttributeBooleanValue(String ns, String a, boolean def) { String v = getAttributeValue(ns, a); return v == null ? def : "true".equals(v) || "-1".equals(v) || "1".equals(v); }
    default int getAttributeResourceValue(String ns, String a, int def) { int r = jet.runtime.ResTable.refId(getAttributeValue(ns, a)); return r == 0 ? def : r; }
    default int getAttributeIntValue(String ns, String a, int def) { String v = getAttributeValue(ns, a); if (v == null) return def; try { return jet.runtime.ResTable.intVal(v); } catch (RuntimeException e) { return def; } }
    default int getAttributeUnsignedIntValue(String ns, String a, int def) { return getAttributeIntValue(ns, a, def); }
    default float getAttributeFloatValue(String ns, String a, float def) { String v = getAttributeValue(ns, a); if (v == null) return def; try { return Float.parseFloat(v); } catch (RuntimeException e) { return def; } }
    default int getAttributeListValue(int i, String[] opts, int def) { String v = getAttributeValue(i); if (v != null) for (int k = 0; k < opts.length; k++) if (opts[k].equals(v)) return k; return def; }
    default boolean getAttributeBooleanValue(int i, boolean def) { String v = getAttributeValue(i); return v == null ? def : "true".equals(v) || "-1".equals(v); }
    default int getAttributeResourceValue(int i, int def) { int r = jet.runtime.ResTable.refId(getAttributeValue(i)); return r == 0 ? def : r; }
    default int getAttributeIntValue(int i, int def) { try { return jet.runtime.ResTable.intVal(getAttributeValue(i)); } catch (RuntimeException e) { return def; } }
    default int getAttributeUnsignedIntValue(int i, int def) { return getAttributeIntValue(i, def); }
    default float getAttributeFloatValue(int i, float def) { try { return Float.parseFloat(getAttributeValue(i)); } catch (RuntimeException e) { return def; } }
    default String getIdAttribute() { return getAttributeValue(null, "id"); }
    default String getClassAttribute() { return getAttributeValue(null, "class"); }
    default int getIdAttributeResourceValue(int def) { return getAttributeResourceValue(null, "id", def); }
    default int getStyleAttribute() { return getAttributeResourceValue(null, "style", 0); }
}
