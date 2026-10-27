package android.animation;
import java.lang.reflect.Method;
public final class ObjectAnimator extends ValueAnimator {
    private Object mTarget; private boolean mAutoCancel;
    public ObjectAnimator() {}
    public static ObjectAnimator ofFloat(Object t, String p, float... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofFloat(p, v)); return a; }
    public static <T> ObjectAnimator ofFloat(T t, android.util.Property<T, Float> p, float... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofFloat(p, v)); return a; }
    public static ObjectAnimator ofInt(Object t, String p, int... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofInt(p, v)); return a; }
    public static <T> ObjectAnimator ofInt(T t, android.util.Property<T, Integer> p, int... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofInt(p, v)); return a; }
    public static ObjectAnimator ofArgb(Object t, String p, int... v) { ObjectAnimator a = ofInt(t, p, v); a.setEvaluator(ArgbEvaluator.getInstance()); return a; }
    public static <T> ObjectAnimator ofArgb(T t, android.util.Property<T, Integer> p, int... v) { ObjectAnimator a = ofInt(t, p, v); a.setEvaluator(ArgbEvaluator.getInstance()); return a; }
    public static ObjectAnimator ofObject(Object t, String p, TypeEvaluator e, Object... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofObject(p, e, v)); return a; }
    public static <T, V> ObjectAnimator ofObject(T t, android.util.Property<T, V> p, TypeEvaluator<V> e, V... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(PropertyValuesHolder.ofObject(p, e, v)); return a; }
    public static ObjectAnimator ofPropertyValuesHolder(Object t, PropertyValuesHolder... v) { ObjectAnimator a = new ObjectAnimator(); a.mTarget = t; a.setValues(v); return a; }
    public Object getTarget() { return mTarget; } @Override public void setTarget(Object t) { mTarget = t; }
    public void setPropertyName(String n) { if (mValues.length > 0) mValues[0].setPropertyName(n); } public String getPropertyName() { return mValues.length > 0 ? mValues[0].getPropertyName() : null; }
    public void setProperty(android.util.Property p) { if (mValues.length > 0) mValues[0].setProperty(p); }
    public void setAutoCancel(boolean c) { mAutoCancel = c; }
    @Override public ObjectAnimator setDuration(long d) { super.setDuration(d); return this; }
    @Override public void setFloatValues(float... v) { if (mValues.length == 0) setValues(PropertyValuesHolder.ofFloat("", v)); else super.setFloatValues(v); }
    @Override void jetApply() {
        if (mTarget == null) return;
        for (PropertyValuesHolder h : mValues) {
            Object v = h.animated(); if (v == null) continue;
            try {
                if (h.mProperty != null) { h.mProperty.set(mTarget, v instanceof Number && h.mType == float.class ? (Object) ((Number) v).floatValue() : v); continue; }
                String n = h.getPropertyName(); if (n == null || n.isEmpty()) continue;
                String m = "set" + Character.toUpperCase(n.charAt(0)) + n.substring(1);
                Method best = null;
                for (Class<?> c = mTarget.getClass(); c != null && best == null; c = c.getSuperclass())
                    for (Method x : c.getDeclaredMethods()) if (x.getName().equals(m) && x.getParameterCount() == 1) { best = x; break; }
                if (best == null) continue;
                best.setAccessible(true); Class<?> pt = best.getParameterTypes()[0];
                Object arg = pt == float.class || pt == Float.class ? (Object) ((Number) v).floatValue() : pt == int.class || pt == Integer.class ? (Object) ((Number) v).intValue() : v;
                best.invoke(mTarget, arg);
            } catch (Throwable ignored) {}
        }
    }
    @Override public ObjectAnimator clone() { return (ObjectAnimator) super.clone(); }
}
