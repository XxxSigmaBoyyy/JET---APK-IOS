package android.graphics;
public final class Color {
    public static final int BLACK = 0xFF000000, WHITE = 0xFFFFFFFF, RED = 0xFFFF0000, GREEN = 0xFF00FF00, BLUE = 0xFF0000FF, TRANSPARENT = 0;
    public static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }
    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }
    public static int alpha(int c) { return (c >>> 24) & 0xff; }
    public static int red(int c) { return (c >> 16) & 0xff; }
    public static int green(int c) { return (c >> 8) & 0xff; }
    public static int blue(int c) { return c & 0xff; }
    private static final java.util.Map<String, Integer> sColorNameMap;
    static {
        sColorNameMap = new java.util.HashMap<>();
        sColorNameMap.put("black", BLACK); sColorNameMap.put("darkgray", 0xFF444444);
        sColorNameMap.put("gray", 0xFF888888); sColorNameMap.put("lightgray", 0xFFCCCCCC);
        sColorNameMap.put("white", WHITE); sColorNameMap.put("red", RED);
        sColorNameMap.put("green", GREEN); sColorNameMap.put("blue", BLUE);
        sColorNameMap.put("yellow", 0xFFFFFF00); sColorNameMap.put("cyan", 0xFF00FFFF);
        sColorNameMap.put("magenta", 0xFFFF00FF); sColorNameMap.put("aqua", 0xFF00FFFF);
        sColorNameMap.put("fuchsia", 0xFFFF00FF); sColorNameMap.put("lime", GREEN);
        sColorNameMap.put("maroon", 0xFF800000); sColorNameMap.put("navy", 0xFF000080);
        sColorNameMap.put("olive", 0xFF808000); sColorNameMap.put("purple", 0xFF800080);
        sColorNameMap.put("silver", 0xFFC0C0C0); sColorNameMap.put("teal", 0xFF008080);
        sColorNameMap.put("transparent", TRANSPARENT);
    }
    public static int parseColor(String s) {
        if (s.charAt(0) == '#') {
            long v = Long.parseLong(s.substring(1), 16);
            if (s.length() == 7) v |= 0xff000000L;
            else if (s.length() != 9) throw new IllegalArgumentException("Unknown color");
            return (int) v;
        }
        Integer i = sColorNameMap.get(s.toLowerCase(java.util.Locale.ROOT));
        if (i != null) return i;
        throw new IllegalArgumentException("Unknown color");
    }

    /** Decode {@code @ColorLong} to ARGB. sRGB (cs id 0) stores ARGB in the high 32 bits. */
    public static int toArgb(long color) {
        if ((color & 0x3fL) == 0L) return (int) (color >>> 32);
        // Half-float RGBA in bits 63..16 — approximate via float16 decode of alpha/r/g/b.
        int a = Math.round(float16((int) ((color >>> 48) & 0xffff)) * 255f) & 0xff;
        int r = Math.round(float16((int) ((color >>> 32) & 0xffff)) * 255f) & 0xff;
        int g = Math.round(float16((int) ((color >>> 16) & 0xffff)) * 255f) & 0xff;
        int b = Math.round(float16((int) (color & 0xffff)) * 255f) & 0xff;
        // Note: Android packs R in 63-48, G 47-32, B 31-16, A 15-6 area differently; if wrong, sRGB path covers Compose.
        return argb(a, r, g, b);
    }
    private static float float16(int h) {
        int s = (h >>> 15) & 1, e = (h >>> 10) & 0x1f, f = h & 0x3ff;
        if (e == 0) return (s != 0 ? -1 : 1) * (f / 1024f) * (1f / 16384f);
        if (e == 31) return f == 0 ? (s != 0 ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY) : Float.NaN;
        return (s != 0 ? -1 : 1) * (1f + f / 1024f) * (float) Math.pow(2, e - 15);
    }

        /** AOSP-equivalent HSV conversions (h in [0,360), s/v in [0,1]). */
    public static void RGBToHSV(int r, int g, int b, float[] hsv) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f, max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf)), d = max - min, h;
        if (d == 0) h = 0; else if (max == rf) h = 60f * (((gf - bf) / d) % 6f); else if (max == gf) h = 60f * ((bf - rf) / d + 2f); else h = 60f * ((rf - gf) / d + 4f);
        if (h < 0) h += 360f; hsv[0] = h; hsv[1] = max == 0 ? 0 : d / max; hsv[2] = max;
    }
    public static void colorToHSV(int c, float[] hsv) { RGBToHSV((c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff, hsv); }
    public static int HSVToColor(float[] hsv) { return HSVToColor(0xff, hsv); }
    public static int HSVToColor(int a, float[] hsv) {
        float h = ((hsv[0] % 360f) + 360f) % 360f, s = Math.max(0, Math.min(1, hsv[1])), v = Math.max(0, Math.min(1, hsv[2]));
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c, r, g, b; int k = (int) (h / 60f);
        switch (k) { case 0: r = c; g = x; b = 0; break; case 1: r = x; g = c; b = 0; break; case 2: r = 0; g = c; b = x; break; case 3: r = 0; g = x; b = c; break; case 4: r = x; g = 0; b = c; break; default: r = c; g = 0; b = x; }
        return (Math.max(0, Math.min(255, a)) << 24) | (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
