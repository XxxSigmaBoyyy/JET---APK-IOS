package android.content;
import android.os.Bundle;
import java.io.Serializable;
import java.util.*;
public class Intent implements Cloneable, android.os.Parcelable {
    @Override public Object clone() { return new Intent(this); }
    public static final android.os.Parcelable.Creator<Intent> CREATOR = new android.os.Parcelable.Creator<Intent>() {
        public Intent createFromParcel(android.os.Parcel in) { return new Intent(); }
        public Intent[] newArray(int size) { return new Intent[size]; }
    };
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel dest, int flags) {}
    private String action; private String type; private android.net.Uri data;
    private String className; private String packageName;
    private final Bundle extras = new Bundle();
    private int flags;
    private java.util.LinkedHashSet<String> categories; private Intent selector; private String identifier; private android.graphics.Rect sourceBounds;
    public static final int FILL_IN_ACTION = 1, FILL_IN_DATA = 2, FILL_IN_CATEGORIES = 4, FILL_IN_COMPONENT = 8, FILL_IN_PACKAGE = 16, FILL_IN_SOURCE_BOUNDS = 32, FILL_IN_SELECTOR = 64, FILL_IN_IDENTIFIER = 256;
    public static final int URI_INTENT_SCHEME = 1, URI_ANDROID_APP_SCHEME = 2, URI_ALLOW_UNSAFE = 4;
    public Intent setComponent(ComponentName c) { if (c == null) { className = null; } else { packageName = c.getPackageName(); className = c.getClassName(); } return this; }
    public ComponentName resolveActivity(android.content.pm.PackageManager pm) { return getComponent(); }
    public Intent setType(String t) { data = null; type = t; return this; }
    public String getType() { return type; }
    public String resolveType(Context c) { return type; }
    public Intent setDataAndNormalize(android.net.Uri u) { return setData(u == null ? null : u.normalizeScheme()); }
    public Intent setTypeAndNormalize(String t) { return setType(t == null ? null : t.trim().toLowerCase(java.util.Locale.ROOT)); }
    public Intent setDataAndTypeAndNormalize(android.net.Uri u, String t) { return setDataAndType(u == null ? null : u.normalizeScheme(), t == null ? null : t.trim().toLowerCase(java.util.Locale.ROOT)); }
    public String getScheme() { return data == null ? null : data.getScheme(); }
    public boolean hasCategory(String c) { return categories != null && categories.contains(c); }
    public java.util.Set<String> getCategories() { return categories; }
    public Intent addCategory(String c) { if (categories == null) categories = new java.util.LinkedHashSet<>(); categories.add(c.intern()); return this; }
    public void removeCategory(String c) { if (categories != null) { categories.remove(c); if (categories.isEmpty()) categories = null; } }
    public void setSelector(Intent sel) { if (sel == this) throw new IllegalArgumentException("Intent being set as a selector of itself"); selector = sel; }
    public Intent getSelector() { return selector; }
    public Intent setIdentifier(String id) { identifier = id; return this; }
    public String getIdentifier() { return identifier; }
    public void setSourceBounds(android.graphics.Rect r) { sourceBounds = r == null ? null : new android.graphics.Rect(r); }
    public android.graphics.Rect getSourceBounds() { return sourceBounds; }
    public boolean filterEquals(Intent o) {
        if (o == null) return false;
        return java.util.Objects.equals(action, o.action) && java.util.Objects.equals(data, o.data) && java.util.Objects.equals(type, o.type)
            && java.util.Objects.equals(identifier, o.identifier) && java.util.Objects.equals(packageName, o.packageName)
            && java.util.Objects.equals(className, o.className) && java.util.Objects.equals(categories, o.categories);
    }
    public int filterHashCode() { return java.util.Objects.hash(action, data, type, identifier, packageName, className, categories); }
    public Intent cloneFilter() { Intent i = new Intent(); i.action = action; i.data = data; i.type = type; i.identifier = identifier; i.packageName = packageName; i.className = className;
        if (categories != null) i.categories = new java.util.LinkedHashSet<>(categories); if (selector != null) i.selector = new Intent(selector); return i; }
    public int fillIn(Intent o, int fl) {
        int changes = 0;
        if (o.action != null && (action == null || (fl & FILL_IN_ACTION) != 0)) { action = o.action; changes |= FILL_IN_ACTION; }
        if ((o.data != null || o.type != null) && ((data == null && type == null) || (fl & FILL_IN_DATA) != 0)) { data = o.data; type = o.type; changes |= FILL_IN_DATA; }
        if (o.identifier != null && (identifier == null || (fl & FILL_IN_IDENTIFIER) != 0)) { identifier = o.identifier; changes |= FILL_IN_IDENTIFIER; }
        if (o.categories != null && (categories == null || (fl & FILL_IN_CATEGORIES) != 0)) { categories = new java.util.LinkedHashSet<>(o.categories); changes |= FILL_IN_CATEGORIES; }
        if (o.packageName != null && (packageName == null || (fl & FILL_IN_PACKAGE) != 0) && selector == null) { packageName = o.packageName; changes |= FILL_IN_PACKAGE; }
        if (o.className != null && (fl & FILL_IN_COMPONENT) != 0) { className = o.className; packageName = o.packageName; changes |= FILL_IN_COMPONENT; }
        if (o.sourceBounds != null && (sourceBounds == null || (fl & FILL_IN_SOURCE_BOUNDS) != 0)) { sourceBounds = new android.graphics.Rect(o.sourceBounds); changes |= FILL_IN_SOURCE_BOUNDS; }
        flags |= o.flags; extras.putAll(o.extras);
        return changes;
    }
    public static Intent createChooser(Intent target, CharSequence title) {
        Intent i = new Intent(ACTION_CHOOSER_JET); i.jetChooserTarget = target;
        if (title != null) i.putExtra("android.intent.extra.TITLE", title); return i;
    }
    public static Intent createChooser(Intent target, CharSequence title, IntentSender sender) { return createChooser(target, title); }
    private static final String ACTION_CHOOSER_JET = "android.intent.action.CHOOSER"; public Intent jetChooserTarget;
    public static Intent makeMainActivity(ComponentName main) { Intent i = new Intent("android.intent.action.MAIN"); i.setComponent(main); i.addCategory("android.intent.category.LAUNCHER"); return i; }
    public String toUri(int fl) {
        StringBuilder b = new StringBuilder(); if (data != null) b.append(data.toString()); b.append("#Intent;");
        if (action != null) b.append("action=").append(android.net.Uri.encode(action)).append(';');
        if (categories != null) for (String c : categories) b.append("category=").append(android.net.Uri.encode(c)).append(';');
        if (type != null) b.append("type=").append(android.net.Uri.encode(type, "/")).append(';');
        if (flags != 0) b.append("launchFlags=0x").append(Integer.toHexString(flags)).append(';');
        if (packageName != null && className == null) b.append("package=").append(android.net.Uri.encode(packageName)).append(';');
        if (className != null) b.append("component=").append(android.net.Uri.encode(packageName + "/" + className, "/")).append(';');
        return b.append("end").toString();
    }
    public static Intent parseUri(String uri, int fl) throws java.net.URISyntaxException {
        int i = uri.lastIndexOf("#Intent;"); Intent in = new Intent("android.intent.action.VIEW");
        if (i < 0) { in.setData(android.net.Uri.parse(uri)); return in; }
        if (i > 0) in.data = android.net.Uri.parse(uri.substring(0, i)); in.action = null;
        for (String part : uri.substring(i + 8).split(";")) {
            if (part.equals("end")) break; int eq = part.indexOf('='); if (eq < 0) continue;
            String k = part.substring(0, eq), v = android.net.Uri.decode(part.substring(eq + 1));
            switch (k) { case "action": in.action = v; break; case "category": in.addCategory(v); break; case "type": in.type = v; break;
                case "launchFlags": in.flags = (int) Long.decode(v).longValue(); break; case "package": in.packageName = v; break;
                case "component": { int sl = v.indexOf('/'); in.packageName = v.substring(0, sl); String c = v.substring(sl + 1); in.className = c.startsWith(".") ? in.packageName + c : c; break; }
                default: if (k.length() > 2 && k.charAt(1) == '.') { String n = k.substring(2); switch (k.charAt(0)) { case 'S': in.putExtra(n, v); break; case 'i': in.putExtra(n, Integer.parseInt(v)); break; case 'l': in.putExtra(n, Long.parseLong(v)); break; case 'B': in.putExtra(n, Boolean.parseBoolean(v)); break; default: } } }
        }
        if (in.action == null) in.action = "android.intent.action.VIEW";
        return in;
    }
    public static Intent getIntent(String uri) throws java.net.URISyntaxException { return parseUri(uri, 0); }
    public Intent() {}
    public Intent(String action) { this.action = action; }
    public Intent(Context pkg, Class<?> cls) { setClass(pkg, cls); }
    public Intent(Intent o) { if (o != null) { action = o.action; type = o.type; data = o.data; className = o.className; packageName = o.packageName; extras.putAll(o.extras); flags = o.flags;
        if (o.categories != null) categories = new java.util.LinkedHashSet<>(o.categories); selector = o.selector == null ? null : new Intent(o.selector); identifier = o.identifier; sourceBounds = o.sourceBounds == null ? null : new android.graphics.Rect(o.sourceBounds); } }
    public android.net.Uri getData() { return data; }
    public String getDataString() { return data == null ? null : data.toString(); }
    public Intent setData(android.net.Uri u) { data = u; type = null; return this; }
    public Intent setDataAndType(android.net.Uri u, String t) { data = u; type = t; return this; }
    public Intent(String action, android.net.Uri uri) { this.action = action; this.data = uri; }
    public Intent(String action, android.net.Uri uri, Context packageContext, Class<?> cls) { this.action = action; this.data = uri; setClass(packageContext, cls); }
    private android.content.ClipData mClipData;
    /** AOSP signature: void setClipData(ClipData). */
    public void setClipData(android.content.ClipData clip) { mClipData = clip; }
    public android.content.ClipData getClipData() { return mClipData; }
    public void setExtrasClassLoader(ClassLoader loader) { /* harness: Bundle has no separate ClassLoader */ }
    public Intent setAction(String a) { action = a; return this; }
    public String getAction() { return action; }
    public Intent setClass(Context ctx, Class<?> cls) { className = cls.getName(); packageName = ctx.getPackageName(); return this; }
    public Intent setClassName(String pkg, String cls) { packageName = pkg; className = cls; return this; }
    public Intent setPackage(String pkg) { packageName = pkg; return this; }
    public String getPackage() { return packageName; }
    public ComponentName getComponent() { return className == null ? null : new ComponentName(packageName, className); }
    public Intent putExtra(String k, boolean v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, boolean[] v) { extras.jetPut(k, v); return this; }
    public boolean getBooleanExtra(String k, boolean d) { Object v = extras.get(k); return v instanceof Boolean ? (Boolean) v : d; }
    public boolean[] getBooleanArrayExtra(String k) { Object v = extras.get(k); return v instanceof boolean[] ? (boolean[]) v : null; }
    public Intent putExtra(String k, byte v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, byte[] v) { extras.jetPut(k, v); return this; }
    public byte getByteExtra(String k, byte d) { Object v = extras.get(k); return v instanceof Byte ? (Byte) v : d; }
    public byte[] getByteArrayExtra(String k) { Object v = extras.get(k); return v instanceof byte[] ? (byte[]) v : null; }
    public Intent putExtra(String k, char v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, char[] v) { extras.jetPut(k, v); return this; }
    public char getCharExtra(String k, char d) { Object v = extras.get(k); return v instanceof Character ? (Character) v : d; }
    public char[] getCharArrayExtra(String k) { Object v = extras.get(k); return v instanceof char[] ? (char[]) v : null; }
    public Intent putExtra(String k, short v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, short[] v) { extras.jetPut(k, v); return this; }
    public short getShortExtra(String k, short d) { Object v = extras.get(k); return v instanceof Short ? (Short) v : d; }
    public short[] getShortArrayExtra(String k) { Object v = extras.get(k); return v instanceof short[] ? (short[]) v : null; }
    public Intent putExtra(String k, int v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, int[] v) { extras.jetPut(k, v); return this; }
    public int getIntExtra(String k, int d) { Object v = extras.get(k); return v instanceof Integer ? (Integer) v : d; }
    public int[] getIntArrayExtra(String k) { Object v = extras.get(k); return v instanceof int[] ? (int[]) v : null; }
    public Intent putExtra(String k, long v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, long[] v) { extras.jetPut(k, v); return this; }
    public long getLongExtra(String k, long d) { Object v = extras.get(k); return v instanceof Long ? (Long) v : d; }
    public long[] getLongArrayExtra(String k) { Object v = extras.get(k); return v instanceof long[] ? (long[]) v : null; }
    public Intent putExtra(String k, float v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, float[] v) { extras.jetPut(k, v); return this; }
    public float getFloatExtra(String k, float d) { Object v = extras.get(k); return v instanceof Float ? (Float) v : d; }
    public float[] getFloatArrayExtra(String k) { Object v = extras.get(k); return v instanceof float[] ? (float[]) v : null; }
    public Intent putExtra(String k, double v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, double[] v) { extras.jetPut(k, v); return this; }
    public double getDoubleExtra(String k, double d) { Object v = extras.get(k); return v instanceof Double ? (Double) v : d; }
    public double[] getDoubleArrayExtra(String k) { Object v = extras.get(k); return v instanceof double[] ? (double[]) v : null; }
    public Intent putExtra(String k, String v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, CharSequence v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, java.io.Serializable v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, android.os.Parcelable v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, Bundle v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, String[] v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, CharSequence[] v) { extras.jetPut(k, v); return this; }
    public Intent putExtra(String k, android.os.Parcelable[] v) { extras.jetPut(k, v); return this; }
    public String getStringExtra(String k) { Object v = extras.get(k); return v instanceof String ? (String) v : v instanceof CharSequence ? v.toString() : null; }
    public CharSequence getCharSequenceExtra(String k) { Object v = extras.get(k); return v instanceof CharSequence ? (CharSequence) v : null; }
    public String[] getStringArrayExtra(String k) { Object v = extras.get(k); return v instanceof String[] ? (String[]) v : null; }
    public CharSequence[] getCharSequenceArrayExtra(String k) { Object v = extras.get(k); return v instanceof CharSequence[] ? (CharSequence[]) v : null; }
    public java.io.Serializable getSerializableExtra(String k) { Object v = extras.get(k); return v instanceof java.io.Serializable ? (java.io.Serializable) v : null; }
    public <T extends java.io.Serializable> T getSerializableExtra(String k, Class<T> c) { Object v = extras.get(k); return c.isInstance(v) ? c.cast(v) : null; }
    @SuppressWarnings("unchecked") public <T extends android.os.Parcelable> T getParcelableExtra(String k) { Object v = extras.get(k); return v instanceof android.os.Parcelable ? (T) v : null; }
    public <T> T getParcelableExtra(String k, Class<T> c) { Object v = extras.get(k); return c.isInstance(v) ? c.cast(v) : null; }
    @SuppressWarnings("unchecked") public <T> java.util.ArrayList<T> getParcelableArrayListExtra(String k, Class<? extends T> c) { Object v = extras.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<T>) v : null; }
    @SuppressWarnings("unchecked") public <T> T[] getParcelableArrayExtra(String k, Class<T> c) { Object v = extras.get(k); if (!(v instanceof Object[])) return null; Object[] a = (Object[]) v; T[] r = (T[]) java.lang.reflect.Array.newInstance(c, a.length); System.arraycopy(a, 0, r, 0, a.length); return r; }
    public android.os.Parcelable[] getParcelableArrayExtra(String k) { Object v = extras.get(k); return v instanceof android.os.Parcelable[] ? (android.os.Parcelable[]) v : null; }
    @SuppressWarnings("unchecked") public <T extends android.os.Parcelable> java.util.ArrayList<T> getParcelableArrayListExtra(String k) { Object v = extras.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<T>) v : null; }
    @SuppressWarnings("unchecked") public java.util.ArrayList<String> getStringArrayListExtra(String k) { Object v = extras.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<String>) v : null; }
    @SuppressWarnings("unchecked") public java.util.ArrayList<Integer> getIntegerArrayListExtra(String k) { Object v = extras.get(k); return v instanceof java.util.ArrayList ? (java.util.ArrayList<Integer>) v : null; }
    public Intent putStringArrayListExtra(String k, java.util.ArrayList<String> v) { extras.jetPut(k, v); return this; }
    public Intent putIntegerArrayListExtra(String k, java.util.ArrayList<Integer> v) { extras.jetPut(k, v); return this; }
    public Intent putParcelableArrayListExtra(String k, java.util.ArrayList<? extends android.os.Parcelable> v) { extras.jetPut(k, v); return this; }
    public Intent putCharSequenceArrayListExtra(String k, java.util.ArrayList<CharSequence> v) { extras.jetPut(k, v); return this; }
    public Bundle getBundleExtra(String k) { Object v = extras.get(k); return v instanceof Bundle ? (Bundle) v : null; }
    public boolean hasExtra(String k) { return extras.containsKey(k); }
    public void removeExtra(String k) { extras.remove(k); }
    public Intent putExtras(Bundle b) { if (b != null) for (String k : b.keySet()) extras.jetPut(k, b.get(k)); return this; }
    public Intent putExtras(Intent o) { return o == null ? this : putExtras(o.extras); }
    public Intent replaceExtras(Bundle b) { extras.clear(); return putExtras(b); }
    public Bundle getExtras() { return extras; }
    public Intent addFlags(int f) { flags |= f; return this; }
    public Intent setFlags(int f) { flags = f; return this; }
    public void removeFlags(int f) { flags &= ~f; }
    public int getFlags() { return flags; }
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;

    /** AOSP Intent.parseIntent: <intent android:action/data/mimeType/targetPackage/targetClass> with <category>/<extra> children. */
    public static Intent parseIntent(android.content.res.Resources r, org.xmlpull.v1.XmlPullParser p, android.util.AttributeSet a) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        final String NS = "http://schemas.android.com/apk/res/android";
        Intent in = new Intent();
        String act = a.getAttributeValue(NS, "action"); if (act != null) in.setAction(act);
        String data = a.getAttributeValue(NS, "data"), mime = a.getAttributeValue(NS, "mimeType");
        if (data != null) in.setDataAndType(android.net.Uri.parse(data), mime); else if (mime != null) in.setType(mime);
        String pkg = a.getAttributeValue(NS, "targetPackage"), cls = a.getAttributeValue(NS, "targetClass");
        if (pkg != null && cls != null) in.setComponent(new ComponentName(pkg, cls));
        int depth = p.getDepth(), t;
        while ((t = p.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && (t != org.xmlpull.v1.XmlPullParser.END_TAG || p.getDepth() > depth)) {
            if (t != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
            if ("category".equals(p.getName())) { String c = p.getAttributeValue(NS, "name"); if (c != null) in.addCategory(c); }
            else if ("extra".equals(p.getName())) { String k = p.getAttributeValue(NS, "name"), v = p.getAttributeValue(NS, "value"); if (k != null) in.putExtra(k, v); }
        }
        return in;
    }
}
