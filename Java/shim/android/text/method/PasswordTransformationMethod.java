package android.text.method;
public class PasswordTransformationMethod implements TransformationMethod {
    private static final PasswordTransformationMethod I = new PasswordTransformationMethod();
    public static PasswordTransformationMethod getInstance() { return I; }
    public CharSequence getTransformation(CharSequence s, android.view.View v) { if (s == null) return null; char[] c = new char[s.length()]; java.util.Arrays.fill(c, '\u2022'); return new String(c); }
    public void onFocusChanged(android.view.View v, CharSequence s, boolean f, int d, android.graphics.Rect p) {}
}
