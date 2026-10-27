package android.text;
public interface Spannable extends Spanned {
    void setSpan(Object what, int start, int end, int flags);
    void removeSpan(Object what);
    class Factory { private static final Factory I = new Factory(); public static Factory getInstance() { return I; } public Spannable newSpannable(CharSequence s) { return new SpannableString(s); } }
}
