package android.text;
/** AOSP: spans implementing this are not copied when the text is copied into a new Spanned/Editable. */
public interface NoCopySpan {
    class Concrete implements NoCopySpan { public Concrete() {} }
}
