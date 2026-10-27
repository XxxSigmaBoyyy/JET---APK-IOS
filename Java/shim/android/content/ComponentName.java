package android.content;
public final class ComponentName {
    private final String pkg, cls;
    public ComponentName(String pkg, String cls) { this.pkg = pkg; this.cls = cls; }
    public ComponentName(Context ctx, Class<?> cls) { this(ctx.getPackageName(), cls.getName()); }
    public ComponentName(Context ctx, String cls) { this(ctx.getPackageName(), cls); }
    public String getPackageName() { return pkg; }
    public String getClassName() { return cls; }
    @Override public String toString() { return pkg + "/" + cls; }
    public String flattenToString() { return pkg + "/" + cls; }
    public String flattenToShortString() { return cls != null && pkg != null && cls.startsWith(pkg + ".") ? pkg + "/" + cls.substring(pkg.length()) : flattenToString(); }
    public static ComponentName unflattenFromString(String s) { int i = s == null ? -1 : s.indexOf(47); if (i < 0) return null; String p = s.substring(0, i), c = s.substring(i + 1); if (c.startsWith(".")) c = p + c; return new ComponentName(p, c); }
}
