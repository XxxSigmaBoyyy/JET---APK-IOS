package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;

public class TextView extends View {
    public TextView(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public TextView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        mTextSize = 14 * res().scaledDensity;
        initDefaults();
        jetThemeTextDefaults(defStyleAttr);
        if (mJetAttrs != null) {
            XmlAttrs x = mJetAttrs; ResTable t = res(); String v;
            if ((v = x.get("textAppearance")) != null) jetApplyAppearance(t, v);
            if ((v = x.get("text")) != null) mText = t.resolve(v);
            if ((v = x.get("hint")) != null) { String hv = t.resolve(v); if (hv != null && !hv.startsWith("@") && !hv.startsWith("?")) mHint = hv; }
            if ((v = x.get("textColorHint")) != null) { try { String hc = t.resolve(v); if (hc != null && hc.startsWith("#")) mHintColor = ResTable.parseColor(hc); } catch (RuntimeException ignore) {} }
            if ((v = x.get("textSize")) != null) mTextSize = t.dimen(v);
            try {
                if ((v = x.get("drawablePadding")) != null) mDrawablePadding = Math.round(t.dimen(v));
                String[] cn = {"drawableLeft", "drawableTop", "drawableRight", "drawableBottom"}, rn = {"drawableStart", null, "drawableEnd", null};
                android.graphics.drawable.Drawable[] cd = new android.graphics.drawable.Drawable[4], rd = new android.graphics.drawable.Drawable[4]; boolean any = false;
                for (int k = 0; k < 4; k++) {
                    if ((v = x.get(cn[k])) != null) { cd[k] = t.drawable(v); any |= cd[k] != null; }
                    if (rn[k] != null && (v = x.get(rn[k])) != null) { rd[k] = t.drawable(v); any |= rd[k] != null; }
                }
                if (any) { for (int k = 0; k < 4; k++) { jetIntrinsicBounds(cd[k]); jetIntrinsicBounds(rd[k]); } mDrawables = cd; mDrawablesRel = rd; }
            } catch (RuntimeException ignore) {}
            if ((v = x.get("textColor")) != null) jetTextColor(t, v);
            if ((v = x.get("gravity")) != null) mGravity = ResTable.intVal(t.resolve(v));
            if ((v = x.get("textStyle")) != null) mBold = (ResTable.intVal(t.resolve(v)) & 1) != 0;
            try {
                if ((v = x.get("maxLines")) != null) mMaxLines = ResTable.intVal(t.resolve(v));
                if ((v = x.get("lines")) != null) mMaxLines = mMinLines = ResTable.intVal(t.resolve(v));
                if ((v = x.get("minLines")) != null) mMinLines = ResTable.intVal(t.resolve(v));
                if ((v = x.get("singleLine")) != null && "true".equals(t.resolve(v))) setSingleLine(true);
                if ((v = x.get("ellipsize")) != null) { int e = ResTable.intVal(t.resolve(v)); mEllipsize = e == 1 ? android.text.TextUtils.TruncateAt.START : e == 2 ? android.text.TextUtils.TruncateAt.MIDDLE : e == 3 ? android.text.TextUtils.TruncateAt.END : e == 4 ? android.text.TextUtils.TruncateAt.MARQUEE : null; }
                if ((v = x.get("lineSpacingExtra")) != null) mSpacingAdd = t.dimen(v);
                if ((v = x.get("lineSpacingMultiplier")) != null) mSpacingMult = Float.parseFloat(t.resolve(v));
                if ((v = x.get("textAllCaps")) != null && "true".equals(t.resolve(v))) mAllCaps = true;
                if ((v = x.get("autoSizeTextType")) != null && ResTable.intVal(t.resolve(v)) == 1) {
                    String mn = x.get("autoSizeMinTextSize"), mx = x.get("autoSizeMaxTextSize"), sg = x.get("autoSizeStepGranularity");
                    int mnp = mn != null ? Math.round(t.dimen(mn)) : Math.round(jetUnit(2, 12)), mxp = mx != null ? Math.round(t.dimen(mx)) : Math.round(jetUnit(2, 112)), sgp = sg != null ? Math.max(1, Math.round(t.dimen(sg))) : 1;
                    if (mxp > mnp) setAutoSizeTextTypeUniformWithConfiguration(mnp, mxp, sgp, 0 /* PX */);
                }
            } catch (RuntimeException ignored) {}
        }
    }
    /** AOSP order: theme android:textAppearance, then the default style (defStyleAttr or android:textViewStyle), then XML. */
    private void jetThemeTextDefaults(int defStyleAttr) {
        try {
            android.content.res.Resources.Theme th = getContext().getTheme(); if (th == null) return; ResTable t = res();
            String ta = th.raw(0x01010034 /* textAppearance */); if (ta != null) jetApplyAppearance(t, ta);
            String st = th.raw(defStyleAttr != 0 ? defStyleAttr : 0x01010084 /* textViewStyle */);
            if (st != null && st.startsWith("@")) {
                java.util.Map<String, String> m = t.style(ResTable.refId(st)); String w;
                if ((w = m.get("textAppearance")) != null) jetApplyAppearance(t, w);
                if ((w = m.get("textColor")) != null) jetTextColor(t, w);
                if ((w = m.get("textSize")) != null) mTextSize = t.dimen(w);
            }
        } catch (Throwable ignored) {}
    }
    private void jetTextColor(ResTable t, String v) {
        try {
            String r = t.resolve(v);
            if (r != null && r.startsWith("#")) { mTextColor = ResTable.parseColor(r); return; }
            String ref = v.startsWith("?") && getContext().getTheme() != null ? getContext().getTheme().raw(ResTable.refId(v)) : v;
            int id = ref != null && ref.startsWith("@") ? ResTable.refId(ref) : 0;
            if (id != 0) { android.content.res.ColorStateList cs = getResources().getColorStateList(id, getContext().getTheme()); if (cs != null) mTextColor = cs.getColorForState(new int[] { 0x0101009e }, cs.getDefaultColor()); }
        } catch (Throwable ignored) {}
    }
    private void jetApplyAppearance(ResTable t, String v) {
        try {
            String ref = v.startsWith("?") && getContext().getTheme() != null ? getContext().getTheme().raw(ResTable.refId(v)) : v;
            if (ref == null || !ref.startsWith("@")) return;
            java.util.Map<String, String> m = t.style(ResTable.refId(ref)); String w;
            if ((w = m.get("textSize")) != null) mTextSize = t.dimen(w);
            if ((w = m.get("textColor")) != null) jetTextColor(t, w);
            if ((w = m.get("textStyle")) != null) mBold = (ResTable.intVal(t.resolve(w)) & 1) != 0;
            if ((w = m.get("textAllCaps")) != null && "true".equals(t.resolve(w))) setAllCaps(true);
        } catch (Throwable ignored) {}
    }

    protected CharSequence mText = "";
    protected float mTextSize;
    protected int mTextColor = 0xFFFFFFFF;   // classic Theme: light text on black
    protected int mGravity = 0x33;          // TOP|LEFT
    protected boolean mBold;
    public TextView(Context c) { this(c, null); }
    public TextView(Context c, AttributeSet a) { this(c, a, 0x01010084, 0); }
    protected void initDefaults() {}
    /** AOSP: setText(CharSequence) is setText(text, mBufferType), so app overrides of the two-arg form see every call. */
    public void setText(CharSequence s) { setText(s, mBufferType); }
    protected BufferType mBufferType = BufferType.NORMAL;
    public void setText(int resId) { setText(getResources().getText(resId)); }
    public CharSequence getText() { return mText; }
    public void setTextColor(int c) { mTextColor = c; invalidate(); }
    public void setTextSize(float sp) { mTextSize = sp * res().scaledDensity; requestLayout(); }
    public void setGravity(int g) { mGravity = g; invalidate(); }

    protected int jetFontStyle() { return (mBold ? 1 : 0) | (mTypeface != null && mTypeface.isItalic() ? 2 : 0); }
    protected int jetFontSize() { return Math.round(mTextSize); }
    /** Metrics come from the active raster backend so line breaking matches what is drawn. */
    protected jet.gfx.TextMetrics jetMetrics() { return android.graphics.JetGfx.metrics(jetFontStyle(), jetFontSize()); }

    /** Greedy word wrap (StaticLayout-like): explicit newlines, break at spaces, hard-break long words, maxLines + ellipsize. */
    protected java.util.List<String> jetLines(jet.gfx.TextMetrics fm, int avail) {
        String s = displayText().toString(); java.util.ArrayList<String> out = new java.util.ArrayList<>();
        int max = mSingleLine ? 1 : mMaxLines;
        if (mSingleLine || avail <= 0) { for (String para : (mSingleLine ? new String[] { s.replace('\n', ' ') } : s.split("\n", -1))) out.add(para); }
        else for (String para : s.split("\n", -1)) {
            if (para.isEmpty()) { out.add(""); continue; }
            int i = 0, n = para.length();
            while (i < n) {
                if (fm.stringWidth(para.substring(i)) <= avail) { out.add(para.substring(i)); break; }
                int lastSp = -1, j = i;
                while (j < n && fm.stringWidth(para.substring(i, j + 1)) <= avail) { if (para.charAt(j) == ' ') lastSp = j; j++; }
                int brk = lastSp > i ? lastSp : Math.max(j, i + 1);
                out.add(para.substring(i, brk));
                i = brk; while (i < n && para.charAt(i) == ' ') i++;
            }
        }
        if (out.size() > max) {
            java.util.List<String> cut = new java.util.ArrayList<>(out.subList(0, max));
            if (mEllipsize != null && max > 0) { StringBuilder last = new StringBuilder(cut.get(max - 1)); String rest = String.join(" ", out.subList(max - 1, out.size())); last = new StringBuilder(rest); while (last.length() > 0 && fm.stringWidth(last + "\u2026") > avail && avail > 0) last.setLength(last.length() - 1); cut.set(max - 1, last + "\u2026"); }
            out = new java.util.ArrayList<>(cut);
        } else if (mEllipsize != null && out.size() == 1 && avail > 0 && fm.stringWidth(out.get(0)) > avail) {
            StringBuilder last = new StringBuilder(out.get(0)); while (last.length() > 0 && fm.stringWidth(last + "\u2026") > avail) last.setLength(last.length() - 1); out.set(0, last + "\u2026");
        }
        while (out.size() < mMinLines) out.add("");
        return out;
    }
    protected int jetLineHeight(jet.gfx.TextMetrics fm) { return Math.round((fm.getAscent() + fm.getDescent()) * (mSpacingMult == 0 ? 1 : mSpacingMult) + mSpacingAdd); }
    private int mLineCount = 1;
    public int getLineCount() { return mLineCount; }
    public int getLineHeight() { return jetLineHeight(jetMetrics()); }
    @Override protected void onMeasure(int ws, int hs) {
        jet.gfx.TextMetrics fm = jetMetrics();
        int wm = MeasureSpec.getMode(ws), wsz = MeasureSpec.getSize(ws);
        int padH = getCompoundPaddingLeft() + getCompoundPaddingRight(), padV = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int avail = wm == MeasureSpec.UNSPECIFIED ? -1 : Math.max(1, wsz - padH);
        java.util.List<String> lines = jetLines(fm, avail);
        int tw = 0; for (String l : lines) tw = Math.max(tw, fm.stringWidth(l));
        mLineCount = lines.size();
        int lh = jetLineHeight(fm);
        int w = tw + padH, h = (lines.isEmpty() ? fm.getAscent() + fm.getDescent() : (lines.size() - 1) * lh + fm.getAscent() + fm.getDescent()) + padV;
        h = Math.max(h, Math.max(jetCdH(0), jetCdH(2)) + mPaddingTop + mPaddingBottom);
        w = Math.max(w, Math.max(jetCdW(1), jetCdW(3)) + mPaddingLeft + mPaddingRight);
        w = Math.max(w, getSuggestedMinimumWidth()); h = Math.max(h, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSize(w, ws), resolveSize(h, hs));
    }
    private static final String JET_TRACE_DRAW = System.getProperty("jet.traceTextDraw");
    @Override protected void onDraw(android.graphics.Canvas g) {
        jetAutoFit();
        android.graphics.Paint tp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG); tp.setColor(jetShowingHint() ? mHintColor : mTextColor);
        int fst = jetFontStyle(), fsz = jetFontSize();
        // Layout (wrap/ellipsize/alignment) uses the same device-independent metrics as onMeasure, like Android's
        // StaticLayout: the canvas transform (e.g. android:scaleX) must not change line breaking.
        jet.gfx.TextMetrics fm = jetMetrics();
        jetDrawCompounds(g);
        int cpl = getCompoundPaddingLeft(), cpt = getCompoundPaddingTop();
        int aw = getWidth() - cpl - getCompoundPaddingRight(), ah = getHeight() - cpt - getCompoundPaddingBottom();
        java.util.List<String> lines = jetLines(fm, aw);
        int lh = jetLineHeight(fm);
        int th = (lines.size() - 1) * lh + fm.getAscent() + fm.getDescent();
        int hg = mGravity & 0x07, vg = mGravity & 0x70;
        if ((mGravity & 0x800000) != 0) hg = (mGravity & 0x07) == 3 ? 3 : hg; // START/END resolved as LTR
        int y = cpt + (vg == 0x10 ? (ah - th) / 2 : vg == 0x50 ? ah - th : 0) + fm.getAscent();
        int sc = g.save(); g.clipRect(0, 0, getWidth(), getHeight());
        if (JET_TRACE_DRAW != null && getClass().getName().contains(JET_TRACE_DRAW)) System.err.println("[jet-textdraw] " + getClass().getName() + " '" + mText + "' " + getWidth() + "x" + getHeight() + " clip=" + g.getClipBounds() + " lines=" + lines.size() + " y0=" + y + " color=" + Integer.toHexString(tp.getColor()) + " size=" + fsz);
        for (String s : lines) {
            int tw = fm.stringWidth(s);
            int x = cpl + (hg == 1 ? (aw - tw) / 2 : hg == 5 ? aw - tw : 0);
            g.jetDrawString(s, x, y, fst, fsz, tp); y += lh;
        }
        g.restoreToCount(sc);
    }

    // ---- Android TextView API batch (state-holding; rendering uses text/colour/size/gravity/typeface) ----
    public enum BufferType { NORMAL, SPANNABLE, EDITABLE }
    public interface OnEditorActionListener { boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event); }
    protected android.text.method.TransformationMethod mTransformation; protected android.text.method.MovementMethod mMovement;
    protected android.text.method.KeyListener mKeyListener;
    protected android.graphics.Typeface mTypeface; protected CharSequence mHint; protected int mHintColor = 0x80808080, mLinkColor = 0xFF33B5E5;
    protected android.content.res.ColorStateList mTextColors; protected int mMaxLines = Integer.MAX_VALUE, mMinLines, mInputType = 1, mImeOptions, mDrawablePadding;
    protected android.text.TextUtils.TruncateAt mEllipsize; protected boolean mAllCaps, mSingleLine, mSelectable, mCursorVisible = true, mIncludePad = true;
    protected float mSpacingAdd, mSpacingMult = 1f, mLetterSpacing, mScaleX = 1f; protected android.text.InputFilter[] mFilters = new android.text.InputFilter[0];
    protected final java.util.ArrayList<android.text.TextWatcher> mWatchers = new java.util.ArrayList<>();
    protected android.graphics.drawable.Drawable[] mDrawables = new android.graphics.drawable.Drawable[4], mDrawablesRel = new android.graphics.drawable.Drawable[4];
    protected OnEditorActionListener mEditorListener;
    /** AOSP private; Telegram EditTextBoldCursor reflects this. */
    protected Editor mEditor = new Editor(); protected CharSequence mError; protected final android.text.TextPaint mPaint = new android.text.TextPaint(1);
    protected int mMaxWidth = Integer.MAX_VALUE, mMinWidth, mMaxHeight = Integer.MAX_VALUE, mMinHeight, mAutoLink; protected boolean mLinksClickable = true;
    public void setText(CharSequence s, BufferType t) {
        // AOSP order: input filters on the new text, beforeTextChanged, swap (Editable for EDITABLE, ChangeWatcher +
        // selection 0 like ArrowKeyMovementMethod.initialize), onTextChanged, afterTextChanged.
        if (s == null) s = "";
        for (android.text.InputFilter f : mFilters) { CharSequence r = f.filter(s, 0, s.length(), new android.text.SpannedString(""), 0, 0); if (r != null) s = r; }
        CharSequence old = mText == null ? "" : mText; int oldLen = old.length();
        java.util.ArrayList<android.text.TextWatcher> ws = new java.util.ArrayList<>(mWatchers);
        for (android.text.TextWatcher w : ws) w.beforeTextChanged(old, 0, oldLen, s.length());
        if (old instanceof android.text.Spannable) ((android.text.Spannable) old).removeSpan(mChangeWatcher);
        if (t == BufferType.EDITABLE || onCheckIsTextEditor() && this instanceof EditText) {
            android.text.SpannableStringBuilder e = new android.text.SpannableStringBuilder(s); e.setFilters(mFilters);
            mText = e; jetAttachWatcher(e); android.text.Selection.setSelection(e, 0);
        } else mText = s;
        if (t != null) mBufferType = t;
        onTextChanged(mText, 0, oldLen, mText.length());
        for (android.text.TextWatcher w : ws) w.onTextChanged(mText, 0, oldLen, mText.length());
        if (mText instanceof android.text.Editable) for (android.text.TextWatcher w : ws) w.afterTextChanged((android.text.Editable) mText);
        requestLayout(); invalidate();
    }
    /** AOSP TextView.ChangeWatcher: the TextView's own span on its Editable that turns edits into TextWatcher calls. */
    private final class ChangeWatcher implements android.text.TextWatcher, android.text.NoCopySpan {
        java.util.ArrayList<android.text.TextWatcher> pending;
        public void beforeTextChanged(CharSequence c, int a, int n, int m) { pending = new java.util.ArrayList<>(mWatchers); for (android.text.TextWatcher w : pending) w.beforeTextChanged(c, a, n, m); }
        public void onTextChanged(CharSequence c, int a, int n, int m) { TextView.this.onTextChanged(c, a, n, m); for (android.text.TextWatcher w : new java.util.ArrayList<>(mWatchers)) w.onTextChanged(c, a, n, m); requestLayout(); invalidate(); }
        public void afterTextChanged(android.text.Editable e) { for (android.text.TextWatcher w : new java.util.ArrayList<>(mWatchers)) w.afterTextChanged(e); }
    }
    private final ChangeWatcher mChangeWatcher = new ChangeWatcher();
    protected void jetAttachWatcher(android.text.Spannable e) { e.setSpan(mChangeWatcher, 0, e.length(), android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE | (100 << android.text.Spanned.SPAN_PRIORITY_SHIFT)); }
    /** The text as an Editable owned by this view (converting once, like getEditableText for EDITABLE buffers). */
    protected android.text.Editable jetEditable() {
        if (!(mText instanceof android.text.Editable)) { android.text.SpannableStringBuilder e = new android.text.SpannableStringBuilder(mText == null ? "" : mText); e.setFilters(mFilters); mText = e; }
        android.text.Editable e = (android.text.Editable) mText;
        if (e.getSpanStart(mChangeWatcher) < 0) jetAttachWatcher(e);
        return e;
    }
    public final void setText(char[] t, int start, int len) { setText(new String(t, start, len)); }
    public final void setText(int resId, BufferType t) { setText(getResources().getText(resId), t); }
    public final void setTextKeepState(CharSequence s) { setText(s); }
    public void append(CharSequence t) { if (mText instanceof android.text.Editable) ((android.text.Editable) mText).append(t); else setText(android.text.TextUtils.concat(mText, t)); }
    public void append(CharSequence t, int a, int b) { append(t.subSequence(a, b)); }
    protected void onTextChanged(CharSequence text, int start, int before, int after) {}
    public int length() { return mText.length(); }
    public android.text.Editable getEditableText() { return mText instanceof android.text.Editable ? (android.text.Editable) mText : null; }
    public void setTransformationMethod(android.text.method.TransformationMethod m) { mTransformation = m; requestLayout(); }
    public final android.text.method.TransformationMethod getTransformationMethod() { return mTransformation; }
    public final void setMovementMethod(android.text.method.MovementMethod m) { mMovement = m; }
    public final android.text.method.MovementMethod getMovementMethod() { return mMovement; }
    public void setKeyListener(android.text.method.KeyListener k) { mKeyListener = k; } public android.text.method.KeyListener getKeyListener() { return mKeyListener; }
    public void setTypeface(android.graphics.Typeface tf) { mTypeface = tf; mBold = tf != null && tf.isBold(); requestLayout(); }
    public void setTypeface(android.graphics.Typeface tf, int style) { setTypeface(android.graphics.Typeface.create(tf, style)); }
    public android.graphics.Typeface getTypeface() { return mTypeface; }
    public void setTextAppearance(int resId) { setTextAppearance(getContext(), resId); }
    public void setTextAppearance(Context c, int resId) {
        try { android.content.res.TypedArray a = c.obtainStyledAttributes(resId, new int[]{0x01010095, 0x01010098, 0x01010097});
            if (a.hasValue(0)) mTextSize = a.getDimensionPixelSize(0, (int) mTextSize); if (a.hasValue(1)) mTextColor = a.getColor(1, mTextColor);
            if (a.hasValue(2)) mBold = (a.getInt(2, 0) & 1) != 0; a.recycle(); } catch (RuntimeException ignore) {}
        requestLayout(); }
    public void setAllCaps(boolean b) { mAllCaps = b; requestLayout(); } public boolean isAllCaps() { return mAllCaps; }
    public void setSingleLine() { setSingleLine(true); } public void setSingleLine(boolean b) { mSingleLine = b; if (b) mMaxLines = 1; }
    public boolean isSingleLine() { return mSingleLine; }
    public void setLines(int n) { mMaxLines = mMinLines = n; } public void setMaxLines(int n) { mMaxLines = n; } public int getMaxLines() { return mMaxLines; }
    public void setMinLines(int n) { mMinLines = n; } public int getMinLines() { return mMinLines; }
    public void setEllipsize(android.text.TextUtils.TruncateAt w) { mEllipsize = w; } public android.text.TextUtils.TruncateAt getEllipsize() { return mEllipsize; }
    public void setMarqueeRepeatLimit(int n) {} public void setHorizontallyScrolling(boolean b) {}
    public void setCompoundDrawables(android.graphics.drawable.Drawable l, android.graphics.drawable.Drawable t, android.graphics.drawable.Drawable r, android.graphics.drawable.Drawable b) { mDrawables = new android.graphics.drawable.Drawable[]{l, t, r, b}; mDrawablesRel = new android.graphics.drawable.Drawable[4]; requestLayout(); invalidate(); }
    public void setCompoundDrawablesWithIntrinsicBounds(android.graphics.drawable.Drawable l, android.graphics.drawable.Drawable t, android.graphics.drawable.Drawable r, android.graphics.drawable.Drawable b) {
        for (android.graphics.drawable.Drawable d : new android.graphics.drawable.Drawable[]{l, t, r, b}) if (d != null) d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        setCompoundDrawables(l, t, r, b); }
    private android.graphics.drawable.Drawable dr(int id) { return id == 0 ? null : getContext().getDrawable(id); }
    public void setCompoundDrawablesWithIntrinsicBounds(int l, int t, int r, int b) { setCompoundDrawablesWithIntrinsicBounds(dr(l), dr(t), dr(r), dr(b)); }
    public void setCompoundDrawablesRelative(android.graphics.drawable.Drawable s, android.graphics.drawable.Drawable t, android.graphics.drawable.Drawable e, android.graphics.drawable.Drawable b) { setCompoundDrawables(s, t, e, b); mDrawablesRel = new android.graphics.drawable.Drawable[]{s, t, e, b}; }
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(android.graphics.drawable.Drawable s, android.graphics.drawable.Drawable t, android.graphics.drawable.Drawable e, android.graphics.drawable.Drawable b) { setCompoundDrawablesWithIntrinsicBounds(s, t, e, b); mDrawablesRel = new android.graphics.drawable.Drawable[]{s, t, e, b}; }
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int s, int t, int e, int b) { setCompoundDrawablesRelativeWithIntrinsicBounds(dr(s), dr(t), dr(e), dr(b)); }
    public android.graphics.drawable.Drawable[] getCompoundDrawables() { return mDrawables.clone(); }
    public android.graphics.drawable.Drawable[] getCompoundDrawablesRelative() { return mDrawablesRel.clone(); }
    public void setCompoundDrawablePadding(int p) { mDrawablePadding = p; } public int getCompoundDrawablePadding() { return mDrawablePadding; }
    public void setCompoundDrawableTintList(android.content.res.ColorStateList t) {} public void setCompoundDrawableTintMode(android.graphics.PorterDuff.Mode m) {}

    private static void jetIntrinsicBounds(android.graphics.drawable.Drawable d) { if (d != null && d.getBounds().isEmpty()) d.setBounds(0, 0, Math.max(0, d.getIntrinsicWidth()), Math.max(0, d.getIntrinsicHeight())); }
    /** Effective compound drawable (AOSP: relative start/end override left/right in LTR). k: 0=left 1=top 2=right 3=bottom. */
    protected android.graphics.drawable.Drawable jetCompound(int k) { android.graphics.drawable.Drawable r = mDrawablesRel[k]; return r != null ? r : mDrawables[k]; }
    private int jetCdW(int k) { android.graphics.drawable.Drawable d = jetCompound(k); return d == null ? 0 : d.getBounds().width(); }
    private int jetCdH(int k) { android.graphics.drawable.Drawable d = jetCompound(k); return d == null ? 0 : d.getBounds().height(); }
    public int getCompoundPaddingLeft() { return mPaddingLeft + (jetCompound(0) == null ? 0 : jetCdW(0) + mDrawablePadding); }
    public int getCompoundPaddingRight() { return mPaddingRight + (jetCompound(2) == null ? 0 : jetCdW(2) + mDrawablePadding); }
    public int getCompoundPaddingTop() { return mPaddingTop + (jetCompound(1) == null ? 0 : jetCdH(1) + mDrawablePadding); }
    public int getCompoundPaddingBottom() { return mPaddingBottom + (jetCompound(3) == null ? 0 : jetCdH(3) + mDrawablePadding); }
    public int getCompoundPaddingStart() { return getCompoundPaddingLeft(); } public int getCompoundPaddingEnd() { return getCompoundPaddingRight(); }
    public int getTotalPaddingLeft() { return getCompoundPaddingLeft(); } public int getTotalPaddingRight() { return getCompoundPaddingRight(); }
    public int getTotalPaddingTop() { return getCompoundPaddingTop(); } public int getTotalPaddingBottom() { return getCompoundPaddingBottom(); }
    public int getTotalPaddingStart() { return getCompoundPaddingLeft(); } public int getTotalPaddingEnd() { return getCompoundPaddingRight(); }
    private void jetDrawCompounds(android.graphics.Canvas g) {
        int vt = getCompoundPaddingTop(), vb = getHeight() - getCompoundPaddingBottom(), hl = getCompoundPaddingLeft(), hr = getWidth() - getCompoundPaddingRight();
        int[] st = getDrawableState();
        for (int k = 0; k < 4; k++) {
            android.graphics.drawable.Drawable d = jetCompound(k); if (d == null) continue;
            int w = jetCdW(k), h = jetCdH(k), x, y;
            if (k == 0) { x = mPaddingLeft; y = vt + (vb - vt - h) / 2; }
            else if (k == 2) { x = getWidth() - mPaddingRight - w; y = vt + (vb - vt - h) / 2; }
            else if (k == 1) { x = hl + (hr - hl - w) / 2; y = mPaddingTop; }
            else { x = hl + (hr - hl - w) / 2; y = getHeight() - mPaddingBottom - h; }
            if (d.isStateful()) d.setState(st);
            int sc = g.save(); g.translate(x - d.getBounds().left, y - d.getBounds().top); d.draw(g); g.restoreToCount(sc);
        }
    }
    public void setHint(CharSequence h) { mHint = h; invalidate(); } public void setHint(int id) { setHint(getResources().getText(id)); } public CharSequence getHint() { return mHint; }
    public void setHintTextColor(int c) { mHintColor = c; } public void setHintTextColor(android.content.res.ColorStateList c) { if (c != null) mHintColor = c.getDefaultColor(); }
    public final int getCurrentHintTextColor() { return mHintColor; } public final android.content.res.ColorStateList getHintTextColors() { return android.content.res.ColorStateList.valueOf(mHintColor); }
    public void setLinkTextColor(int c) { mLinkColor = c; } public void setLinkTextColor(android.content.res.ColorStateList c) { if (c != null) mLinkColor = c.getDefaultColor(); }
    public final android.content.res.ColorStateList getLinkTextColors() { return android.content.res.ColorStateList.valueOf(mLinkColor); }
    public void setTextColor(android.content.res.ColorStateList c) { if (c != null) { mTextColors = c; mTextColor = c.getDefaultColor(); invalidate(); } }
    public final android.content.res.ColorStateList getTextColors() { return mTextColors != null ? mTextColors : android.content.res.ColorStateList.valueOf(mTextColor); }
    public final int getCurrentTextColor() { return mTextColor; }
    public void setHighlightColor(int c) {} public int getHighlightColor() { return 0x6633B5E5; }
    public void setTextSize(int unit, float size) { mTextSize = android.util.TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics()); requestLayout(); }
    public float getTextSize() { return mTextSize; } public float getTextScaleX() { return mScaleX; } public void setTextScaleX(float s) { mScaleX = s; }
    public int getGravity() { return mGravity; }
    public void setLineSpacing(float add, float mult) { mSpacingAdd = add; mSpacingMult = mult; } public float getLineSpacingExtra() { return mSpacingAdd; } public float getLineSpacingMultiplier() { return mSpacingMult; }
    public void setLineHeight(int h) {}
    public void setFirstBaselineToTopHeight(int h) {} public void setLastBaselineToBottomHeight(int h) {} public int getFirstBaselineToTopHeight() { return 0; } public int getLastBaselineToBottomHeight() { return 0; }
    public void setLetterSpacing(float s) { mLetterSpacing = s; } public float getLetterSpacing() { return mLetterSpacing; }
    public void setIncludeFontPadding(boolean b) { mIncludePad = b; } public boolean getIncludeFontPadding() { return mIncludePad; }
    public void setFallbackLineSpacing(boolean b) {} public void setElegantTextHeight(boolean b) {} public void setFontFeatureSettings(String s) {} public boolean setFontVariationSettings(String s) { return false; }
    public void setTextLocale(java.util.Locale l) {} public void setTextLocales(android.os.LocaleList l) {} public java.util.Locale getTextLocale() { return java.util.Locale.getDefault(); }
    public void setBreakStrategy(int s) {} public int getBreakStrategy() { return 0; } public void setHyphenationFrequency(int f) {} public int getHyphenationFrequency() { return 0; }
    public void setJustificationMode(int m) {} public int getJustificationMode() { return 0; }
    // ---- Autosize (AOSP TextView uniform mode): pick the largest preset size whose layout fits the view box. ----
    public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0, AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;
    private int mAutoSizeType; private int[] mAutoSizes = new int[0]; private int mAutoMin = -1, mAutoMax = -1, mAutoStep = -1; private int mAutoFitW = -1, mAutoFitH = -1; private CharSequence mAutoFitText;
    private float jetUnit(int unit, float v) { android.util.DisplayMetrics m = getResources().getDisplayMetrics(); return android.util.TypedValue.applyDimension(unit, v, m); }
    public void setAutoSizeTextTypeWithDefaults(int t) {
        if (t == AUTO_SIZE_TEXT_TYPE_UNIFORM) setAutoSizeTextTypeUniformWithConfiguration(12, 112, 1, 2 /* COMPLEX_UNIT_SP */);
        else { mAutoSizeType = 0; mAutoSizes = new int[0]; mAutoMin = mAutoMax = mAutoStep = -1; }
    }
    public void setAutoSizeTextTypeUniformWithConfiguration(int min, int max, int step, int unit) {
        float lo = jetUnit(unit, min), hi = jetUnit(unit, max), st = jetUnit(unit, step);
        if (lo <= 0) throw new IllegalArgumentException("Minimum auto-size text size (" + min + "px) is less or equal to (0px)");
        if (hi <= lo) throw new IllegalArgumentException("Maximum auto-size text size (" + max + "px) is less or equal to minimum auto-size text size (" + min + "px)");
        if (st <= 0) throw new IllegalArgumentException("The auto-size step granularity (" + step + "px) is less or equal to (0px)");
        int n = (int) Math.floor((hi - lo) / st) + 1; int[] sz = new int[n];
        for (int i = 0; i < n; i++) sz[i] = Math.round(lo + i * st);
        mAutoSizeType = AUTO_SIZE_TEXT_TYPE_UNIFORM; mAutoSizes = java.util.Arrays.stream(sz).distinct().sorted().toArray();
        mAutoMin = Math.round(lo); mAutoMax = Math.round(hi); mAutoStep = Math.round(st); mAutoFitW = -1; requestLayout();
    }
    public void setAutoSizeTextTypeUniformWithPresetSizes(int[] sizes, int unit) {
        int[] px = new int[sizes.length]; for (int i = 0; i < sizes.length; i++) px[i] = Math.round(jetUnit(unit, sizes[i]));
        mAutoSizes = java.util.Arrays.stream(px).filter(v -> v > 0).distinct().sorted().toArray();
        mAutoSizeType = mAutoSizes.length > 0 ? AUTO_SIZE_TEXT_TYPE_UNIFORM : 0; mAutoMin = mAutoSizes.length > 0 ? mAutoSizes[0] : -1; mAutoMax = mAutoSizes.length > 0 ? mAutoSizes[mAutoSizes.length - 1] : -1; mAutoStep = -1; mAutoFitW = -1; requestLayout();
    }
    public int getAutoSizeTextType() { return mAutoSizeType; }
    public int getAutoSizeMinTextSize() { return mAutoMin; } public int getAutoSizeMaxTextSize() { return mAutoMax; } public int getAutoSizeStepGranularity() { return mAutoStep; } public int[] getAutoSizeTextAvailableSizes() { return mAutoSizes.clone(); }
    private boolean jetFits(float size, int aw, int ah) {
        float keep = mTextSize; int keepMax = mMaxLines; android.text.TextUtils.TruncateAt keepE = mEllipsize;
        try {
            mTextSize = size; mMaxLines = Integer.MAX_VALUE; mEllipsize = null;
            jet.gfx.TextMetrics fm = jetMetrics();
            java.util.List<String> lines = jetLines(fm, aw);
            int max = mSingleLine ? 1 : keepMax;
            if (lines.size() > max) return false;
            for (String l : lines) if (fm.stringWidth(l) > aw) return false;
            int h = (lines.size() - 1) * jetLineHeight(fm) + fm.getAscent() + fm.getDescent();
            return h <= ah;
        } finally { mTextSize = keep; mMaxLines = keepMax; mEllipsize = keepE; }
    }
    /** Called before drawing; cheap when nothing changed. */
    protected void jetAutoFit() {
        if (mAutoSizeType != AUTO_SIZE_TEXT_TYPE_UNIFORM || mAutoSizes.length == 0) return;
        int aw = getWidth() - mPaddingLeft - mPaddingRight, ah = getHeight() - mPaddingTop - mPaddingBottom;
        if (aw <= 0 || ah <= 0) return;
        if (aw == mAutoFitW && ah == mAutoFitH && java.util.Objects.equals(String.valueOf(mText), String.valueOf(mAutoFitText))) return;
        int lo = 0, hi = mAutoSizes.length - 1, best = 0;
        while (lo <= hi) { int mid = (lo + hi) >>> 1; if (jetFits(mAutoSizes[mid], aw, ah)) { best = mid; lo = mid + 1; } else hi = mid - 1; }
        mTextSize = mAutoSizes[best]; mAutoFitW = aw; mAutoFitH = ah; mAutoFitText = mText;
    }
    public void setInputType(int t) { mInputType = t; } public int getInputType() { return mInputType; } public void setRawInputType(int t) { mInputType = t; }
    public void setImeOptions(int o) { mImeOptions = o; } public int getImeOptions() { return mImeOptions; } public void setPrivateImeOptions(String s) {} public void setImeActionLabel(CharSequence l, int id) {}
    public void setImeHintLocales(android.os.LocaleList l) {} public void setShowSoftInputOnFocus(boolean b) {}
    public void setFilters(android.text.InputFilter[] f) { mFilters = f == null ? new android.text.InputFilter[0] : f; if (mText instanceof android.text.Editable) ((android.text.Editable) mText).setFilters(mFilters); } public android.text.InputFilter[] getFilters() { return mFilters; }
    public void addTextChangedListener(android.text.TextWatcher w) { mWatchers.add(w); } public void removeTextChangedListener(android.text.TextWatcher w) { mWatchers.remove(w); }
    public void setOnEditorActionListener(OnEditorActionListener l) { mEditorListener = l; }
    // ---- editor (Pass 33): text input reaches an editable TextView through its InputConnection or hardware keys. ----
    /** Replace [a,b) of the Editable, notifying TextWatchers in Android order (ChangeWatcher: before -> edit -> on -> after). */
    public void jetEditReplace(int a, int b, CharSequence t) {
        android.text.Editable e = jetEditable(); if (t == null) t = "";
        int n = e.length(); a = Math.max(0, Math.min(a, n)); b = Math.max(0, Math.min(b, n)); if (a > b) { int x = a; a = b; b = x; }
        // AOSP BaseInputConnection.replaceText order: the cursor is put at the end of the replaced range BEFORE the
        // replace; the SPAN_POINT_POINT selection then rides to the end of the inserted text, and any selection or
        // reformatting a TextWatcher applies inside replace() (phone-number formatters) is left untouched.
        android.text.Selection.setSelection(e, b);
        e.replace(a, b, t);   // filters + TextWatchers (ChangeWatcher span) run inside, like AOSP
    }
    protected void jetSetSel(int s, int e) {}
    @Override public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo o) {
        if (!onCheckIsTextEditor() || !isEnabled()) return null;
        o.inputType = mInputType; o.imeOptions = mImeOptions; o.hintText = mHint; o.initialSelStart = getSelectionStart(); o.initialSelEnd = getSelectionEnd();
        o.initialCapsMode = 0; return new JetEditableInputConnection(this);
    }
    /** Editable-backed InputConnection for TextView editors (AOSP EditableInputConnection role). */
    public static class JetEditableInputConnection extends android.view.inputmethod.BaseInputConnection {
        final TextView tv; int compA = -1, compB = -1;
        public JetEditableInputConnection(TextView tv) { super(tv, true); this.tv = tv; }
        private int selA() { int a = tv.getSelectionStart(), b = tv.getSelectionEnd(); int n = tv.length(); if (a < 0) a = n; if (b < 0) b = n; return Math.min(a, b); }
        private int selB() { int a = tv.getSelectionStart(), b = tv.getSelectionEnd(); int n = tv.length(); if (a < 0) a = n; if (b < 0) b = n; return Math.max(a, b); }
        @Override public android.text.Editable getEditable() { return tv.jetEditable(); }
        @Override public boolean commitText(CharSequence t, int pos) { int a = compA >= 0 ? compA : selA(), b = compA >= 0 ? compB : selB(); compA = compB = -1; tv.jetEditReplace(a, b, t); return true; }
        @Override public boolean setComposingText(CharSequence t, int pos) { int a = compA >= 0 ? compA : selA(), b = compA >= 0 ? compB : selB(); tv.jetEditReplace(a, b, t); compA = a; compB = a + (t == null ? 0 : t.length()); return true; }
        @Override public boolean finishComposingText() { compA = compB = -1; return true; }
        @Override public boolean deleteSurroundingText(int before, int after) {
            int a = selA(), b = selB(), n = tv.length(); compA = compB = -1;
            if (after > 0 && b < n) tv.jetEditReplace(b, Math.min(n, b + after), "");
            if (before > 0 && a > 0) tv.jetEditReplace(Math.max(0, a - before), a, "");
            return true;
        }
        @Override public boolean setSelection(int s, int e) { int n = tv.length(); if (s < 0 || e < 0 || s > n || e > n) return true; tv.jetSetSel(s, e); return true; }
        @Override public CharSequence getTextBeforeCursor(int n, int f) { CharSequence t = getEditable(); int a = selA(); return t.subSequence(Math.max(0, a - n), a).toString(); }
        @Override public CharSequence getTextAfterCursor(int n, int f) { CharSequence t = getEditable(); int b = selB(); return t.subSequence(b, Math.min(t.length(), b + n)).toString(); }
        @Override public CharSequence getSelectedText(int f) { int a = selA(), b = selB(); return a == b ? null : getEditable().subSequence(a, b).toString(); }
        @Override public boolean performEditorAction(int code) { tv.onEditorAction(code); return true; }
    }
    /** AOSP package-private; Telegram EditTextBoldCursor reflects this. */
    int getVerticalOffset(boolean forceVertical) { return getExtendedPaddingTop(); } public void onEditorAction(int code) { if (mEditorListener != null) mEditorListener.onEditorAction(this, code, null); }
    public android.text.TextPaint getPaint() { mPaint.setTextSize(mTextSize); mPaint.setColor(mTextColor); return mPaint; }
    public void setPaintFlags(int f) { mPaint.setFlags(f); invalidate(); } public int getPaintFlags() { return mPaint.getFlags(); }
    /** AOSP: the Layout of the current text once the view has been measured (null before). Built on demand from
     *  StaticLayout with the view's paint, inner width and horizontal gravity; cached until text/size/width change. */
    private android.text.Layout mJetLayout; private Object mJetLayoutKey;
    public android.text.Layout getLayout() {
        int w = (getWidth() > 0 ? getWidth() : getMeasuredWidth()) - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (getMeasuredWidth() <= 0 && getWidth() <= 0) return null;
        w = Math.max(0, w);
        CharSequence t = mText == null ? "" : mText;
        java.util.List<Object> key = java.util.Arrays.asList(t.toString(), w, mTextSize, mScaleX, mGravity & 0x07);
        if (mJetLayout == null || !key.equals(mJetLayoutKey)) {
            int hg = mGravity & 0x07;
            android.text.Layout.Alignment al = hg == 1 ? android.text.Layout.Alignment.ALIGN_CENTER : hg == 5 ? android.text.Layout.Alignment.ALIGN_OPPOSITE : android.text.Layout.Alignment.ALIGN_NORMAL;
            mJetLayout = new android.text.StaticLayout(t, getPaint(), w, al, 1f, 0f, true); mJetLayoutKey = key;
        }
        return mJetLayout;
    }
    public int getBaseline() { return mPaddingTop + Math.round(mTextSize); }
    public void setShadowLayer(float r, float dx, float dy, int c) {} public float getShadowRadius() { return 0; } public float getShadowDx() { return 0; } public float getShadowDy() { return 0; } public int getShadowColor() { return 0; }
    public void setTextIsSelectable(boolean b) { mSelectable = b; } public boolean isTextSelectable() { return mSelectable; }
    public void setCursorVisible(boolean b) { mCursorVisible = b; } public boolean isCursorVisible() { return mCursorVisible; }
    public int getSelectionStart() { return android.text.Selection.getSelectionStart(mText); } public int getSelectionEnd() { return android.text.Selection.getSelectionEnd(mText); } public boolean hasSelection() { int a = getSelectionStart(), b = getSelectionEnd(); return a >= 0 && a != b; }
    public void setCustomSelectionActionModeCallback(android.view.ActionMode.Callback cb) {} public void setCustomInsertionActionModeCallback(android.view.ActionMode.Callback cb) {}
    public void setError(CharSequence e) { mError = e; } public void setError(CharSequence e, android.graphics.drawable.Drawable icon) { mError = e; } public CharSequence getError() { return mError; }
    public void setMaxWidth(int w) { mMaxWidth = w; } public int getMaxWidth() { return mMaxWidth; } public void setMinWidth(int w) { mMinWidth = w; } public int getMinWidth() { return mMinWidth; }
    public void setMaxHeight(int h) { mMaxHeight = h; } public int getMaxHeight() { return mMaxHeight; } public void setMinHeight(int h) { mMinHeight = h; } public int getMinHeight() { return mMinHeight; }
    public void setWidth(int w) { mMinWidth = mMaxWidth = w; } public void setHeight(int h) { mMinHeight = mMaxHeight = h; }
    public void setEms(int e) {} public void setMaxEms(int e) {} public void setMinEms(int e) {}
    public void setAutoLinkMask(int m) { mAutoLink = m; } public int getAutoLinkMask() { return mAutoLink; }
    public void setLinksClickable(boolean b) { mLinksClickable = b; } public boolean getLinksClickable() { return mLinksClickable; }
    private boolean mFreezesText;
    public void setFreezesText(boolean b) { mFreezesText = b; } public boolean getFreezesText() { return mFreezesText; }
    /** AOSP TextView.onSaveInstanceState: text when frozen (EditText always), selection when present. */
    @Override public android.os.Parcelable onSaveInstanceState() {
        android.os.Parcelable sup = super.onSaveInstanceState();
        boolean freezes = getFreezesText(); int start = -1, end = -1; boolean hasSel = false;
        if (mText != null) { start = getSelectionStart(); end = getSelectionEnd(); if (start >= 0 || end >= 0) hasSel = true; }
        if (!freezes && !hasSel) return sup;
        SavedState ss = new SavedState(sup);
        if (freezes) ss.text = mText instanceof android.text.Spanned ? new android.text.SpannableStringBuilder(mText) : (mText == null ? null : mText.toString());
        if (hasSel) { ss.selStart = start; ss.selEnd = end; }
        ss.frozenWithFocus = isFocused() && start >= 0 && end >= 0;
        return ss;
    }
    @Override public void onRestoreInstanceState(android.os.Parcelable state) {
        if (!(state instanceof SavedState)) { super.onRestoreInstanceState(state); return; }
        SavedState ss = (SavedState) state; super.onRestoreInstanceState(ss.getSuperState());
        if (ss.text != null) setText(ss.text);
        if (ss.selStart >= 0 && ss.selEnd >= 0 && mText instanceof android.text.Spannable) {
            int len = mText.length();
            if (ss.selStart <= len && ss.selEnd <= len) android.text.Selection.setSelection((android.text.Spannable) mText, ss.selStart, ss.selEnd);
        }
    }
    public static class SavedState extends BaseSavedState {
        int selStart = -1, selEnd = -1; CharSequence text; boolean frozenWithFocus;
        SavedState(android.os.Parcelable superState) { super(superState); }
        private SavedState(android.os.Parcel in) { super(in); selStart = in.readInt(); selEnd = in.readInt(); frozenWithFocus = in.readInt() != 0; text = in.readString(); }
        @Override public void writeToParcel(android.os.Parcel out, int flags) { super.writeToParcel(out, flags); out.writeInt(selStart); out.writeInt(selEnd); out.writeInt(frozenWithFocus ? 1 : 0); out.writeString(text == null ? null : text.toString()); }
        @Override public String toString() { return "TextView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " start=" + selStart + " end=" + selEnd + (text != null ? " text=" + text : "") + "}"; }
        public static final android.os.Parcelable.Creator<SavedState> CREATOR = new android.os.Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(android.os.Parcel in) { return new SavedState(in); }
            public SavedState[] newArray(int n) { return new SavedState[n]; }
        };
    }
    public void setSpannableFactory(android.text.Spannable.Factory f) {} public void setEditableFactory(android.text.Editable.Factory f) {}
    public boolean moveCursorToVisibleOffset() { return false; } public boolean didTouchFocusSelect() { return false; }
    private android.graphics.drawable.Drawable mCursorDrawable, mSelectHandle, mSelectHandleLeft, mSelectHandleRight;
    public void setTextCursorDrawable(android.graphics.drawable.Drawable d) { mCursorDrawable = d; invalidate(); }
    public void setTextCursorDrawable(int id) { setTextCursorDrawable(id == 0 ? null : getContext().getDrawable(id)); }
    /** AOSP: theme drawables (textCursorDrawable / textSelectHandle*); here material-shaped drawables tinted with the accent. */
    public android.graphics.drawable.Drawable getTextCursorDrawable() { if (mCursorDrawable == null) { android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setColor(jetAccent()); g.setSize(dpx(2), (int) Math.ceil(getTextSize() * 1.2f)); mCursorDrawable = g; } return mCursorDrawable; }
    public android.graphics.drawable.Drawable getTextSelectHandle() { if (mSelectHandle == null) mSelectHandle = jetHandle(); return mSelectHandle; }
    public android.graphics.drawable.Drawable getTextSelectHandleLeft() { if (mSelectHandleLeft == null) mSelectHandleLeft = jetHandle(); return mSelectHandleLeft; }
    public android.graphics.drawable.Drawable getTextSelectHandleRight() { if (mSelectHandleRight == null) mSelectHandleRight = jetHandle(); return mSelectHandleRight; }
    public void setTextSelectHandle(android.graphics.drawable.Drawable d) { mSelectHandle = d; } public void setTextSelectHandleLeft(android.graphics.drawable.Drawable d) { mSelectHandleLeft = d; } public void setTextSelectHandleRight(android.graphics.drawable.Drawable d) { mSelectHandleRight = d; }
    private android.graphics.drawable.Drawable jetHandle() { android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setShape(android.graphics.drawable.GradientDrawable.OVAL); g.setColor(jetAccent()); g.setSize(dpx(22), dpx(22)); return g; }
    private int dpx(int d) { return (int) (d * getResources().getDisplayMetrics().density + 0.5f); }
    private int jetAccent() { try { android.content.res.TypedArray a = getContext().getTheme().obtainStyledAttributes(new int[] { 0x01010435 /* colorAccent */ }); int c = a.getColor(0, 0xFF6200EE); a.recycle(); return c; } catch (RuntimeException e) { return 0xFF6200EE; } }
    public void beginBatchEdit() {} public void endBatchEdit() {}
    public boolean isSuggestionsEnabled() { return false; } public void setSelectAllOnFocus(boolean b) {}
    public int getExtendedPaddingTop() { return mPaddingTop; } public int getExtendedPaddingBottom() { return mPaddingBottom; }
    public float getMinTextSizeHint() { return 0; }
    /** AOSP: an empty TextView lays out and draws its hint (in the hint colour). */
    protected boolean jetShowingHint() { return (mText == null || mText.length() == 0) && mHint != null && mHint.length() > 0; }
    protected CharSequence displayText() { if (jetShowingHint()) return mHint; CharSequence t = mText; if (mTransformation != null) { CharSequence x = mTransformation.getTransformation(t, this); if (x != null) t = x; } if (mAllCaps) t = t.toString().toUpperCase(); return t; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    private android.graphics.BlendMode jet_CompoundDrawableTintBlendMode;
    private int jet_LineBreakStyle;
    private int jet_LineBreakWordStyle;
    protected boolean getDefaultEditable() { jet.runtime.ShimStubs.hit("android.widget.TextView.getDefaultEditable()Z"); return false; }
    protected android.text.method.MovementMethod getDefaultMovementMethod() { jet.runtime.ShimStubs.hit("android.widget.TextView.getDefaultMovementMethod()Landroid/text/method/MovementMethod;"); return null; }
    public android.content.res.ColorStateList getCompoundDrawableTintList() { jet.runtime.ShimStubs.hit("android.widget.TextView.getCompoundDrawableTintList()Landroid/content/res/ColorStateList;"); return null; }
    public void setCompoundDrawableTintBlendMode(android.graphics.BlendMode p0) { jet_CompoundDrawableTintBlendMode = p0; }
    public android.graphics.PorterDuff.Mode getCompoundDrawableTintMode() { jet.runtime.ShimStubs.hit("android.widget.TextView.getCompoundDrawableTintMode()Landroid/graphics/PorterDuff$Mode;"); return null; }
    public android.graphics.BlendMode getCompoundDrawableTintBlendMode() { return jet_CompoundDrawableTintBlendMode; }
    public void setTextSelectHandle(int id) { setTextSelectHandle(getContext().getDrawable(id)); }
    public void setTextSelectHandleLeft(int id) { setTextSelectHandleLeft(getContext().getDrawable(id)); }
    public void setTextSelectHandleRight(int id) { setTextSelectHandleRight(getContext().getDrawable(id)); }
    public android.os.LocaleList getTextLocales() { jet.runtime.ShimStubs.hit("android.widget.TextView.getTextLocales()Landroid/os/LocaleList;"); return null; }
    public int getTextSizeUnit() { jet.runtime.ShimStubs.hit("android.widget.TextView.getTextSizeUnit()I"); return 0; }
    public boolean isFallbackLineSpacing() { jet.runtime.ShimStubs.hit("android.widget.TextView.isFallbackLineSpacing()Z"); return false; }
    public boolean isElegantTextHeight() { jet.runtime.ShimStubs.hit("android.widget.TextView.isElegantTextHeight()Z"); return false; }
    public java.lang.String getFontFeatureSettings() { jet.runtime.ShimStubs.hit("android.widget.TextView.getFontFeatureSettings()Ljava/lang/String;"); return null; }
    public java.lang.String getFontVariationSettings() { jet.runtime.ShimStubs.hit("android.widget.TextView.getFontVariationSettings()Ljava/lang/String;"); return null; }
    public void setLineBreakStyle(int p0) { jet_LineBreakStyle = p0; }
    public void setLineBreakWordStyle(int p0) { jet_LineBreakWordStyle = p0; }
    public int getLineBreakStyle() { return jet_LineBreakStyle; }
    public int getLineBreakWordStyle() { return jet_LineBreakWordStyle; }
    public boolean getShowSoftInputOnFocus() { jet.runtime.ShimStubs.hit("android.widget.TextView.getShowSoftInputOnFocus()Z"); return false; }
    public boolean isHorizontallyScrollable() { jet.runtime.ShimStubs.hit("android.widget.TextView.isHorizontallyScrollable()Z"); return false; }
    public int getMinEms() { jet.runtime.ShimStubs.hit("android.widget.TextView.getMinEms()I"); return 0; }
    public int getMaxEms() { jet.runtime.ShimStubs.hit("android.widget.TextView.getMaxEms()I"); return 0; }
    public void setTextKeepState(java.lang.CharSequence p0, android.widget.TextView.BufferType p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.setTextKeepState(Ljava/lang/CharSequence;Landroid/widget/TextView$BufferType;)V"); }
    public java.lang.String[] getAutofillHints() { jet.runtime.ShimStubs.hit("android.widget.TextView.getAutofillHints()[Ljava/lang/String;"); return null; }
    public java.lang.CharSequence getImeActionLabel() { jet.runtime.ShimStubs.hit("android.widget.TextView.getImeActionLabel()Ljava/lang/CharSequence;"); return null; }
    public int getImeActionId() { jet.runtime.ShimStubs.hit("android.widget.TextView.getImeActionId()I"); return 0; }
    public java.lang.String getPrivateImeOptions() { jet.runtime.ShimStubs.hit("android.widget.TextView.getPrivateImeOptions()Ljava/lang/String;"); return null; }
    public void setInputExtras(int p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.setInputExtras(I)V"); }
    public android.os.Bundle getInputExtras(boolean p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.getInputExtras(Z)Landroid/os/Bundle;"); return null; }
    public android.os.LocaleList getImeHintLocales() { jet.runtime.ShimStubs.hit("android.widget.TextView.getImeHintLocales()Landroid/os/LocaleList;"); return null; }
    protected boolean setFrame(int p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.widget.TextView.setFrame(IIII)Z"); return false; }
    public boolean onPreDraw() { jet.runtime.ShimStubs.hit("android.widget.TextView.onPreDraw()Z"); return false; }
    public void onScreenStateChanged(int p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onScreenStateChanged(I)V"); }
    protected boolean isPaddingOffsetRequired() { jet.runtime.ShimStubs.hit("android.widget.TextView.isPaddingOffsetRequired()Z"); return false; }
    protected int getLeftPaddingOffset() { jet.runtime.ShimStubs.hit("android.widget.TextView.getLeftPaddingOffset()I"); return 0; }
    protected int getTopPaddingOffset() { jet.runtime.ShimStubs.hit("android.widget.TextView.getTopPaddingOffset()I"); return 0; }
    protected int getBottomPaddingOffset() { jet.runtime.ShimStubs.hit("android.widget.TextView.getBottomPaddingOffset()I"); return 0; }
    protected int getRightPaddingOffset() { jet.runtime.ShimStubs.hit("android.widget.TextView.getRightPaddingOffset()I"); return 0; }
    public void getFocusedRect(android.graphics.Rect p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.getFocusedRect(Landroid/graphics/Rect;)V"); }
    public int getLineBounds(int p0, android.graphics.Rect p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.getLineBounds(ILandroid/graphics/Rect;)I"); return 0; }
    public android.view.PointerIcon onResolvePointerIcon(android.view.MotionEvent p0, int p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onResolvePointerIcon(Landroid/view/MotionEvent;I)Landroid/view/PointerIcon;"); return null; }
    public boolean onKeyPreIme(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onKeyPreIme(ILandroid/view/KeyEvent;)Z"); return false; }
    public boolean onKeyMultiple(int p0, int p1, android.view.KeyEvent p2) { jet.runtime.ShimStubs.hit("android.widget.TextView.onKeyMultiple(IILandroid/view/KeyEvent;)Z"); return false; }
    public void onBeginBatchEdit() { jet.runtime.ShimStubs.hit("android.widget.TextView.onBeginBatchEdit()V"); }
    public void onEndBatchEdit() { jet.runtime.ShimStubs.hit("android.widget.TextView.onEndBatchEdit()V"); }
    public boolean onPrivateIMECommand(java.lang.String p0, android.os.Bundle p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onPrivateIMECommand(Ljava/lang/String;Landroid/os/Bundle;)Z"); return false; }
    public boolean bringPointIntoView(int p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.bringPointIntoView(I)Z"); return false; }
    public void debug(int p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.debug(I)V"); }
    public int getMarqueeRepeatLimit() { jet.runtime.ShimStubs.hit("android.widget.TextView.getMarqueeRepeatLimit()I"); return 0; }
    protected void onSelectionChanged(int p0, int p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onSelectionChanged(II)V"); }
    public void onVisibilityAggregated(boolean p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onVisibilityAggregated(Z)V"); }
    public void clearComposingText() { jet.runtime.ShimStubs.hit("android.widget.TextView.clearComposingText()V"); }
    protected void onCreateContextMenu(android.view.ContextMenu p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onCreateContextMenu(Landroid/view/ContextMenu;)V"); }
    public boolean showContextMenu() { jet.runtime.ShimStubs.hit("android.widget.TextView.showContextMenu()Z"); return false; }
    public boolean showContextMenu(float p0, float p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.showContextMenu(FF)Z"); return false; }
    public boolean onTrackballEvent(android.view.MotionEvent p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onTrackballEvent(Landroid/view/MotionEvent;)Z"); return false; }
    public void setScroller(android.widget.Scroller p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.setScroller(Landroid/widget/Scroller;)V"); }
    protected float getLeftFadingEdgeStrength() { jet.runtime.ShimStubs.hit("android.widget.TextView.getLeftFadingEdgeStrength()F"); return 0; }
    protected float getRightFadingEdgeStrength() { jet.runtime.ShimStubs.hit("android.widget.TextView.getRightFadingEdgeStrength()F"); return 0; }
    protected int computeHorizontalScrollRange() { jet.runtime.ShimStubs.hit("android.widget.TextView.computeHorizontalScrollRange()I"); return 0; }
    protected int computeVerticalScrollRange() { jet.runtime.ShimStubs.hit("android.widget.TextView.computeVerticalScrollRange()I"); return 0; }
    protected int computeVerticalScrollExtent() { jet.runtime.ShimStubs.hit("android.widget.TextView.computeVerticalScrollExtent()I"); return 0; }
    public void findViewsWithText(java.util.ArrayList p0, java.lang.CharSequence p1, int p2) { jet.runtime.ShimStubs.hit("android.widget.TextView.findViewsWithText(Ljava/util/ArrayList;Ljava/lang/CharSequence;I)V"); }
    public boolean onKeyShortcut(int p0, android.view.KeyEvent p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onKeyShortcut(ILandroid/view/KeyEvent;)Z"); return false; }
    public java.lang.CharSequence getAccessibilityClassName() { jet.runtime.ShimStubs.hit("android.widget.TextView.getAccessibilityClassName()Ljava/lang/CharSequence;"); return null; }
    public int getAutofillType() { jet.runtime.ShimStubs.hit("android.widget.TextView.getAutofillType()I"); return 0; }
    public void sendAccessibilityEventUnchecked(android.view.accessibility.AccessibilityEvent p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.sendAccessibilityEventUnchecked(Landroid/view/accessibility/AccessibilityEvent;)V"); }
    public boolean isInputMethodTarget() { jet.runtime.ShimStubs.hit("android.widget.TextView.isInputMethodTarget()Z"); return false; }
    public boolean onTextContextMenuItem(int p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onTextContextMenuItem(I)Z"); return false; }
    public android.view.ActionMode.Callback getCustomSelectionActionModeCallback() { jet.runtime.ShimStubs.hit("android.widget.TextView.getCustomSelectionActionModeCallback()Landroid/view/ActionMode$Callback;"); return null; }
    public android.view.ActionMode.Callback getCustomInsertionActionModeCallback() { jet.runtime.ShimStubs.hit("android.widget.TextView.getCustomInsertionActionModeCallback()Landroid/view/ActionMode$Callback;"); return null; }
    public int getOffsetForPosition(float p0, float p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.getOffsetForPosition(FF)I"); return 0; }
    public boolean onDragEvent(android.view.DragEvent p0) { jet.runtime.ShimStubs.hit("android.widget.TextView.onDragEvent(Landroid/view/DragEvent;)Z"); return false; }
    public void onCreateViewTranslationRequest(int[] p0, java.util.function.Consumer p1) { jet.runtime.ShimStubs.hit("android.widget.TextView.onCreateViewTranslationRequest([ILjava/util/function/Consumer;)V"); }
    // ---- END generated SDK surface ----
}
