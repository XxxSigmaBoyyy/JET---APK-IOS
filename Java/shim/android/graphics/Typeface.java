package android.graphics;
public class Typeface {
    public static final Typeface DEFAULT = new Typeface(false, false);
    public static final Typeface DEFAULT_BOLD = new Typeface(true, false);
    public static final Typeface MONOSPACE = new Typeface(false, false);
    public static final Typeface SANS_SERIF = DEFAULT;
    public static final Typeface SERIF = DEFAULT;
    public static final int NORMAL = 0, BOLD = 1, ITALIC = 2, BOLD_ITALIC = 3;
    private final boolean bold, italic;
    private Typeface(boolean b, boolean i) { bold = b; italic = i; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public static Typeface create(Typeface family, int style) {
        return new Typeface((style & BOLD) != 0, (style & ITALIC) != 0);
    }
    public static Typeface defaultFromStyle(int style) { return create(DEFAULT, style); }
    public static Typeface create(String family, int style) { Typeface t = create(DEFAULT, style); return t; }
    public static Typeface create(Typeface family, int weight, boolean italic) { return new Typeface(weight >= 600, italic); }
    public static Typeface createFromAsset(android.content.res.AssetManager m, String path) { return DEFAULT; }
    public static Typeface createFromFile(java.io.File f) { return DEFAULT; } public static Typeface createFromFile(String p) { return DEFAULT; }
    public int getStyle() { return (bold ? BOLD : 0) | (italic ? ITALIC : 0); } public int getWeight() { return bold ? 700 : 400; }
    @Override public boolean equals(Object o) { return o instanceof Typeface && ((Typeface) o).bold == bold && ((Typeface) o).italic == italic; } @Override public int hashCode() { return (bold ? 1 : 0) | (italic ? 2 : 0); }
    public static final class Builder {
        private int weight = 400; private boolean italic;
        public Builder(java.io.File f) {} public Builder(String path) {} public Builder(android.content.res.AssetManager m, String path) {} public Builder(java.io.FileDescriptor fd) {}
        public Builder setWeight(int w) { weight = w; return this; } public Builder setItalic(boolean i) { italic = i; return this; } public Builder setFallback(String f) { return this; }
        public Builder setFontVariationSettings(String s) { return this; } public Builder setTtcIndex(int i) { return this; }
        public Typeface build() { return new Typeface(weight >= 600, italic); }
    }
}
