package android.util;
public class Base64 {
    public static final int DEFAULT = 0, NO_PADDING = 1, NO_WRAP = 2, CRLF = 4, URL_SAFE = 8, NO_CLOSE = 16;
    private static java.util.Base64.Encoder enc(int f) { java.util.Base64.Encoder e = (f & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : ((f & NO_WRAP) != 0 ? java.util.Base64.getEncoder() : java.util.Base64.getMimeEncoder(76, (f & CRLF) != 0 ? "\r\n".getBytes() : "\n".getBytes())); return (f & NO_PADDING) != 0 ? e.withoutPadding() : e; }
    public static byte[] encode(byte[] in, int f) { byte[] b = enc(f).encode(in); if ((f & NO_WRAP) == 0 && (f & URL_SAFE) == 0 && b.length > 0) { byte[] n = java.util.Arrays.copyOf(b, b.length + 1); n[b.length] = '\n'; return n; } return b; }
    public static String encodeToString(byte[] in, int f) { return new String(encode(in, f)); }
    public static byte[] encode(byte[] in, int off, int len, int f) { return encode(java.util.Arrays.copyOfRange(in, off, off + len), f); }
    public static String encodeToString(byte[] in, int off, int len, int f) { return new String(encode(in, off, len, f)); }
    public static byte[] decode(String s, int f) { return decode(s.getBytes(), f); }
    public static byte[] decode(byte[] in, int f) { String s = new String(in).replaceAll("\\s", ""); return ((f & URL_SAFE) != 0 ? java.util.Base64.getUrlDecoder() : java.util.Base64.getDecoder()).decode(s); }
}
