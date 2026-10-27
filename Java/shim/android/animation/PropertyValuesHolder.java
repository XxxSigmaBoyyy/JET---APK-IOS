package android.animation;
public class PropertyValuesHolder implements Cloneable {
    String mName; android.util.Property mProperty; Object[] mValues; TypeEvaluator mEvaluator; Class<?> mType; Object mAnimatedValue;
    private PropertyValuesHolder(String n, android.util.Property p, Class<?> type, Object[] v) { mName = n; mProperty = p; mType = type; mValues = v; }
    public static PropertyValuesHolder ofFloat(String n, float... v) { return new PropertyValuesHolder(n, null, float.class, box(v)); }
    public static PropertyValuesHolder ofFloat(android.util.Property<?, Float> p, float... v) { return new PropertyValuesHolder(p.getName(), p, float.class, box(v)); }
    public static PropertyValuesHolder ofInt(String n, int... v) { return new PropertyValuesHolder(n, null, int.class, box(v)); }
    public static PropertyValuesHolder ofInt(android.util.Property<?, Integer> p, int... v) { return new PropertyValuesHolder(p.getName(), p, int.class, box(v)); }
    public static PropertyValuesHolder ofObject(String n, TypeEvaluator e, Object... v) { PropertyValuesHolder h = new PropertyValuesHolder(n, null, Object.class, v); h.mEvaluator = e; return h; }
    public static <V> PropertyValuesHolder ofObject(android.util.Property p, TypeEvaluator<V> e, V... v) { PropertyValuesHolder h = new PropertyValuesHolder(p.getName(), p, Object.class, v); h.mEvaluator = e; return h; }
    public static PropertyValuesHolder ofMultiInt(String n, int[][] v) { return new PropertyValuesHolder(n, null, Object.class, v); }
    public static PropertyValuesHolder ofMultiFloat(String n, float[][] v) { return new PropertyValuesHolder(n, null, Object.class, v); }
    public static PropertyValuesHolder ofKeyframe(String n, Keyframe... k) { Object[] v = new Object[k.length]; for (int i = 0; i < k.length; i++) v[i] = k[i].getValue(); return new PropertyValuesHolder(n, null, k.length > 0 && k[0].getValue() instanceof Integer ? int.class : float.class, v); }
    static Object[] box(float[] v) { Object[] o = new Object[v.length]; for (int i = 0; i < v.length; i++) o[i] = v[i]; return o; }
    static Object[] box(int[] v) { Object[] o = new Object[v.length]; for (int i = 0; i < v.length; i++) o[i] = v[i]; return o; }
    public void setFloatValues(float... v) { mValues = box(v); mType = float.class; } public void setIntValues(int... v) { mValues = box(v); mType = int.class; } public void setObjectValues(Object... v) { mValues = v; }
    public void setEvaluator(TypeEvaluator e) { mEvaluator = e; }
    public void setPropertyName(String n) { mName = n; } public String getPropertyName() { return mName; } public void setProperty(android.util.Property p) { mProperty = p; }
    void calc(float f) {
        if (mValues == null || mValues.length == 0) return;
        if (mValues.length == 1) { mAnimatedValue = mValues[0]; return; }
        float seg = f * (mValues.length - 1); int i = Math.max(0, Math.min(mValues.length - 2, (int) Math.floor(seg))); float lf = seg - i;
        Object a = mValues[i], b = mValues[i + 1];
        if (mEvaluator != null) mAnimatedValue = mEvaluator.evaluate(lf, a, b);
        else if (a instanceof Integer && b instanceof Integer) mAnimatedValue = (int) ((Integer) a + lf * ((Integer) b - (Integer) a));
        else if (a instanceof Number && b instanceof Number) mAnimatedValue = ((Number) a).floatValue() + lf * (((Number) b).floatValue() - ((Number) a).floatValue());
        else mAnimatedValue = lf < 1 ? a : b;
    }
    Object animated() { return mAnimatedValue; }
    @Override public PropertyValuesHolder clone() { try { return (PropertyValuesHolder) super.clone(); } catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
    @Override public String toString() { return mName + ": " + java.util.Arrays.toString(mValues); }
}
