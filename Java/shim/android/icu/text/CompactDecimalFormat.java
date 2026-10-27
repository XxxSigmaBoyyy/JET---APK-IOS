package android.icu.text;

import java.util.Locale;

/** Linux-only harness shim over java.text.CompactNumberFormat (ICU-compatible output for SHORT/LONG, e.g. "1.2K"). */
public class CompactDecimalFormat {
    public enum CompactStyle { SHORT, LONG }
    private final java.text.NumberFormat f;
    private CompactDecimalFormat(Locale l, CompactStyle s) {
        f = java.text.NumberFormat.getCompactNumberInstance(l == null ? Locale.getDefault() : l,
                s == CompactStyle.LONG ? java.text.NumberFormat.Style.LONG : java.text.NumberFormat.Style.SHORT);
        f.setMaximumFractionDigits(1); // ICU default: 2 significant digits
    }
    public static CompactDecimalFormat getInstance(Locale l, CompactStyle s) { return new CompactDecimalFormat(l, s); }
    public String format(long v) { return f.format(v); }
    public String format(double v) { return f.format(v); }
    public String format(Object v) { return f.format(v); }
    public void setMaximumFractionDigits(int n) { f.setMaximumFractionDigits(n); }
    public void setMinimumFractionDigits(int n) { f.setMinimumFractionDigits(n); }
}
