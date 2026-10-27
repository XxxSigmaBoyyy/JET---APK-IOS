package jet.apk;

import java.nio.ByteBuffer;
import java.util.*;

/** resources.arsc reader (ResTable / ResTable_package / ResTable_type, sparse + offset16 + compact entries).
 *  Selection semantics mirror androguard ARSCParser as used by jetc/apk2bundle.py:
 *  public ids = entries present in a default-locale config; configs per id kept in table order. */
public final class Arsc {
    public static final class Config {
        public final int mcc, mnc, language, country, orientation, touchscreen, density, keyboard, navigation, inputFlags,
                screenWidth, screenHeight, sdkVersion, minorVersion, screenLayout, uiMode, smallestScreenWidthDp,
                screenWidthDp, screenHeightDp, screenLayout2, colorMode; final byte[] raw;
        Config(ByteBuffer b, int off) {
            int size = b.getInt(off);
            raw = new byte[Math.max(0, size - 4)];
            for (int i = 0; i < raw.length; i++) raw[i] = b.get(off + 4 + i);
            mcc = u16(4); mnc = u16(6); language = u16(8); country = u16(10);
            orientation = u8(12); touchscreen = u8(13); density = u16(14);
            keyboard = u8(16); navigation = u8(17); inputFlags = u8(18);
            screenWidth = u16(20); screenHeight = u16(22); sdkVersion = u16(24); minorVersion = u16(26);
            screenLayout = u8(28); uiMode = u8(29); smallestScreenWidthDp = u16(30);
            screenWidthDp = u16(32); screenHeightDp = u16(34);
            screenLayout2 = u8(48); colorMode = u8(49);
        }
        private int u8(int o) { o -= 4; return o < raw.length ? raw[o] & 0xff : 0; }
        private int u16(int o) { o -= 4; return o + 1 < raw.length ? (raw[o] & 0xff) | ((raw[o + 1] & 0xff) << 8) : 0; }
        /** True when only sdkVersion/minorVersion may be set (unqualified or bare -vNN). */
        public boolean versionOnly() {
            for (int i = 0; i < raw.length; i++) { int o = i + 4; if (o >= 24 && o < 28) continue; if (raw[i] != 0) return false; }
            return true;
        }
        public boolean defaultLocale() { return language == 0 && country == 0; }
        /** apk2bundle DENS lookup of this config's density qualifier (160 when absent/unknown). */
        public int densQualifier() {
            switch (density) {
                case 120: case 160: case 240: case 320: case 480: case 640: return density;
                case 0xFFFF: return 0;   // nodpi
                case 0xFFFE: return 1;   // anydpi
                default: return 160;
            }
        }
        @Override public boolean equals(Object o) { return o instanceof Config && Arrays.equals(raw, ((Config) o).raw); }
        @Override public int hashCode() { return Arrays.hashCode(raw); }
    }

    /** One Res_value or bag entry. */
    public static final class Entry {
        public final int id; public final String key; public final boolean complex;
        public final int type, data;            // simple
        public final int parent; public final int[] bagKeys, bagTypes, bagData;   // complex
        final Arsc t;
        Entry(Arsc t, int id, String key, int type, int data) { this.t = t; this.id = id; this.key = key; complex = false; this.type = type; this.data = data; parent = 0; bagKeys = bagTypes = bagData = null; }
        Entry(Arsc t, int id, String key, int parent, int[] k, int[] ty, int[] d) { this.t = t; this.id = id; this.key = key; complex = true; type = 0; data = 0; this.parent = parent; bagKeys = k; bagTypes = ty; bagData = d; }
        /** apk2bundle fmt(e.key) */
        public String fmt() { return ResChunks.format(type, data, type == ResChunks.TYPE_STRING ? t.strings.get(data) : null); }
        public String fmtBag(int i) { return ResChunks.format(bagTypes[i], bagData[i], bagTypes[i] == ResChunks.TYPE_STRING ? t.strings.get(bagData[i]) : null); }
    }

    public ResChunks.StringPool strings;
    public String packageName = ""; public int packageId;
    /** id -> (config -> entry), insertion-ordered as in the table. */
    public final Map<Integer, LinkedHashMap<Config, Entry>> values = new HashMap<>();
    /** public ids (default-locale) -> {type, name}, insertion order */
    public final LinkedHashMap<Integer, String[]> pub = new LinkedHashMap<>();
    final Map<Integer, String> typeNames = new HashMap<>();

    public Arsc(byte[] data) { this(data, null); }
    /** @param onlyPackage parse only the package with this name (null: first package) */
    public Arsc(byte[] data, String onlyPackage) {
        ByteBuffer b = ResChunks.le(data);
        int hdr = b.getShort(2) & 0xffff, end = Math.min(data.length, b.getInt(4));
        int p = hdr; boolean pkgDone = false;
        while (p + 8 <= end) {
            int type = b.getShort(p) & 0xffff, size = b.getInt(p + 4);
            if (size <= 0) break;
            if (type == ResChunks.RES_STRING_POOL && strings == null) strings = new ResChunks.StringPool(b, p);
            else if (type == ResChunks.TABLE_PACKAGE && !pkgDone) {
                StringBuilder nm = new StringBuilder();
                for (int i = 0; i < 128; i++) { char c = b.getChar(p + 12 + 2 * i); if (c == 0) break; nm.append(c); }
                if (onlyPackage == null || onlyPackage.equals(nm.toString())) { parsePackage(b, p, size, nm.toString()); pkgDone = true; }
            }
            p += size;
        }
    }

    private void parsePackage(ByteBuffer b, int p, int size, String name) {
        packageName = name; packageId = b.getInt(p + 8);
        int phdr = b.getShort(p + 2) & 0xffff;
        ResChunks.StringPool typeStr = new ResChunks.StringPool(b, p + b.getInt(p + 268));
        ResChunks.StringPool keyStr = new ResChunks.StringPool(b, p + b.getInt(p + 276));
        int end = p + size, q = p + phdr;
        while (q + 8 <= end) {
            int t = b.getShort(q) & 0xffff, csz = b.getInt(q + 4);
            if (csz <= 0 || q + csz > end) break;
            if (t == ResChunks.TABLE_TYPE) parseType(b, q, typeStr, keyStr);
            q += csz;
        }
    }

    private void parseType(ByteBuffer b, int q, ResChunks.StringPool typeStr, ResChunks.StringPool keyStr) {
        int hsz = b.getShort(q + 2) & 0xffff;
        int tid = b.get(q + 8) & 0xff, flags = b.get(q + 9) & 0xff;
        int count = b.getInt(q + 12), entriesStart = b.getInt(q + 16);
        Config cfg = new Config(b, q + 20);
        String tname = typeStr.get(tid - 1); typeNames.put(tid, tname);
        boolean sparse = (flags & 1) != 0, off16 = (flags & 2) != 0;
        int idx0 = q + hsz;
        for (int i = 0; i < count; i++) {
            int ei, off;
            if (sparse) { ei = b.getShort(idx0 + 4 * i) & 0xffff; off = (b.getShort(idx0 + 4 * i + 2) & 0xffff) * 4; }
            else if (off16) { ei = i; int o = b.getShort(idx0 + 2 * i) & 0xffff; if (o == 0xffff) continue; off = o * 4; }
            else { ei = i; off = b.getInt(idx0 + 4 * i); if (off == -1) continue; }
            int e = q + entriesStart + off;
            int id = (packageId << 24) | (tid << 16) | ei;
            int esize = b.getShort(e) & 0xffff, eflags = b.getShort(e + 2) & 0xffff;
            Entry en;
            if ((eflags & 0x8) != 0) {   // FLAG_COMPACT: key index in size field, type in flags high byte
                String key = keyStr.get(esize);
                en = new Entry(this, id, key, eflags >>> 8, b.getInt(e + 4));
            } else {
                String key = keyStr.get(b.getInt(e + 4));
                if ((eflags & 0x1) != 0) {
                    int parent = b.getInt(e + 8), n = b.getInt(e + 12);
                    int[] k = new int[n], ty = new int[n], d = new int[n];
                    int m = e + esize;
                    for (int j = 0; j < n; j++, m += 12) { k[j] = b.getInt(m); ty[j] = b.get(m + 7) & 0xff; d[j] = b.getInt(m + 8); }
                    en = new Entry(this, id, key, parent, k, ty, d);
                } else {
                    int v = e + esize;
                    en = new Entry(this, id, key, b.get(v + 3) & 0xff, b.getInt(v + 4));
                }
            }
            values.computeIfAbsent(id, z -> new LinkedHashMap<>()).put(cfg, en);
            if (cfg.defaultLocale() && !pub.containsKey(id)) pub.put(id, new String[]{tname, en.key});
        }
    }

    public String typeName(int id) { return typeNames.get((id >>> 16) & 0xff); }

    /** apk2bundle default_cfg: highest bare -vNN <= sdk among version-only configs, else the first config. */
    public Map.Entry<Config, Entry> defaultCfg(int id, int sdk) {
        LinkedHashMap<Config, Entry> cs = values.get(id);
        if (cs == null || cs.isEmpty()) return null;
        Map.Entry<Config, Entry> best = null; int bv = -1;
        for (Map.Entry<Config, Entry> c : cs.entrySet()) {
            if (!c.getKey().versionOnly()) continue;
            int v = c.getKey().sdkVersion;
            if (v <= sdk && v > bv) { bv = v; best = c; }
        }
        return best != null ? best : cs.entrySet().iterator().next();
    }
    public Collection<Map.Entry<Config, Entry>> configs(int id) {
        LinkedHashMap<Config, Entry> cs = values.get(id);
        return cs == null ? Collections.emptyList() : cs.entrySet();
    }
}
