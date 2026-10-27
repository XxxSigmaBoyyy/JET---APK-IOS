package android.text;
public interface Editable extends CharSequence, GetChars, Spannable, Appendable {
    class Factory { private static final Factory I = new Factory(); public static Factory getInstance() { return I; } public Editable newEditable(CharSequence s) { return new SpannableStringBuilder(s); } }
    void setFilters(InputFilter[] f); InputFilter[] getFilters();
    Editable append(CharSequence text); Editable append(CharSequence text, int start, int end); Editable append(char c);
    Editable replace(int st, int en, CharSequence source, int start, int end);
    Editable replace(int st, int en, CharSequence text);
    Editable insert(int where, CharSequence text);
    Editable delete(int st, int en);
    void clear();
}
