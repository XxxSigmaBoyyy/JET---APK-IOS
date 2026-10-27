package android.text;
/** AOSP android.text.Html subset: tags b/strong, i/em, u, a href, br, p/div (paragraphs), font color ignored; HTML entities.
 *  Span classes are created reflectively (they may be SDK auto-stubs in this runtime). */
public class Html {
    public static final int FROM_HTML_MODE_LEGACY = 0, FROM_HTML_MODE_COMPACT = 63, FROM_HTML_OPTION_USE_CSS_COLORS = 256,
        FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH = 1, FROM_HTML_SEPARATOR_LINE_BREAK_HEADING = 2, FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM = 4,
        FROM_HTML_SEPARATOR_LINE_BREAK_LIST = 8, FROM_HTML_SEPARATOR_LINE_BREAK_DIV = 16, FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE = 32,
        TO_HTML_PARAGRAPH_LINES_CONSECUTIVE = 0, TO_HTML_PARAGRAPH_LINES_INDIVIDUAL = 1;
    public interface ImageGetter { android.graphics.drawable.Drawable getDrawable(String source); }
    public interface TagHandler { void handleTag(boolean opening, String tag, Editable output, org.xml.sax.XMLReader xmlReader); }
    private Html() {}
    @Deprecated public static Spanned fromHtml(String s) { return fromHtml(s, FROM_HTML_MODE_LEGACY, null, null); }
    public static Spanned fromHtml(String s, int flags) { return fromHtml(s, flags, null, null); }
    @Deprecated public static Spanned fromHtml(String s, ImageGetter ig, TagHandler th) { return fromHtml(s, FROM_HTML_MODE_LEGACY, ig, th); }
    public static Spanned fromHtml(String src, int flags, ImageGetter ig, TagHandler th) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        if (src == null) return out;
        java.util.ArrayDeque<Object[]> open = new java.util.ArrayDeque<>();
        String paraSep = (flags & FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH) != 0 ? "\n" : "\n\n";
        int i = 0, n = src.length(); boolean lastWs = false;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '<') {
                int e = src.indexOf('>', i); if (e < 0) e = n - 1;
                String tag = src.substring(i + 1, e).trim(); i = e + 1;
                if (tag.startsWith("!")) continue;
                boolean close = tag.startsWith("/"); if (close) tag = tag.substring(1).trim();
                String name = tag.split("[\\s/]", 2)[0].toLowerCase(java.util.Locale.ROOT);
                if (name.equals("br")) { out.append('\n'); lastWs = true; continue; }
                if (name.equals("p") || name.equals("div") || name.matches("h[1-6]")) {
                    if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append(close ? paraSep : "\n"); lastWs = true; continue;
                }
                if (!close) {
                    Object span = null;
                    switch (name) {
                        case "b": case "strong": span = span("android.text.style.StyleSpan", int.class, 1); break;
                        case "i": case "em": case "cite": case "dfn": span = span("android.text.style.StyleSpan", int.class, 2); break;
                        case "u": span = span("android.text.style.UnderlineSpan", null, null); break;
                        case "a": span = span("android.text.style.URLSpan", String.class, attr(tag, "href")); break;
                        default: if (th != null) th.handleTag(true, name, out, null);
                    }
                    if (!tag.endsWith("/")) open.push(new Object[]{name, out.length(), span});
                } else {
                    for (java.util.Iterator<Object[]> it = open.iterator(); it.hasNext(); ) {
                        Object[] o = it.next();
                        if (o[0].equals(name)) { it.remove(); if (o[2] != null && (int) o[1] < out.length()) out.setSpan(o[2], (int) o[1], out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); else if (o[2] == null && th != null) th.handleTag(false, name, out, null); break; }
                    }
                }
                continue;
            }
            if (c == '&') {
                int e = src.indexOf(';', i);
                if (e > i && e - i <= 10) { String ent = src.substring(i + 1, e); String r = entity(ent); if (r != null) { out.append(r); i = e + 1; lastWs = false; continue; } }
            }
            if (Character.isWhitespace(c)) { if (!lastWs) out.append(' '); lastWs = true; i++; continue; }
            out.append(c); lastWs = false; i++;
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') out.delete(out.length() - 1, out.length());
        return out;
    }
    private static String attr(String tag, String a) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b" + a + "\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))").matcher(tag);
        return m.find() ? (m.group(2) != null ? m.group(2) : m.group(3) != null ? m.group(3) : m.group(4)) : null;
    }
    private static String entity(String e) {
        switch (e) { case "amp": return "&"; case "lt": return "<"; case "gt": return ">"; case "quot": return "\""; case "apos": return "'"; case "nbsp": return "\u00a0"; }
        try { if (e.startsWith("#x") || e.startsWith("#X")) return new String(Character.toChars(Integer.parseInt(e.substring(2), 16))); if (e.startsWith("#")) return new String(Character.toChars(Integer.parseInt(e.substring(1)))); } catch (RuntimeException ignored) {}
        return null;
    }
    private static Object span(String cls, Class<?> pt, Object arg) {
        try { Class<?> k = Class.forName(cls, true, Thread.currentThread().getContextClassLoader() != null ? Thread.currentThread().getContextClassLoader() : Html.class.getClassLoader());
              return pt == null ? k.getConstructor().newInstance() : k.getConstructor(pt).newInstance(arg); } catch (Throwable t) { return null; }
    }
    public static String escapeHtml(CharSequence t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.length(); i++) { char c = t.charAt(i); if (c == '<') b.append("&lt;"); else if (c == '>') b.append("&gt;"); else if (c == '&') b.append("&amp;"); else if (c > 0x7e || c < ' ') b.append("&#").append((int) c).append(';'); else b.append(c); }
        return b.toString();
    }
    @Deprecated public static String toHtml(Spanned t) { return toHtml(t, TO_HTML_PARAGRAPH_LINES_CONSECUTIVE); }
    public static String toHtml(Spanned t, int option) { return "<p dir=\"ltr\">" + escapeHtml(t).replace("\n", "<br>\n") + "</p>\n"; }
}
