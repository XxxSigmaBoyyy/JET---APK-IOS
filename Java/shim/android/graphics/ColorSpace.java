package android.graphics;
/** Named ColorSpace instances with the AOSP ids, models, primaries, white points and transfer parameters
 *  (identity-comparable singletons, as ColorSpace.get() returns on Android). Colour conversion is not performed here. */
public abstract class ColorSpace {
    public enum Named { SRGB, LINEAR_SRGB, EXTENDED_SRGB, LINEAR_EXTENDED_SRGB, BT709, BT2020, DCI_P3, DISPLAY_P3, NTSC_1953, SMPTE_C, ADOBE_RGB, PRO_PHOTO, ACES, ACESCG, CIE_XYZ, CIE_LAB, BT2020_HLG, BT2020_PQ }
    public enum Model { RGB(3), XYZ(3), LAB(3), CMYK(4); private final int c; Model(int c) { this.c = c; } public int getComponentCount() { return c; } }
    public enum RenderIntent { PERCEPTUAL, RELATIVE, SATURATION, ABSOLUTE }
    public enum Adaptation { BRADFORD, VON_KRIES, CIECAT02 }
    public static final float[] ILLUMINANT_A = {0.44757f, 0.40745f}, ILLUMINANT_B = {0.34842f, 0.35161f}, ILLUMINANT_C = {0.31006f, 0.31616f},
        ILLUMINANT_D50 = {0.34567f, 0.35850f}, ILLUMINANT_D55 = {0.33242f, 0.34743f}, ILLUMINANT_D60 = {0.32168f, 0.33767f}, ILLUMINANT_D65 = {0.31271f, 0.32902f},
        ILLUMINANT_D75 = {0.29902f, 0.31485f}, ILLUMINANT_E = {0.33333f, 0.33333f};
    public static final int MIN_ID = -1, MAX_ID = 63;
    private final String mName; private final Model mModel; private final int mId;
    ColorSpace(String name, Model model, int id) { mName = name; mModel = model; mId = id; }
    public String getName() { return mName; } public int getId() { return mId; } public Model getModel() { return mModel; }
    public int getComponentCount() { return mModel.getComponentCount(); }
    public boolean isSrgb() { return false; } public abstract boolean isWideGamut();
    public abstract float getMinValue(int component); public abstract float getMaxValue(int component);
    @Override public String toString() { return mName + " (id=" + mId + ", model=" + mModel + ")"; }
    private static final ColorSpace[] NAMED = new ColorSpace[Named.values().length];
    private static final float[] SRGB_P = {0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f};
    static {
        Rgb.TransferParameters srgbTf = new Rgb.TransferParameters(1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4);
        Rgb.TransferParameters lin = new Rgb.TransferParameters(1, 0, 0, 0, 1);
        Rgb.TransferParameters bt709Tf = new Rgb.TransferParameters(1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, 1 / 0.45);
        Rgb.TransferParameters g22 = new Rgb.TransferParameters(1, 0, 0, 0, 2.2);
        put(Named.SRGB, new Rgb("sRGB IEC61966-2.1", SRGB_P, ILLUMINANT_D65, srgbTf, 0, 0f, 1f, true));
        put(Named.LINEAR_SRGB, new Rgb("sRGB IEC61966-2.1 (Linear)", SRGB_P, ILLUMINANT_D65, lin, 1, 0f, 1f, false));
        put(Named.EXTENDED_SRGB, new Rgb("scRGB-nl IEC 61966-2-2:2003", SRGB_P, ILLUMINANT_D65, srgbTf, 2, -0.799f, 2.399f, false));
        put(Named.LINEAR_EXTENDED_SRGB, new Rgb("scRGB IEC 61966-2-2:2003", SRGB_P, ILLUMINANT_D65, lin, 3, -0.5f, 7.499f, false));
        put(Named.BT709, new Rgb("Rec. ITU-R BT.709-5", new float[]{0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f}, ILLUMINANT_D65, bt709Tf, 4, 0f, 1f, false));
        put(Named.BT2020, new Rgb("Rec. ITU-R BT.2020-1", new float[]{0.708f, 0.292f, 0.170f, 0.797f, 0.131f, 0.046f}, ILLUMINANT_D65, new Rgb.TransferParameters(1 / 1.0993, 0.0993 / 1.0993, 1 / 4.5, 0.08145, 1 / 0.45), 5, 0f, 1f, false));
        put(Named.DCI_P3, new Rgb("SMPTE RP 431-2-2007 DCI (P3)", new float[]{0.680f, 0.320f, 0.265f, 0.690f, 0.150f, 0.060f}, new float[]{0.314f, 0.351f}, new Rgb.TransferParameters(1, 0, 0, 0, 2.6), 6, 0f, 1f, false));
        put(Named.DISPLAY_P3, new Rgb("Display P3", new float[]{0.680f, 0.320f, 0.265f, 0.690f, 0.150f, 0.060f}, ILLUMINANT_D65, srgbTf, 7, 0f, 1f, false));
        put(Named.NTSC_1953, new Rgb("NTSC (1953)", new float[]{0.67f, 0.33f, 0.21f, 0.71f, 0.14f, 0.08f}, ILLUMINANT_C, bt709Tf, 8, 0f, 1f, false));
        put(Named.SMPTE_C, new Rgb("SMPTE-C RGB", new float[]{0.630f, 0.340f, 0.310f, 0.595f, 0.155f, 0.070f}, ILLUMINANT_D65, bt709Tf, 9, 0f, 1f, false));
        put(Named.ADOBE_RGB, new Rgb("Adobe RGB (1998)", new float[]{0.64f, 0.33f, 0.21f, 0.71f, 0.15f, 0.06f}, ILLUMINANT_D65, g22, 10, 0f, 1f, false));
        put(Named.PRO_PHOTO, new Rgb("ROMM RGB ISO 22028-2:2013", new float[]{0.7347f, 0.2653f, 0.1596f, 0.8404f, 0.0366f, 0.0001f}, ILLUMINANT_D50, new Rgb.TransferParameters(1, 0, 1 / 16.0, 0.031248, 1.8), 11, 0f, 1f, false));
        put(Named.ACES, new Rgb("SMPTE ST 2065-1:2012 ACES", new float[]{0.73470f, 0.26530f, 0.0f, 1.0f, 0.00010f, -0.0770f}, ILLUMINANT_D60, lin, 12, -65504f, 65504f, false));
        put(Named.ACESCG, new Rgb("Academy S-2014-004 ACEScg", new float[]{0.713f, 0.293f, 0.165f, 0.830f, 0.128f, 0.044f}, ILLUMINANT_D60, lin, 13, -65504f, 65504f, false));
        put(Named.CIE_XYZ, new Simple("Generic XYZ", Model.XYZ, 14, -2f, 2f));
        put(Named.CIE_LAB, new Simple("Generic L*a*b*", Model.LAB, 15, -128f, 128f));
        put(Named.BT2020_HLG, new Rgb("Hybrid Log Gamma encoding", new float[]{0.708f, 0.292f, 0.170f, 0.797f, 0.131f, 0.046f}, ILLUMINANT_D65, new Rgb.TransferParameters(2.0, 2.0, 1 / 0.17883277, 0.28466892, 0.55991073, -0.685490157, -3.0), 16, 0f, 1f, false));
        put(Named.BT2020_PQ, new Rgb("Perceptual Quantizer encoding", new float[]{0.708f, 0.292f, 0.170f, 0.797f, 0.131f, 0.046f}, ILLUMINANT_D65, new Rgb.TransferParameters(-1.555223, 1.860454, 32 / 2523.0, 2413 / 128.0, -2392 / 128.0, 8192 / 1305.0, -2.0), 17, 0f, 1f, false));
    }
    private static void put(Named n, ColorSpace c) { NAMED[n.ordinal()] = c; }
    public static ColorSpace get(Named name) { return NAMED[name.ordinal()]; }
    public static ColorSpace getFromDataSpace(int dataSpace) { switch (dataSpace) { case 142671872: return get(Named.SRGB); case 143261696: return get(Named.DISPLAY_P3); case 138477568: return get(Named.LINEAR_EXTENDED_SRGB); default: return null; } }
    public int getDataSpace() { switch (mId) { case 0: return 142671872; case 7: return 143261696; case 3: return 138477568; default: return 0; } }
    public static ColorSpace match(float[] toXYZD50, Rgb.TransferParameters function) { return null; }
    static final class Simple extends ColorSpace {
        private final float mMin, mMax;
        Simple(String n, Model m, int id, float min, float max) { super(n, m, id); mMin = min; mMax = max; }
        public boolean isWideGamut() { return true; }
        public float getMinValue(int c) { return c == 0 && getModel() == Model.LAB ? 0f : mMin; }
        public float getMaxValue(int c) { return c == 0 && getModel() == Model.LAB ? 100f : mMax; }
    }
    public static class Rgb extends ColorSpace {
        public static class TransferParameters {
            public final double a, b, c, d, e, f, g;
            public TransferParameters(double a, double b, double c, double d, double g) { this(a, b, c, d, 0, 0, g); }
            public TransferParameters(double a, double b, double c, double d, double e, double f, double g) { this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f; this.g = g; }
            @Override public boolean equals(Object o) { if (!(o instanceof TransferParameters)) return false; TransferParameters t = (TransferParameters) o; return a == t.a && b == t.b && c == t.c && d == t.d && e == t.e && f == t.f && g == t.g; }
            @Override public int hashCode() { return java.util.Objects.hash(a, b, c, d, e, f, g); }
        }
        private final float[] mPrimaries, mWhite; private final TransferParameters mTf; private final float mMin, mMax; private final boolean mSrgb;
        Rgb(String name, float[] primaries, float[] white, TransferParameters tf, int id, float min, float max, boolean srgb) { super(name, Model.RGB, id); mPrimaries = primaries; mWhite = white; mTf = tf; mMin = min; mMax = max; mSrgb = srgb; }
        public Rgb(String name, float[] primaries, float[] whitePoint, TransferParameters function) { this(name, primaries.length == 9 ? xyFromXyz(primaries) : primaries, whitePoint.length == 3 ? new float[]{whitePoint[0] / (whitePoint[0] + whitePoint[1] + whitePoint[2]), whitePoint[1] / (whitePoint[0] + whitePoint[1] + whitePoint[2])} : whitePoint, function, -1, 0f, 1f, false); }
        public Rgb(String name, float[] primaries, float[] whitePoint, double gamma) { this(name, primaries, whitePoint, new TransferParameters(1, 0, 0, 0, gamma)); }
        private static float[] xyFromXyz(float[] p) { float[] r = new float[6]; for (int i = 0; i < 3; i++) { float s = p[i * 3] + p[i * 3 + 1] + p[i * 3 + 2]; r[i * 2] = p[i * 3] / s; r[i * 2 + 1] = p[i * 3 + 1] / s; } return r; }
        public float[] getPrimaries() { return mPrimaries.clone(); } public float[] getPrimaries(float[] out) { System.arraycopy(mPrimaries, 0, out, 0, 6); return out; }
        public float[] getWhitePoint() { return mWhite.clone(); } public float[] getWhitePoint(float[] out) { out[0] = mWhite[0]; out[1] = mWhite[1]; return out; }
        public TransferParameters getTransferParameters() { return mTf; }
        public double getGamma() { return mTf != null && mTf.a == 1 && mTf.b == 0 && mTf.c == 0 && mTf.d == 0 ? mTf.g : Double.NaN; }
        public boolean isSrgb() { return mSrgb; }
        public boolean isWideGamut() { double area = Math.abs((mPrimaries[0] * mPrimaries[3] + mPrimaries[2] * mPrimaries[5] + mPrimaries[4] * mPrimaries[1] - mPrimaries[1] * mPrimaries[2] - mPrimaries[3] * mPrimaries[4] - mPrimaries[5] * mPrimaries[0]) / 2); return area / 0.1121 > 0.9 * 1.2 || mMin < 0 || mMax > 1; }
        public float getMinValue(int c) { return mMin; } public float getMaxValue(int c) { return mMax; }
        public float[] getTransform() { return null; } public float[] getInverseTransform() { return null; }
    }
}
