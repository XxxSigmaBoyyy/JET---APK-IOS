package android.content.res;
import android.util.DisplayMetrics;
import jet.runtime.ResTable;
/** Thin facade over the JET bundle's resource table. */
public class Resources {
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String s) { super(s); }
    }
    private final ResTable t;
    private Configuration mConfig = Configuration.jetDevice();
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    public Resources(AssetManager assets, android.util.DisplayMetrics metrics, Configuration config) {
        // Prefer sharing the harness SYSTEM table; fall back to empty metrics-only facade.
        ResTable table = (SYSTEM != null) ? SYSTEM.t : null;
        this.t = table;
        if (SYSTEM == null) SYSTEM = this;
        if (metrics != null) {
            mMetrics.density = metrics.density;
            mMetrics.densityDpi = metrics.densityDpi;
            mMetrics.scaledDensity = metrics.scaledDensity;
        } else if (table != null) {
            mMetrics.density = table.density;
            mMetrics.densityDpi = (int)(160 * table.density);
            mMetrics.scaledDensity = table.density;
        }
        if (config != null) mConfig = new Configuration(config);
    }
public Resources(ResTable t) {
        this.t = t; if (SYSTEM == null) SYSTEM = this;
        mMetrics.density = t.density;
        mMetrics.densityDpi = (int)(160 * t.density);
        mMetrics.scaledDensity = t.density;
    }
    public ResTable table() { return t; }
    public String getString(int id) { String s = t.string(id); if (s == null) throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id)); return s; }
    public CharSequence getText(int id) { return getString(id); }
    private XmlResourceParser xml(int id) {
        // Soft-missing framework/app XML: empty parser so Material/AppCompat inflation proceeds.
        if (t == null || !t.hasXmlFile(id)) {
            System.err.println("[JET] soft-missing xml 0x" + Integer.toHexString(id));
            if (t == null) return null;
            // android.R.transition / anim must not return the LinearLayout placeholder — TransitionInflater
            // treats the root tag as the transition type ("Unknown scene name: LinearLayout").
            int type = (id >>> 16) & 0xff;
            if ((id >>> 24) == 0x01 && (type == 0x0f || type == 0x04 || type == 0x01)) {
                try {
                    org.w3c.dom.Element e = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new java.io.ByteArrayInputStream(
                            ("<transitionSet xmlns:android=\"http://schemas.android.com/apk/res/android\"/>").getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .getDocumentElement();
                    return new jet.runtime.DomPullParser(e, t);
                } catch (Exception ex) { return t.xmlParser(0); }
            }
            return t.xmlParser(0);
        }
        return t.xmlParser(id);
    }
    public XmlResourceParser getXml(int id) { return xml(id); }
    public XmlResourceParser getLayout(int id) { return xml(id); }
    public XmlResourceParser getAnimation(int id) { return xml(id); }
    public int getColor(int id) { return t.color(id); }
    public float getDimension(int id) { return t.dimension(id); }
    public String getResourceName(int id) { return t.fullName(id); }
    public String getResourceEntryName(int id) { return t.name(id); }
    public String getPackageName() { return t.pkg; }
    public DisplayMetrics getDisplayMetrics() { return mMetrics; }
    public Configuration getConfiguration() { jetSyncOrientation(); return mConfig; }
    /** Host rotated (requested orientation): mirror it into this Resources' configuration like a config change. */
    private void jetSyncOrientation() { int o = DisplayMetrics.jetLandscape() ? 2 : 1; if (mConfig.orientation != o) { Configuration c = new Configuration(mConfig); c.orientation = o; int w = c.screenWidthDp, h = c.screenHeightDp; if ((o == 2) == (w < h)) { c.screenWidthDp = h; c.screenHeightDp = w; } mConfig = c; } }
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        if (config != null) { Configuration m = new Configuration(mConfig); m.updateFrom(config); mConfig = m; }
        if (metrics != null) {
            mMetrics.density = metrics.density;
            mMetrics.densityDpi = metrics.densityDpi;
            mMetrics.widthPixels = metrics.widthPixels;
            mMetrics.heightPixels = metrics.heightPixels;
            mMetrics.scaledDensity = metrics.scaledDensity;
        }
    }
    public int getIdentifier(String name, String defType, String defPackage) {
        String n = name, pk = defPackage; int c = n.indexOf(':'); if (c >= 0) { pk = n.substring(0, c); n = n.substring(c + 1); }
        String ty = defType; int sl = n.indexOf('/'); if (sl >= 0) { ty = n.substring(0, sl); n = n.substring(sl + 1); }
        Integer id = ty == null ? null : t.idByName(ty, n, "android".equals(pk)); return id == null ? 0 : id;
    }
    public android.graphics.drawable.Drawable getDrawable(int id) { try { return t.drawable("@" + Integer.toHexString(id)); } catch (Throwable e) { return null; } }
    public android.graphics.drawable.Drawable getDrawable(int id, Theme th) { return getDrawable(id); }
    public android.graphics.drawable.Drawable getDrawableForDensity(int id, int d, Theme th) { return getDrawable(id); }
    public int getColor(int id, Theme th) { if (t.hasXmlFile(id)) { ColorStateList c = getColorStateList(id, th); if (c != null) return c.getDefaultColor(); } return getColor(id); }
    public ColorStateList getColorStateList(int id) { return getColorStateList(id, null); }
    /** Most recently styled theme: stands in for the context theme when callers pass none (ContextCompat paths). */
    public static Theme jetLastTheme;
    /** Last Context.setTheme / Activity theme resid (sticky; not cleared by overlays). */
    public static int jetLastAppThemeResid;
    public ColorStateList getColorStateList(int id, Theme th) {
        if (t.hasXmlFile(id)) {
            try { org.w3c.dom.Element e = t.layout(id); if ("selector".equals(e.getTagName())) { ColorStateList c = ColorStateList.jetInflate(this, e, th != null ? th : jetLastTheme); if (c != null) return c; } } catch (Throwable ex) { System.err.println("[RES] color selector 0x" + Integer.toHexString(id) + ": " + ex); }
        }
        return ColorStateList.valueOf(getColor(id));
    }
    private String raw(int id) { String v = t.resolve("@" + Integer.toHexString(id)); if (v == null || v.startsWith("@")) { String d = t.string(id); if (d != null) v = d; } return v; }
    public void getValue(int id, android.util.TypedValue out, boolean resolveRefs) {
        String v = raw(id);
        if (v == null) throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id));
        out.resourceId = id; out.string = v; out.type = android.util.TypedValue.TYPE_STRING;
        try {
            if (v.startsWith("#")) { out.type = android.util.TypedValue.TYPE_INT_COLOR_ARGB8; out.data = ResTable.parseColor(v); }
            else if ("true".equals(v) || "false".equals(v)) { out.type = android.util.TypedValue.TYPE_INT_BOOLEAN; out.data = "true".equals(v) ? 1 : 0; }
            else if (v.matches("-?[0-9]+")) { out.type = android.util.TypedValue.TYPE_INT_DEC; out.data = Integer.parseInt(v); }
            else if (v.matches("-?[0-9.]+(dip|dp|sp|px)")) { out.type = android.util.TypedValue.TYPE_DIMENSION; String un = v.replaceAll("^-?[0-9.]+", ""); int u = un.equals("px") ? 0 : un.equals("sp") ? 2 : 1; out.data = android.util.TypedValue.createComplexDimension(Float.parseFloat(v.substring(0, v.length() - un.length())), u); }
            else if (v.matches("-?[0-9]*\\.[0-9]+")) { out.type = android.util.TypedValue.TYPE_FLOAT; out.data = Float.floatToIntBits(Float.parseFloat(v)); }
        } catch (Exception ignored) {}
    }
    public void getValue(String name, android.util.TypedValue out, boolean resolveRefs) { throw new NotFoundException(name); }
    public boolean getBoolean(int id) { return "true".equals(raw(id)); }
    public int getInteger(int id) { String v = raw(id); try { return v == null ? 0 : ResTable.intVal(v); } catch (Exception e) { return 0; } }
    public float getFloat(int id) { String v = raw(id); try { return v == null ? 0 : Float.parseFloat(v); } catch (Exception e) { return 0; } }
    public int getDimensionPixelSize(int id) { return Math.round(getDimension(id)); }
    public int getDimensionPixelOffset(int id) { return (int) getDimension(id); }
    public float getFraction(int id, int base, int pbase) { return 0; }
    public String[] getStringArray(int id) { return arr(id); }
    private String[] arr(int id) { String[] a = t.array(id); if (a == null) throw new NotFoundException("Array resource ID #0x" + Integer.toHexString(id)); String[] r = new String[a.length]; for (int i = 0; i < a.length; i++) r[i] = t.resolve(a[i]); return r; }
    public java.io.InputStream openRawResource(int id) { java.io.File f = t.resFile(id); if (f == null || !f.isFile()) throw new NotFoundException("File resource ID #0x" + Integer.toHexString(id)); try { return new java.io.FileInputStream(f); } catch (java.io.IOException e) { throw new NotFoundException(e.toString()); } }
    public java.io.InputStream openRawResource(int id, android.util.TypedValue v) { if (v != null) { v.density = jetResourceDensity(id); v.resourceId = id; } return openRawResource(id); }
    /** JET: the dpi bucket of the bitmap file chosen for a drawable resource. */
    public int jetResourceDensity(int id) { return t == null ? 160 : t.bitmapDensity(id); }
    public CharSequence[] getTextArray(int id) { return arr(id); }
    public int[] getIntArray(int id) { String[] a = arr(id); int[] r = new int[a.length]; for (int i = 0; i < a.length; i++) try { r[i] = jet.runtime.ResTable.intVal(a[i]); } catch (RuntimeException e) { r[i] = 0; } return r; }
    public TypedArray obtainTypedArray(int id) { String[] a = t.array(id); if (a == null) throw new NotFoundException("Array resource ID #0x" + Integer.toHexString(id)); return TypedArray.jetFromValues(this, a.clone()); }
    public TypedArray obtainAttributes(android.util.AttributeSet set, int[] attrs) { return new TypedArray(this, attrs, jetLastTheme, set); }
    public String getQuantityString(int id, int qty) { String v = t == null ? null : t.plural(id, qty); return v != null ? v : getString(id); }
    public String getQuantityString(int id, int qty, Object... args) { return String.format(getQuantityString(id, qty), args); }
    public CharSequence getQuantityText(int id, int qty) { return getQuantityString(id, qty); }
    public String getString(int id, Object... args) { return String.format(getString(id), args); }
    public CharSequence getText(int id, CharSequence def) { String s = t.string(id); return s != null ? s : def; }
    public String getResourceTypeName(int id) { String n = t.fullName(id); int a = n.indexOf(':'), b = n.indexOf('/'); return a >= 0 && b > a ? n.substring(a + 1, b) : null; }
    public String getResourcePackageName(int id) { return (id >>> 24) == 0x01 ? "android" : t.pkg; }
    private AssetManager mAssets;
    /** APK assets/ live in the JET bundle (read-only), not in the app data dir. */
    public synchronized AssetManager getAssets() { if (mAssets == null) mAssets = new AssetManager(t != null && t.bundleDir() != null ? new java.io.File(t.bundleDir(), "assets") : null); return mAssets; }
    public static Resources getSystem() { return SYSTEM; }
    static Resources SYSTEM;

    public final Theme newTheme() { return new Theme(); }
    public final class Theme {
        final java.util.LinkedHashMap<String, String> vals = new java.util.LinkedHashMap<>();
        /** True for Context/Activity themes; false for short-lived obtainStyledAttributes overlays / ContextThemeWrapper. */
        private boolean jetPublish = true;
        public void jetSetPublish(boolean p) { jetPublish = p; }
        public void applyStyle(int resid, boolean force) {
            if (resid == 0) return;
            // Only Context.setTheme / Activity themes publish as jetLastTheme. Temporary Theme copies used
            // inside obtainStyledAttributes must not overwrite it — otherwise Material CSL inflation sees an
            // overlay without colorPrimary and AndroidX falls back to magenta (#FFFF00FF).
            java.util.Map<String, String> st = t.style(resid);
            if (jetPublish) {
                jetLastTheme = this;
                // Only pin sticky resid for real app/material themes that define colour attrs.
                // Widget ThemeOverlays (TabLayout etc.) must not replace it — sticky fallback would
                // then miss colorPrimary and AndroidX CSL inflation paints #FFFF00FF.
                if (resid != 0 && (st.containsKey("colorPrimary") || st.containsKey("colorOnSurface")))
                    jetLastAppThemeResid = resid;
            }
            for (java.util.Map.Entry<String, String> e : st.entrySet())
                if (force || !vals.containsKey(e.getKey())) vals.put(e.getKey(), e.getValue());
        }
        public void setTo(Theme other) { vals.clear(); if (other != null) vals.putAll(other.vals); }
        public Resources getResources() { return Resources.this; }
        public android.graphics.drawable.Drawable getDrawable(int id) { return Resources.this.getDrawable(id); }
        public int getChangingConfigurations() { return 0; }
        public void rebase() {}
        public int[] getAttributeResolutionStack(int a, int b, int c) { return new int[0]; }
        public String raw(int attr) {
            String v = vals.get(t.attrKey(attr));
            if (v == null) v = vals.get(String.format("0x%08x", attr)); // pre-normalisation hex keys
            int guard = 0;
            int last = attr;
            while (v != null && v.startsWith("?") && guard++ < 8) {
                last = ResTable.refId(v);
                v = vals.get(t.attrKey(last));
                if (v == null) v = vals.get(String.format("0x%08x", last));
            }
            // Overlay/widget themes omit colour attrs; fall back to the published app theme.
            if (v == null && jetLastTheme != null && jetLastTheme != this) {
                String fb = jetLastTheme.vals.get(t.attrKey(attr));
                if (fb == null) fb = jetLastTheme.vals.get(String.format("0x%08x", attr));
                if (fb != null) { v = fb; last = attr;
                    while (v != null && v.startsWith("?") && guard++ < 8) {
                        last = ResTable.refId(v);
                        v = jetLastTheme.vals.get(t.attrKey(last));
                        if (v == null) v = jetLastTheme.vals.get(String.format("0x%08x", last));
                    }
                }
            }
            if (v == null && jetLastAppThemeResid != 0) {
                String ak = t.attrKey(attr);
                java.util.Map<String, String> st = t.style(jetLastAppThemeResid);
                v = st.get(ak);
                if (v == null) v = st.get(String.format("0x%08x", attr));
                int g2 = 0;
                while (v != null && v.startsWith("?") && g2++ < 8) {
                    int a2 = ResTable.refId(v);
                    v = st.get(t.attrKey(a2));
                    if (v == null) v = st.get(String.format("0x%08x", a2));
                }
            }
            if (v == null && (last >>> 24) == 0x01) v = jet.runtime.FrameworkThemeDefaults.get(last);
            return v;
        }
        public TypedArray obtainStyledAttributes(int[] attrs) { return new TypedArray(Resources.this, attrs, this, null); }
        public TypedArray obtainStyledAttributes(int resid, int[] attrs) {
            Theme tmp = new Theme(); tmp.jetPublish = false; tmp.setTo(this); tmp.applyStyle(resid, true);
            return new TypedArray(Resources.this, attrs, tmp, null);
        }
        public TypedArray obtainStyledAttributes(android.util.AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            Theme tmp = new Theme(); tmp.jetPublish = false; tmp.setTo(this);
            int ds = defStyleAttr != 0 ? ResTable.refId(raw(defStyleAttr)) : 0;
            if (ds != 0) tmp.applyStyle(ds, true); else if (defStyleRes != 0) tmp.applyStyle(defStyleRes, true);
            if (TypedArray.DBG) System.err.println("[TA] osa dsa=" + Integer.toHexString(defStyleAttr) + " raw=" + (defStyleAttr != 0 ? raw(defStyleAttr) : null) + " ds=" + Integer.toHexString(ds) + " dsr=" + Integer.toHexString(defStyleRes) + " themeN=" + vals.size() + " tmpN=" + tmp.vals.size());
            return new TypedArray(Resources.this, attrs, tmp, set);
        }
        public boolean resolveAttribute(int resid, android.util.TypedValue out, boolean resolveRefs) {
            String v = raw(resid);
            if (v == null) return false;
            out.string = null; out.resourceId = 0; out.data = 0;
            int ref = ResTable.refId(v);
            if (ref != 0 && resolveRefs) {
                // Follow @ref chains like AssetManager: file-backed resources become TYPE_STRING "res/<type>/<name>.xml" (+resourceId).
                for (int guard = 0; guard < 8; guard++) {
                    String type = getResourceTypeName(ref);
                    if (t.resFile(ref) != null && !"color".equals(type) || "interpolator".equals(type) || "anim".equals(type) || "animator".equals(type)) {
                        out.type = android.util.TypedValue.TYPE_STRING; out.resourceId = ref; out.string = "res/" + type + "/" + t.name(ref) + ".xml"; return true;
                    }
                    String nv = t.resolve("@" + Integer.toHexString(ref));
                    if (nv != null && nv.startsWith("@") && ResTable.refId(nv) != 0 && ResTable.refId(nv) != ref) { ref = ResTable.refId(nv); continue; }
                    out.resourceId = ref;
                    if ("string".equals(type) && nv != null) { out.type = android.util.TypedValue.TYPE_STRING; out.string = nv; return true; }
                    if (nv != null && nv.startsWith("#")) { out.type = android.util.TypedValue.TYPE_INT_COLOR_ARGB8; out.data = ResTable.parseColor(nv); return true; }
                    if ("bool".equals(type) && nv != null) { out.type = android.util.TypedValue.TYPE_INT_BOOLEAN; out.data = "true".equals(nv) ? 1 : 0; return true; }
                    out.type = android.util.TypedValue.TYPE_REFERENCE; out.data = ref; return true;
                }
            }
            if (ref != 0) { out.resourceId = ref; out.type = android.util.TypedValue.TYPE_REFERENCE; out.data = ref; }
            else if (v.startsWith("#")) { out.type = android.util.TypedValue.TYPE_INT_COLOR_ARGB8; out.data = ResTable.parseColor(v); }
            else if ("true".equals(v) || "false".equals(v)) { out.type = android.util.TypedValue.TYPE_INT_BOOLEAN; out.data = "true".equals(v) ? 1 : 0; }
            else { out.type = android.util.TypedValue.TYPE_STRING; out.string = v; }
            return true;
        }
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public android.graphics.Typeface getFont(int p0) { jet.runtime.ShimStubs.hit("android.content.res.Resources.getFont(I)Landroid/graphics/Typeface;"); return null; }
    public android.graphics.drawable.Drawable getDrawableForDensity(int p0, int p1) { jet.runtime.ShimStubs.hit("android.content.res.Resources.getDrawableForDensity(II)Landroid/graphics/drawable/Drawable;"); return null; }
    public void getValueForDensity(int p0, int p1, android.util.TypedValue p2, boolean p3) { jet.runtime.ShimStubs.hit("android.content.res.Resources.getValueForDensity(IILandroid/util/TypedValue;Z)V"); }
    public static int getAttributeSetSourceResId(android.util.AttributeSet p0) { jet.runtime.ShimStubs.hit("android.content.res.Resources.getAttributeSetSourceResId(Landroid/util/AttributeSet;)I"); return 0; }
    public void parseBundleExtras(android.content.res.XmlResourceParser p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.content.res.Resources.parseBundleExtras(Landroid/content/res/XmlResourceParser;Landroid/os/Bundle;)V"); }
    public void parseBundleExtra(java.lang.String p0, android.util.AttributeSet p1, android.os.Bundle p2) { jet.runtime.ShimStubs.hit("android.content.res.Resources.parseBundleExtra(Ljava/lang/String;Landroid/util/AttributeSet;Landroid/os/Bundle;)V"); }
    public void flushLayoutCache() { jet.runtime.ShimStubs.hit("android.content.res.Resources.flushLayoutCache()V"); }
    public void finishPreloading() { jet.runtime.ShimStubs.hit("android.content.res.Resources.finishPreloading()V"); }
    // ---- END generated SDK surface ----
}
