package android.net;
import java.util.*;
/** RFC 2396 Uri (android.net.Uri semantics: lenient parse, lazily split, immutable). */
public abstract class Uri implements Comparable<Uri>, android.os.Parcelable {
    public static final Uri EMPTY = new StringUri("");
    public static Uri parse(String s) { if (s == null) throw new NullPointerException("uriString"); return new StringUri(s); }
    public static Uri fromFile(java.io.File f) { if (f == null) throw new NullPointerException("file"); return new StringUri("file://" + encode(f.getAbsolutePath(), "/")); }
    public static Uri fromParts(String scheme, String ssp, String fragment) { return new StringUri(scheme + ":" + encode(ssp, "/:@") + (fragment == null ? "" : "#" + encode(fragment))); }
    public static Uri withAppendedPath(Uri base, String seg) { return base.buildUpon().appendEncodedPath(seg).build(); }

    public abstract String toString();
    // ---- parsing (lenient, like Android's StringUri)
    private String part(int which) {
        String s = toString(); int hash = s.indexOf('#'); String frag = hash >= 0 ? s.substring(hash + 1) : null; String noFrag = hash >= 0 ? s.substring(0, hash) : s;
        int colon = noFrag.indexOf(':'); int slash = noFrag.indexOf('/'), q0 = noFrag.indexOf('?');
        boolean hasScheme = colon > 0 && (slash < 0 || colon < slash) && (q0 < 0 || colon < q0);
        String scheme = hasScheme ? noFrag.substring(0, colon) : null; String ssp = hasScheme ? noFrag.substring(colon + 1) : noFrag;
        if (which == 0) return scheme; if (which == 1) return ssp; if (which == 2) return frag;
        boolean hier = !hasScheme || ssp.startsWith("/");
        if (!hier) return null;
        int q = ssp.indexOf('?'); String query = q >= 0 ? ssp.substring(q + 1) : null; String hp = q >= 0 ? ssp.substring(0, q) : ssp;
        String auth = null, path = hp;
        if (hp.startsWith("//")) { int e = hp.indexOf('/', 2); auth = e >= 0 ? hp.substring(2, e) : hp.substring(2); path = e >= 0 ? hp.substring(e) : ""; }
        if (which == 3) return auth; if (which == 4) return path; if (which == 5) return query;
        return null;
    }
    public String getScheme() { return part(0); }
    public String getEncodedSchemeSpecificPart() { return part(1); } public String getSchemeSpecificPart() { return decode(part(1)); }
    public String getEncodedFragment() { return part(2); } public String getFragment() { return decode(part(2)); }
    public String getEncodedAuthority() { return part(3); } public String getAuthority() { return decode(part(3)); }
    public String getEncodedPath() { return part(4); } public String getPath() { return decode(part(4)); }
    public String getEncodedQuery() { return part(5); } public String getQuery() { return decode(part(5)); }
    public String getEncodedUserInfo() { String a = part(3); int at = a == null ? -1 : a.lastIndexOf('@'); return at < 0 ? null : a.substring(0, at); }
    public String getUserInfo() { return decode(getEncodedUserInfo()); }
    public String getHost() { String a = part(3); if (a == null) return null; int at = a.lastIndexOf('@'); String h = a.substring(at + 1); if (h.startsWith("[")) { int e = h.indexOf(']'); return e > 0 ? h.substring(0, e + 1) : h; } int c = h.lastIndexOf(':'); return decode(c >= 0 ? h.substring(0, c) : h); }
    public int getPort() { String a = part(3); if (a == null) return -1; int at = a.lastIndexOf('@'); String h = a.substring(at + 1); int c = h.lastIndexOf(':'); if (c < 0 || h.endsWith("]")) return -1; try { return Integer.parseInt(h.substring(c + 1)); } catch (NumberFormatException e) { return -1; } }
    public List<String> getPathSegments() { String p = part(4); ArrayList<String> r = new ArrayList<>(); if (p != null) for (String x : p.split("/")) if (!x.isEmpty()) r.add(decode(x)); return Collections.unmodifiableList(r); }
    public String getLastPathSegment() { List<String> s = getPathSegments(); return s.isEmpty() ? null : s.get(s.size() - 1); }
    public boolean isHierarchical() { String s = part(0); return s == null || part(1).startsWith("/"); }
    public boolean isOpaque() { return !isHierarchical(); }
    public boolean isRelative() { return part(0) == null; } public boolean isAbsolute() { return !isRelative(); }
    public Set<String> getQueryParameterNames() { String q = part(5); LinkedHashSet<String> r = new LinkedHashSet<>(); if (q != null) for (String kv : q.split("&")) if (!kv.isEmpty()) r.add(decode(kv.split("=", 2)[0])); return Collections.unmodifiableSet(r); }
    public List<String> getQueryParameters(String key) { String q = part(5); ArrayList<String> r = new ArrayList<>(); if (q != null) for (String kv : q.split("&")) { String[] p = kv.split("=", 2); if (decode(p[0]).equals(key)) r.add(p.length > 1 ? decode(p[1].replace('+', ' ')) : ""); } return Collections.unmodifiableList(r); }
    public String getQueryParameter(String key) { List<String> v = getQueryParameters(key); return v.isEmpty() ? null : v.get(0); }
    public boolean getBooleanQueryParameter(String key, boolean def) { String v = getQueryParameter(key); if (v == null) return def; v = v.toLowerCase(Locale.ROOT); return !("false".equals(v) || "0".equals(v)); }
    public Uri normalizeScheme() { String s = getScheme(); if (s == null || s.equals(s.toLowerCase(Locale.ROOT))) return this; return parse(s.toLowerCase(Locale.ROOT) + toString().substring(s.length())); }
    public Builder buildUpon() {
        Builder b = new Builder().scheme(getScheme());
        if (isHierarchical()) { b.encodedAuthority(getEncodedAuthority()).encodedPath(getEncodedPath()).encodedQuery(getEncodedQuery()); }
        else b.encodedOpaquePart(getEncodedSchemeSpecificPart());
        return b.encodedFragment(getEncodedFragment());
    }
    @Override public boolean equals(Object o) { return o instanceof Uri && toString().equals(o.toString()); }
    @Override public int hashCode() { return toString().hashCode(); }
    public int compareTo(Uri o) { return toString().compareTo(o.toString()); }
    public String toSafeString() { return toString(); }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
    public static void writeToParcel(android.os.Parcel p, Uri u) {}

    private static final String HEX = "0123456789ABCDEF";
    public static String encode(String s) { return encode(s, null); }
    public static String encode(String s, String allow) {
        if (s == null) return null; StringBuilder b = new StringBuilder();
        for (byte by : s.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            char c = (char) (by & 0xFF);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "_-!.~'()*".indexOf(c) >= 0 || (allow != null && allow.indexOf(c) >= 0)) b.append(c);
            else b.append('%').append(HEX.charAt((c >> 4) & 15)).append(HEX.charAt(c & 15));
        }
        return b.toString();
    }
    public static String decode(String s) {
        if (s == null) return null; if (s.indexOf('%') < 0) return s;
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c == '%' && i + 2 < s.length() + 0 && i + 2 <= s.length() - 1) { try { o.write(Integer.parseInt(s.substring(i + 1, i + 3), 16)); i += 2; continue; } catch (NumberFormatException ignored) {} } byte[] bb = String.valueOf(c).getBytes(java.nio.charset.StandardCharsets.UTF_8); o.write(bb, 0, bb.length); }
        return new String(o.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static final class StringUri extends Uri { final String s; StringUri(String s) { this.s = s; } public String toString() { return s; } }

    public static final class Builder {
        private String scheme, opaque, authority, path, query, fragment;
        public Builder() {}
        public Builder scheme(String s) { scheme = s; return this; }
        public Builder opaquePart(String o) { opaque = encode(o, "/:@"); return this; } public Builder encodedOpaquePart(String o) { opaque = o; return this; }
        public Builder authority(String a) { opaque = null; authority = encode(a, "@:[]"); return this; } public Builder encodedAuthority(String a) { opaque = null; authority = a; return this; }
        public Builder path(String p) { opaque = null; path = encode(p, "/"); return this; } public Builder encodedPath(String p) { opaque = null; path = p; return this; }
        public Builder appendPath(String seg) { return appendEncodedPath(encode(seg)); }
        public Builder appendEncodedPath(String seg) { opaque = null; if (path == null || path.isEmpty()) path = "/" + (seg.startsWith("/") ? seg.substring(1) : seg); else path = (path.endsWith("/") ? path : path + "/") + (seg.startsWith("/") ? seg.substring(1) : seg); return this; }
        public Builder query(String q) { opaque = null; query = encode(q, "=&"); return this; } public Builder encodedQuery(String q) { opaque = null; query = q; return this; }
        public Builder appendQueryParameter(String k, String v) { opaque = null; String kv = encode(k) + "=" + encode(v); query = query == null || query.isEmpty() ? kv : query + "&" + kv; return this; }
        public Builder clearQuery() { query = null; return this; }
        public Builder fragment(String f) { fragment = encode(f); return this; } public Builder encodedFragment(String f) { fragment = f; return this; }
        public Uri build() {
            StringBuilder b = new StringBuilder(); if (scheme != null) b.append(scheme).append(':');
            if (opaque != null) b.append(opaque);
            else { if (authority != null) b.append("//").append(authority); if (path != null) { if (authority != null && !path.isEmpty() && !path.startsWith("/")) b.append('/'); b.append(path); } if (query != null) b.append('?').append(query); }
            if (fragment != null) b.append('#').append(fragment);
            return new StringUri(b.toString());
        }
        @Override public String toString() { return build().toString(); }
    }
}
