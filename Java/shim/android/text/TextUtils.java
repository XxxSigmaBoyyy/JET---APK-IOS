package android.text;
public class TextUtils {
    public static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }
    public static String join(CharSequence delim, Iterable<?> tokens) {
        StringBuilder b = new StringBuilder(); boolean first = true;
        for (Object t : tokens) { if (!first) b.append(delim); b.append(t); first = false; }
        return b.toString();
    }
    public static CharSequence concat(CharSequence... t) {
        StringBuilder b = new StringBuilder(); for (CharSequence s : t) if (s != null) b.append(s); return b;
    }
    public enum TruncateAt { START, MIDDLE, END, MARQUEE, END_SMALL }
    public static final int CAP_MODE_CHARACTERS = 4096, CAP_MODE_WORDS = 8192, CAP_MODE_SENTENCES = 16384;
    public static boolean equals(CharSequence a, CharSequence b) { if (a == b) return true; if (a == null || b == null || a.length() != b.length()) return false; return a.toString().equals(b.toString()); }
    public static String join(CharSequence d, Object[] t) { StringBuilder b = new StringBuilder(); for (int i = 0; i < t.length; i++) { if (i > 0) b.append(d); b.append(t[i]); } return b.toString(); }
    public static String[] split(String t, String re) { return t.isEmpty() ? new String[0] : t.split(re, -1); }
    public static int getTrimmedLength(CharSequence s) { return s.toString().trim().length(); }
    public static boolean isDigitsOnly(CharSequence s) { for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false; return true; }
    public static boolean isGraphic(CharSequence s) { for (int i = 0; i < s.length(); i++) if (!Character.isWhitespace(s.charAt(i))) return true; return false; }
    public static String htmlEncode(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    public static CharSequence ellipsize(CharSequence t, TextPaint p, float avail, TruncateAt w) { return t; }
    public static String substring(CharSequence s, int a, int b) { return s.toString().substring(a, b); }
    public static int indexOf(CharSequence s, char c) { return s.toString().indexOf(c); }
    public static int indexOf(CharSequence s, char c, int from) { return s.toString().indexOf(c, from); }
    public static int indexOf(CharSequence s, CharSequence needle) { return indexOf(s, needle, 0); }
    public static int indexOf(CharSequence s, CharSequence needle, int from) {
        if (s == null || needle == null) return -1;
        return s.toString().indexOf(needle.toString(), from);
    }
    public static int lastIndexOf(CharSequence s, char c) { return s.toString().lastIndexOf(c); }
    public static int lastIndexOf(CharSequence s, char c, int from) { return s.toString().lastIndexOf(c, from); }
    public static CharSequence stringOrSpannedString(CharSequence s) { return s == null ? null : s instanceof Spanned ? new SpannedString(s) : s.toString(); }
    public static int getLayoutDirectionFromLocale(java.util.Locale l) { if (l == null || l.equals(java.util.Locale.ROOT)) return 0; String s = l.getScript(); if ("Arab".equals(s) || "Hebr".equals(s) || "Thaa".equals(s) || "Syrc".equals(s) || "Nkoo".equals(s)) return 1; if (!s.isEmpty()) return 0; switch (l.getLanguage()) { case "ar": case "fa": case "he": case "iw": case "ur": case "ps": case "sd": case "ug": case "yi": case "ji": case "dv": case "ckb": case "ku": return 1; default: return 0; } }
    public static int getCapsMode(CharSequence c, int off, int reqModes) { return 0; }
    public static void copySpansFrom(Spanned src, int s, int e, Class k, Spannable dst, int off) {}
    public static boolean regionMatches(CharSequence a, int ao, CharSequence b, int bo, int len) { return a.toString().regionMatches(ao, b.toString(), bo, len); }
    public static CharSequence commaEllipsize(CharSequence t, TextPaint p, float a, String one, String more) { return t; }
    public static String expandTemplate(CharSequence t, CharSequence... v) { String r = t.toString(); for (int i = 0; i < v.length; i++) r = r.replace("^" + (i + 1), v[i]); return r; }
}
