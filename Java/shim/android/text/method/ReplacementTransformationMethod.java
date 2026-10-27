package android.text.method;
public abstract class ReplacementTransformationMethod implements TransformationMethod {
    protected abstract char[] getOriginal(); protected abstract char[] getReplacement();
    public CharSequence getTransformation(CharSequence s, android.view.View v) { if (s == null) return null; String r = s.toString(); char[] o = getOriginal(), n = getReplacement(); for (int i = 0; i < o.length; i++) r = r.replace(o[i], n[i]); return r; }
    public void onFocusChanged(android.view.View v, CharSequence s, boolean f, int d, android.graphics.Rect p) {}
}
