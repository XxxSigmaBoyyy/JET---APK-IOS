package android.util;
public class TypedValue {
    public static final int TYPE_NULL = 0, TYPE_REFERENCE = 1, TYPE_ATTRIBUTE = 2, TYPE_STRING = 3, TYPE_FLOAT = 4,
        TYPE_DIMENSION = 5, TYPE_FRACTION = 6, TYPE_FIRST_INT = 16, TYPE_INT_DEC = 16, TYPE_INT_HEX = 17,
        TYPE_INT_BOOLEAN = 18, TYPE_FIRST_COLOR_INT = 28, TYPE_INT_COLOR_ARGB8 = 28, TYPE_INT_COLOR_RGB8 = 29,
        TYPE_INT_COLOR_ARGB4 = 30, TYPE_INT_COLOR_RGB4 = 31, TYPE_LAST_COLOR_INT = 31, TYPE_LAST_INT = 31;
    public static final int COMPLEX_UNIT_PX = 0, COMPLEX_UNIT_DIP = 1, COMPLEX_UNIT_SP = 2, COMPLEX_UNIT_PT = 3, COMPLEX_UNIT_IN = 4, COMPLEX_UNIT_MM = 5,
        COMPLEX_UNIT_FRACTION = 0, COMPLEX_UNIT_FRACTION_PARENT = 1, COMPLEX_UNIT_SHIFT = 0, COMPLEX_UNIT_MASK = 0xf,
        COMPLEX_RADIX_SHIFT = 4, COMPLEX_RADIX_MASK = 0x3, COMPLEX_RADIX_23p0 = 0, COMPLEX_RADIX_16p7 = 1, COMPLEX_RADIX_8p15 = 2, COMPLEX_RADIX_0p23 = 3,
        COMPLEX_MANTISSA_SHIFT = 8, COMPLEX_MANTISSA_MASK = 0xffffff, DENSITY_DEFAULT = 0, DENSITY_NONE = 0xffff, DATA_NULL_UNDEFINED = 0, DATA_NULL_EMPTY = 1;
    private static final float MANTISSA_MULT = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTS = { 1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT, 1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT };
    public static float complexToFloat(int complex) { return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT)) * RADIX_MULTS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK]; }
    public static float complexToDimension(int data, DisplayMetrics m) { return applyDimension((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK, complexToFloat(data), m); }
    public static int complexToDimensionPixelOffset(int data, DisplayMetrics m) { return (int) complexToDimension(data, m); }
    public static int complexToDimensionPixelSize(int data, DisplayMetrics m) {
        float value = complexToFloat(data), f = applyDimension((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK, value, m);
        int res = (int) (f >= 0 ? f + 0.5f : f - 0.5f); if (res != 0) return res; if (value == 0) return 0; return value > 0 ? 1 : -1;
    }
    public static float complexToFraction(int data, float base, float pbase) { float v = complexToFloat(data); return ((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK) == COMPLEX_UNIT_FRACTION_PARENT ? v * pbase : v * base; }
    /** AOSP TypedValue.floatToComplex. */
    public static int floatToComplex(float value) {
        if (value < (float) -0x7FFFFF - .5f || value >= (float) 0x7FFFFF + .5f) throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        int bits = Float.floatToIntBits(value); int sign = bits >>> 31; int exponent = (bits >>> 23) & 0xff; int mantissa = bits & 0x7fffff;
        if (exponent == 0 && mantissa == 0) return 0;
        long m = ((long) mantissa | 0x800000L); int shift = exponent - 127; int radix; int s2;
        if (shift < -23 + 23 - 23) { radix = COMPLEX_RADIX_0p23; s2 = shift + 23; }
        // pick the radix that keeps 23 bits of integer+fraction precision
        if (Math.abs(value) < 1f) { radix = COMPLEX_RADIX_0p23; } else if (Math.abs(value) < (1 << 8)) { radix = COMPLEX_RADIX_8p15; } else if (Math.abs(value) < (1 << 16)) { radix = COMPLEX_RADIX_16p7; } else { radix = COMPLEX_RADIX_23p0; }
        int frac = radix == COMPLEX_RADIX_0p23 ? 23 : radix == COMPLEX_RADIX_8p15 ? 15 : radix == COMPLEX_RADIX_16p7 ? 7 : 0;
        long mant = Math.round(Math.abs((double) value) * (1L << frac)); if (sign == 1) mant = -mant; mant &= COMPLEX_MANTISSA_MASK;
        return (int) ((mant << COMPLEX_MANTISSA_SHIFT) | ((long) radix << COMPLEX_RADIX_SHIFT));
    }
    public static int createComplexDimension(float value, int units) { return floatToComplex(value) | (units & COMPLEX_UNIT_MASK); }
    public float getDimension(DisplayMetrics m) { return complexToDimension(data, m); }
    public float getFraction(float base, float pbase) { return complexToFraction(data, base, pbase); }
    public int type, data, resourceId, assetCookie, changingConfigurations, density;
    public CharSequence string;
    public float getFloat() { return Float.intBitsToFloat(data); }
    public int getComplexUnit() { return COMPLEX_UNIT_MASK & (data >> COMPLEX_UNIT_SHIFT); }
    public static float applyDimension(int unit, float value, DisplayMetrics m) {
        switch (unit) { case COMPLEX_UNIT_DIP: return value * m.density; case COMPLEX_UNIT_SP: return value * m.scaledDensity; default: return value; }
    }
    public CharSequence coerceToString() { return string != null ? string : String.valueOf(data); }
}
