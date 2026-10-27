package android.text;
public class SpannableString implements Spannable, GetChars {
    private final String s; private final SpanSet sp = new SpanSet();
    public SpannableString(CharSequence s) { this.s = s == null ? "" : s.toString(); if (s instanceof Spanned) { Spanned o = (Spanned) s; for (Object w : o.getSpans(0, s.length(), Object.class)) sp.set(w, o.getSpanStart(w), o.getSpanEnd(w), o.getSpanFlags(w)); } }
    public static SpannableString valueOf(CharSequence s) { return s instanceof SpannableString ? (SpannableString) s : new SpannableString(s); }
    public int length() { return s.length(); } public char charAt(int i) { return s.charAt(i); }
    public CharSequence subSequence(int a, int b) { return s.substring(a, b); } @Override public String toString() { return s; }
    public void getChars(int a, int b, char[] d, int o) { s.getChars(a, b, d, o); }
    public void setSpan(Object w, int a, int b, int f) { sp.set(w, a, b, f); } public void removeSpan(Object w) { sp.remove(w); }
    public <T> T[] getSpans(int a, int b, Class<T> t) { return sp.get(a, b, t); }
    public int getSpanStart(Object w) { return sp.start(w); } public int getSpanEnd(Object w) { return sp.end(w); } public int getSpanFlags(Object w) { return sp.flags(w); }
    public int nextSpanTransition(int a, int l, Class t) { return sp.next(a, l, t); }
}
