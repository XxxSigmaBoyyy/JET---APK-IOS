package android.text;
public class TextPaint extends android.graphics.Paint {
    public int baselineShift, bgColor, linkColor, underlineColor; public float density = 1f; public int[] drawableState;
    public TextPaint() {} public TextPaint(int flags) { super(flags); } public TextPaint(android.graphics.Paint p) { super(p); if (p instanceof TextPaint) copyText((TextPaint) p); }
    public void set(TextPaint tp) { super.set(tp); if (tp != null) copyText(tp); }
    private void copyText(TextPaint t) { baselineShift = t.baselineShift; bgColor = t.bgColor; linkColor = t.linkColor; underlineColor = t.underlineColor; density = t.density; drawableState = t.drawableState; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public float getUnderlineThickness() { jet.runtime.ShimStubs.hit("android.text.TextPaint.getUnderlineThickness()F"); return 0; }
    // ---- END generated SDK surface ----
}
