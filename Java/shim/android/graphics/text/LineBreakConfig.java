package android.graphics.text;
/** JET: API 33 LineBreakConfig (line-break style/word-style; the host breaker treats all as NORMAL). */
public final class LineBreakConfig {
    public static final int LINE_BREAK_STYLE_UNSPECIFIED = -1, LINE_BREAK_STYLE_NONE = 0, LINE_BREAK_STYLE_LOOSE = 1, LINE_BREAK_STYLE_NORMAL = 2, LINE_BREAK_STYLE_STRICT = 3, LINE_BREAK_STYLE_AUTO = 5;
    public static final int LINE_BREAK_WORD_STYLE_UNSPECIFIED = -1, LINE_BREAK_WORD_STYLE_NONE = 0, LINE_BREAK_WORD_STYLE_PHRASE = 1, LINE_BREAK_WORD_STYLE_AUTO = 2;
    public static final int HYPHENATION_UNSPECIFIED = -1, HYPHENATION_DISABLED = 0, HYPHENATION_ENABLED = 1;
    public static final LineBreakConfig NONE = new Builder().setLineBreakStyle(LINE_BREAK_STYLE_NONE).setLineBreakWordStyle(LINE_BREAK_WORD_STYLE_NONE).build();
    private final int style, wordStyle, hyphenation;
    private LineBreakConfig(int s, int w, int h) { style = s; wordStyle = w; hyphenation = h; }
    public int getLineBreakStyle() { return style; }
    public int getLineBreakWordStyle() { return wordStyle; }
    public int getHyphenation() { return hyphenation; }
    public LineBreakConfig merge(LineBreakConfig o) { return new LineBreakConfig(o.style != -1 ? o.style : style, o.wordStyle != -1 ? o.wordStyle : wordStyle, o.hyphenation != -1 ? o.hyphenation : hyphenation); }
    public static LineBreakConfig getLineBreakConfig(int s, int w) { return new Builder().setLineBreakStyle(s).setLineBreakWordStyle(w).build(); }
    @Override public boolean equals(Object o) { return o instanceof LineBreakConfig && ((LineBreakConfig) o).style == style && ((LineBreakConfig) o).wordStyle == wordStyle && ((LineBreakConfig) o).hyphenation == hyphenation; }
    @Override public int hashCode() { return (style * 31 + wordStyle) * 31 + hyphenation; }
    public static final class Builder {
        private int s = LINE_BREAK_STYLE_UNSPECIFIED, w = LINE_BREAK_WORD_STYLE_UNSPECIFIED, h = HYPHENATION_UNSPECIFIED;
        public Builder() {}
        public Builder merge(LineBreakConfig c) { if (c.style != -1) s = c.style; if (c.wordStyle != -1) w = c.wordStyle; if (c.hyphenation != -1) h = c.hyphenation; return this; }
        public Builder setLineBreakStyle(int v) { s = v; return this; }
        public Builder setLineBreakWordStyle(int v) { w = v; return this; }
        public Builder setHyphenation(int v) { h = v; return this; }
        public LineBreakConfig build() { return new LineBreakConfig(s, w, h); }
    }
}
