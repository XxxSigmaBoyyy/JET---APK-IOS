package android.text;
/** AOSP-like editable text: every edit goes through {@link #replace}, which applies InputFilters, shifts spans
 *  (selection included, see {@link Selection}) and notifies TextWatcher spans (TextView attaches its ChangeWatcher). */
public class SpannableStringBuilder implements Editable {
    private final SpanSet sp = new SpanSet(); private InputFilter[] filters = new InputFilter[0];
    private final StringBuilder b;
    public SpannableStringBuilder() { b = new StringBuilder(); }
    public SpannableStringBuilder(CharSequence s) { this(s, 0, s == null ? 0 : s.length()); }
    public SpannableStringBuilder(CharSequence s, int start, int end) {
        if (s == null) s = "";
        b = new StringBuilder(end - start).append(s, start, end);   // not s.subSequence: ours returns a new SSB (recursion)
        if (s instanceof Spanned) copySpans((Spanned) s, start, end, 0);
    }
    private void copySpans(Spanned src, int start, int end, int at) {
        for (Object o : src.getSpans(start, end, Object.class)) {
            if (o instanceof NoCopySpan) continue;
            int a = src.getSpanStart(o), e = src.getSpanEnd(o); if (a < 0) continue;
            a = Math.max(a, start) - start + at; e = Math.min(e, end) - start + at;
            sp.set(o, a, Math.max(a, e), src.getSpanFlags(o));
        }
    }
    public int length() { return b.length(); }
    public char charAt(int i) { return b.charAt(i); }
    public CharSequence subSequence(int s, int e) { return new SpannableStringBuilder(this, s, e); }
    public String toString() { return b.toString(); }
    public SpannableStringBuilder append(CharSequence csq) { int n = b.length(); return replace(n, n, csq == null ? "null" : csq); }
    public SpannableStringBuilder append(CharSequence csq, int start, int end) { int n = b.length(); return replace(n, n, csq, start, end); }
    public SpannableStringBuilder append(char c) { return append(String.valueOf(c)); }
    public SpannableStringBuilder append(CharSequence t, Object what, int flags) { int st = b.length(); append(t); sp.set(what, st, b.length(), flags); return this; }
    public static SpannableStringBuilder valueOf(CharSequence s) { return s instanceof SpannableStringBuilder ? (SpannableStringBuilder) s : new SpannableStringBuilder(s); }
    public void getChars(int a, int e, char[] d, int o) { b.getChars(a, e, d, o); }
    public void setSpan(Object w, int a, int e, int f) { sp.set(w, a, e, f); } public void removeSpan(Object w) { sp.remove(w); }
    public <T> T[] getSpans(int a, int e, Class<T> t) { return sp.get(a, e, t); }
    public int getSpanStart(Object w) { return sp.start(w); } public int getSpanEnd(Object w) { return sp.end(w); } public int getSpanFlags(Object w) { return sp.flags(w); }
    public int nextSpanTransition(int a, int l, Class t) { return sp.next(a, l, t); }
    public void setFilters(InputFilter[] f) { filters = f == null ? new InputFilter[0] : f; } public InputFilter[] getFilters() { return filters; }
    public void clearSpans() { sp.spans.clear(); }
    // AOSP: covariant SpannableStringBuilder returns (WhatsApp RegisterAsCompanionActivity binds these).
    public SpannableStringBuilder replace(int st, int en, CharSequence source, int start, int end) {
        if (source == null) { source = ""; start = end = 0; }
        for (InputFilter f : filters) {
            CharSequence r = f.filter(source, start, end, this, st, en);
            if (r != null) { source = r; start = 0; end = r.length(); }
        }
        int before = en - st, after = end - start;
        if (before == 0 && after == 0 && !(source instanceof Spanned)) return this;
        TextWatcher[] ws = sp.get(st, st + before, TextWatcher.class);
        for (TextWatcher w : ws) w.beforeTextChanged(this, st, before, after);
        b.replace(st, en, source.subSequence(start, end).toString());
        sp.edit(st, en, after);
        if (source instanceof Spanned) copySpans((Spanned) source, start, end, st);
        for (TextWatcher w : ws) w.onTextChanged(this, st, before, after);
        for (TextWatcher w : ws) w.afterTextChanged(this);
        return this;
    }
    public SpannableStringBuilder replace(int st, int en, CharSequence text) { return replace(st, en, text, 0, text == null ? 0 : text.length()); }
    public SpannableStringBuilder insert(int where, CharSequence text, int start, int end) { return replace(where, where, text, start, end); }
    public SpannableStringBuilder insert(int where, CharSequence text) { return replace(where, where, text); }
    public SpannableStringBuilder delete(int st, int en) { return replace(st, en, "", 0, 0); }
    public void clear() { replace(0, b.length(), "", 0, 0); }
}
