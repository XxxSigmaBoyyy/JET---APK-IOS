package android.text;
public interface InputFilter {
    CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend);
    class LengthFilter implements InputFilter {
        private final int max; public LengthFilter(int max) { this.max = max; } public int getMax() { return max; }
        public CharSequence filter(CharSequence s, int st, int en, Spanned d, int ds, int de) {
            // AOSP LengthFilter: keep at most max chars in dest after the edit; never split a surrogate pair
            int keep = max - (d.length() - (de - ds));
            if (keep <= 0) return "";
            if (keep >= en - st) return null;
            keep += st;
            if (Character.isHighSurrogate(s.charAt(keep - 1))) { --keep; if (keep == st) return ""; }
            return s.subSequence(st, keep);
        }
    }
    class AllCaps implements InputFilter {
        public AllCaps() {} public AllCaps(java.util.Locale l) {}
        public CharSequence filter(CharSequence s, int st, int en, Spanned d, int ds, int de) { return s.subSequence(st, en).toString().toUpperCase(); }
    }
}
