package jet.apk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Android binary resource primitives (ResourceTypes.h): string pools and Res_value formatting.
 *  Value formatting mirrors androguard's format_value so on-device bundles match jetc/apk2bundle.py output. */
public final class ResChunks {
    private ResChunks() {}
    public static final int RES_STRING_POOL = 0x0001, RES_TABLE = 0x0002, RES_XML = 0x0003;
    public static final int XML_START_NS = 0x0100, XML_END_NS = 0x0101, XML_START_ELEM = 0x0102, XML_END_ELEM = 0x0103,
            XML_CDATA = 0x0104, XML_RESOURCE_MAP = 0x0180;
    public static final int TABLE_PACKAGE = 0x0200, TABLE_TYPE = 0x0201, TABLE_TYPE_SPEC = 0x0202, TABLE_LIBRARY = 0x0203;

    public static final int TYPE_NULL = 0, TYPE_REFERENCE = 1, TYPE_ATTRIBUTE = 2, TYPE_STRING = 3, TYPE_FLOAT = 4,
            TYPE_DIMENSION = 5, TYPE_FRACTION = 6, TYPE_DYNAMIC_REFERENCE = 7, TYPE_DYNAMIC_ATTRIBUTE = 8,
            TYPE_INT_DEC = 0x10, TYPE_INT_HEX = 0x11, TYPE_INT_BOOLEAN = 0x12,
            TYPE_FIRST_COLOR = 0x1c, TYPE_LAST_COLOR = 0x1f, TYPE_FIRST_INT = 0x10, TYPE_LAST_INT = 0x1f;

    static ByteBuffer le(byte[] b) { return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN); }

    /** ResStringPool chunk at absolute offset {@code off}. Strings decoded lazily. */
    public static final class StringPool {
        final ByteBuffer b; final int count, flags, stringsStart, base; final boolean utf8; final String[] cache;
        public StringPool(ByteBuffer buf, int off) {
            b = buf; base = off;
            int hdr = b.getShort(off + 2) & 0xffff;
            count = b.getInt(off + 8); flags = b.getInt(off + 16);
            stringsStart = b.getInt(off + 20); utf8 = (flags & 0x100) != 0;
            cache = new String[Math.max(0, count)];
            idxBase = off + hdr;
        }
        final int idxBase;
        public int size() { return count; }
        public String get(int i) {
            if (i < 0 || i >= count) return "";
            String s = cache[i];
            if (s != null) return s;
            int p = base + stringsStart + b.getInt(idxBase + i * 4);
            try {
                if (utf8) {
                    int n = b.get(p) & 0xff; p++; if ((n & 0x80) != 0) p++;          // utf16 length (skipped)
                    int len = b.get(p) & 0xff; p++;
                    if ((len & 0x80) != 0) { len = ((len & 0x7f) << 8) | (b.get(p) & 0xff); p++; }
                    byte[] d = new byte[len]; for (int k = 0; k < len; k++) d[k] = b.get(p + k);
                    s = decodeMutf8(d);
                } else {
                    int len = b.getShort(p) & 0xffff; p += 2;
                    if ((len & 0x8000) != 0) { len = ((len & 0x7fff) << 16) | (b.getShort(p) & 0xffff); p += 2; }
                    char[] c = new char[len]; for (int k = 0; k < len; k++) c[k] = b.getChar(p + 2 * k);
                    s = new String(c);
                }
            } catch (RuntimeException e) { s = ""; }
            int z = s.indexOf('\0'); if (z >= 0) s = s.substring(0, z);   // aapt reads until NUL
            return cache[i] = s;
        }
        static String decodeMutf8(byte[] d) {
            // aapt2 writes standard UTF-8 (4-byte for supplementary); older aapt wrote CESU-8 surrogate pairs.
            try { return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(d)).toString(); }
            catch (java.nio.charset.CharacterCodingException e) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < d.length; ) {
                    int c = d[i] & 0xff;
                    if (c < 0x80) { sb.append((char) c); i++; }
                    else if ((c & 0xe0) == 0xc0 && i + 1 < d.length) { sb.append((char) (((c & 0x1f) << 6) | (d[i + 1] & 0x3f))); i += 2; }
                    else if ((c & 0xf0) == 0xe0 && i + 2 < d.length) { sb.append((char) (((c & 0x0f) << 12) | ((d[i + 1] & 0x3f) << 6) | (d[i + 2] & 0x3f))); i += 3; }
                    else if ((c & 0xf8) == 0xf0 && i + 3 < d.length) { sb.appendCodePoint(((c & 0x07) << 18) | ((d[i + 1] & 0x3f) << 12) | ((d[i + 2] & 0x3f) << 6) | (d[i + 3] & 0x3f)); i += 4; }
                    else { sb.append('\ufffd'); i++; }
                }
                return sb.toString();
            }
        }
    }

    static final double[] RADIX_MULTS = {0.00390625, 3.051758e-005, 1.192093e-007, 4.656613e-010};   // androguard literals (not exact 2^-n)
    static final String[] DIMENSION_UNITS = {"px", "dip", "sp", "pt", "in", "mm", "", ""};
    static final String[] FRACTION_UNITS = {"%", "%p", "", "", "", "", "", ""};

    static double complexToFloat(long x) { return (double) (x & 0xFFFFFF00L) * RADIX_MULTS[(int) ((x >> 4) & 3)]; }

    /** Python "%f" (6 decimals, round-half-even on the binary value as CPython does). */
    static String pyF(double v) { return new java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_EVEN).toPlainString(); }

    /** androguard.core.axml.format_value + apk2bundle.fmt (sign-extended 24-bit dimension mantissa). */
    public static String format(int type, int data, String str) {
        long u = data & 0xffffffffL;
        String pk = (u >>> 24) == 1 ? "android:" : "";
        switch (type) {
            case TYPE_STRING: return str;
            case TYPE_ATTRIBUTE: return "?" + pk + String.format("%08X", u);
            case TYPE_REFERENCE: return "@" + pk + String.format("%08X", u);
            case TYPE_FLOAT: return pyF(Float.intBitsToFloat(data));
            case TYPE_INT_HEX: return String.format("0x%08X", u);
            case TYPE_INT_BOOLEAN: return data == 0 ? "false" : "true";
            case TYPE_DIMENSION: {
                double v = complexToFloat(u);
                if (v >= 8388608) v -= 16777216;   // apk2bundle.fmt: un-wrapped negative dimension
                return pyF(v) + DIMENSION_UNITS[(int) (u & 0xf)];
            }
            case TYPE_FRACTION: return pyF(complexToFloat(u) * 100) + FRACTION_UNITS[(int) (u & 0xf)];
            default:
                if (type >= TYPE_FIRST_COLOR && type <= TYPE_LAST_COLOR) return String.format("#%08X", u);
                if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) return Long.toString(u > 0x7FFFFFFFL ? (u & 0x7FFFFFFFL) - 0x80000000L : u);
                return String.format("<0x%X, type 0x%02X>", u, type);
        }
    }
}
