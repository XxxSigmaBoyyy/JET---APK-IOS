package android.animation;
public abstract class Keyframe implements Cloneable {
    float mFraction; Object mValue; TimeInterpolator mInterp;
    public static Keyframe ofFloat(float f, float v) { Keyframe k = new Keyframe() {}; k.mFraction = f; k.mValue = v; return k; }
    public static Keyframe ofFloat(float f) { Keyframe k = new Keyframe() {}; k.mFraction = f; k.mValue = 0f; return k; }
    public static Keyframe ofInt(float f, int v) { Keyframe k = new Keyframe() {}; k.mFraction = f; k.mValue = v; return k; }
    public static Keyframe ofInt(float f) { Keyframe k = new Keyframe() {}; k.mFraction = f; k.mValue = 0; return k; }
    public static Keyframe ofObject(float f, Object v) { Keyframe k = new Keyframe() {}; k.mFraction = f; k.mValue = v; return k; }
    public Object getValue() { return mValue; } public void setValue(Object v) { mValue = v; } public float getFraction() { return mFraction; } public void setFraction(float f) { mFraction = f; }
    public boolean hasValue() { return mValue != null; } public TimeInterpolator getInterpolator() { return mInterp; } public void setInterpolator(TimeInterpolator i) { mInterp = i; }
    public Class getType() { return mValue == null ? Object.class : mValue.getClass(); }
    @Override public Keyframe clone() { try { return (Keyframe) super.clone(); } catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
}
