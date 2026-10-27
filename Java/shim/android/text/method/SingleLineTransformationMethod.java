package android.text.method;
public class SingleLineTransformationMethod extends ReplacementTransformationMethod {
    private static final SingleLineTransformationMethod I = new SingleLineTransformationMethod();
    public static SingleLineTransformationMethod getInstance() { return I; }
    protected char[] getOriginal() { return new char[]{'\n', '\r'}; } protected char[] getReplacement() { return new char[]{' ', '\uFEFF'}; }
}
