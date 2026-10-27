package android.content.res;
import android.util.TypedValue;
import jet.runtime.ResTable;
/** Attribute values resolved from (AttributeSet, theme). */
public class TypedArray {
    private final Resources res;
    private final int[] attrs;
    private final String[] vals;
    public TypedArray(Resources res, int[] attrs) { this(res, attrs, null, null); }
    private TypedArray(Resources res, String[] raw) { this.res = res; this.attrs = new int[raw.length]; this.vals = raw; }
    /** obtainTypedArray(): index i is array item i. */
    public static TypedArray jetFromValues(Resources res, String[] raw) { return new TypedArray(res, raw); }
    public TypedArray(Resources res, int[] attrs, Resources.Theme theme, android.util.AttributeSet set) {
        this.res = res; this.attrs = attrs == null ? new int[0] : attrs; this.mTheme = theme;
        vals = new String[this.attrs.length];
        for (int i = 0; i < this.attrs.length; i++) {
            String v = null;
            if (jet.runtime.XmlAttrs.of(set) != null && res != null && res.table() != null) {
                jet.runtime.XmlAttrs x = jet.runtime.XmlAttrs.of(set);
                ResTable t = res.table(); int a = this.attrs[i];
                String key = t.attrKey(a);
                v = x.get(key);
                if (v == null && (a >>> 24) != 0x01) { String n = t.name(a); if (n != null) { v = x.get("app:" + n); if (v == null) v = x.get(n); } }
            }
            Resources.Theme th = theme != null ? theme : Resources.jetLastTheme;
            if (v == null && th != null) v = th.raw(this.attrs[i]);
            else if (v != null && v.startsWith("?") && th != null) {
                String r = th.raw(ResTable.refId(v));
                if (r != null) v = r;
            }
            if (v != null && (v.startsWith("<") || "@0".equals(v) || "@null".equals(v))) v = null;   // TYPE_NULL (@null/@empty)
            vals[i] = v;
        }
    }
    private ResTable t() { return res == null ? null : res.table(); }
    private String v(int i) { return i >= 0 && i < vals.length ? vals[i] : null; }
    private String prim(int i) {
        String s = v(i); ResTable t = t();
        if (s != null && s.startsWith("?")) {
            Resources.Theme th = mTheme != null ? mTheme : Resources.jetLastTheme;
            if (th != null) { String r = th.raw(ResTable.refId(s)); if (r != null) s = r; }
        }
        if (s != null && s.startsWith("@") && t != null) { String r = t.resolve("@" + Integer.toHexString(ResTable.refId(s))); if (r != null && !r.startsWith("@")) return r; }
        return s;
    }
    public int length() { return attrs.length; }
    public int getIndexCount() { int n = 0; for (String s : vals) if (s != null) n++; return n; }
    public int getIndex(int at) { int n = 0; for (int i = 0; i < vals.length; i++) if (vals[i] != null && n++ == at) return i; return -1; }
    public Resources getResources() { return res; }
    public boolean hasValue(int index) { return v(index) != null; }
    public boolean hasValueOrEmpty(int index) { return hasValue(index); }
    public boolean getBoolean(int index, boolean def) { String s = prim(index); return s == null ? def : ("true".equals(s) || "1".equals(s) || (s.startsWith("0x") && !"0x0".equals(s) && !s.matches("0x0+"))); }
    public int getInt(int index, int def) {
        String s = prim(index); if (s == null) return def;
        try {
            if (s.startsWith("#")) return ResTable.parseColor(s);            // TYPE_INT_COLOR_*: data is the ARGB value
            if (s.equals("true")) return 1; if (s.equals("false")) return 0; // TYPE_INT_BOOLEAN
            if (s.startsWith("@")) { int id = ResTable.refId(s); if (id != 0 && res != null && "color".equals(res.getResourceTypeName(id))) return res.getColor(id, mTheme); }
            return ResTable.intVal(s);
        } catch (Exception e) { return def; }
    }
    public int getInteger(int index, int def) { return getInt(index, def); }
    public float getFloat(int index, float def) { String s = prim(index); try { return s == null ? def : Float.parseFloat(s); } catch (Exception e) { return def; } }
    public int getColor(int index, int def) {
        String s = prim(index); if (s == null) return def;
        try {
            if (s.startsWith("#")) return ResTable.parseColor(s);
            if (s.startsWith("?")) {
                Resources.Theme th = mTheme != null ? mTheme : Resources.jetLastTheme;
                if (th != null) { String r = th.raw(ResTable.refId(s)); if (r != null && r.startsWith("#")) return ResTable.parseColor(r);
                    if (r != null) { s = r; } }
            }
            int id = ResTable.refId(s);
            // Attr ids are not colours — resolving them via getColor yields garbage / AndroidX magenta (-65281).
            if (id != 0 && res != null && !"attr".equals(res.getResourceTypeName(id))) return res.getColor(id, mTheme != null ? mTheme : Resources.jetLastTheme);
        } catch (Exception ignored) {}
        return def;
    }
    private Resources.Theme mTheme;
    public ColorStateList getColorStateList(int index) {
        String s = v(index); if (s == null) return null;
        int id = s.startsWith("@") ? ResTable.refId(s) : 0;
        if (id != 0 && res != null && t().hasXmlFile(id)) return res.getColorStateList(id, mTheme);
        return ColorStateList.valueOf(getColor(index, 0));
    }
    public static final boolean DBG = System.getProperty("jet.debugTA") != null;
    public int getResourceId(int index, int def) { int id = ResTable.refId(v(index));
        if (DBG && id == 0) { ResTable t = t(); int a = index >= 0 && index < attrs.length ? attrs[index] : 0; System.err.println("[TA] getResourceId miss attr=" + Integer.toHexString(a) + " " + (t != null ? t.name(a) : "?") + " raw=" + v(index) + " theme=" + (mTheme != null)); }
        return id != 0 ? id : def; }
    public int getSourceResourceId(int index, int def) { return def; }
    private float dim(int index, float def) { String s = prim(index); ResTable t = t(); if (s == null || t == null) return def; try { return t.dimen(s); } catch (Exception e) { return def; } }
    public int getLayoutDimension(int index, int def) { return (int) dim(index, def); }
    public int getLayoutDimension(int index, String name) { return (int) dim(index, -2); }
    public int getDimensionPixelSize(int index, int def) { float f = dim(index, Float.NaN); return Float.isNaN(f) ? def : Math.round(f); }
    public int getDimensionPixelOffset(int index, int def) { float f = dim(index, Float.NaN); return Float.isNaN(f) ? def : (int) f; }
    public float getDimension(int index, float def) { return dim(index, def); }
    public float getFraction(int index, int base, int pbase, float def) { return def; }
    public String getString(int index) { String s = prim(index); return s == null || s.startsWith("@") ? null : s; }
    /** AOSP: XML position of the source of this array ("Binary XML file line #N"); used in error messages. */
    public String getPositionDescription() { return "<jet TypedArray>"; }
    public String getNonResourceString(int index) { return getString(index); }
    public CharSequence getText(int index) { return getString(index); }
    public CharSequence[] getTextArray(int index) { int id = getResourceId(index, 0); if (id == 0 || res == null) return null; try { return res.getTextArray(id); } catch (RuntimeException e) { return null; } }
    public android.graphics.drawable.Drawable getDrawable(int index) {
        String s = v(index); ResTable t = t(); if (s == null || t == null) return null;
        try { return t.drawable(s.startsWith("@") ? "@" + Integer.toHexString(ResTable.refId(s)) : s); } catch (Throwable e) { return null; }
    }
    public int getType(int index) { String s = v(index); if (s == null) return TypedValue.TYPE_NULL; if (s.startsWith("@")) return TypedValue.TYPE_REFERENCE; if (s.startsWith("#")) return TypedValue.TYPE_INT_COLOR_ARGB8; return TypedValue.TYPE_STRING; }
    public boolean getValue(int index, TypedValue out) {
        String s = v(index); if (s == null) return false;
        int id = ResTable.refId(s);
        out.resourceId = id; out.type = getType(index); out.data = id != 0 ? id : (s.startsWith("#") ? ResTable.parseColor(s) : 0); out.string = s;
        if (id != 0 && t() != null) { // AOSP resolves references: @color/x peeks as TYPE_INT_COLOR_*, bags/files stay references
            try { String r = t().resolve(s); if (r != null && r.startsWith("#")) { out.type = TypedValue.TYPE_INT_COLOR_ARGB8; out.data = ResTable.parseColor(r); out.string = null; } } catch (Throwable ignored) {}
        }
        return true;
    }
    public TypedValue peekValue(int index) { TypedValue tv = new TypedValue(); return getValue(index, tv) ? tv : null; }
    public int getChangingConfigurations() { return 0; }
    public void recycle() {}
    public void close() {}
}
