package android.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;
/**
 * AOSP Switch: label text + a track with a sliding thumb on the end side. Track/thumb come from the theme's switchStyle
 * (Holo 9-patch selectors); when the style's drawables cannot be loaded (Material animated-selector thumb) the
 * Material 2-state look is drawn: 34x14dp rounded track, 20dp thumb, colorControlActivated when checked.
 */
public class Switch extends CompoundButton {
    private Drawable mThumb, mTrack; private CharSequence mTextOn = "ON", mTextOff = "OFF";
    private int mSwitchMinWidth, mSwitchPadding, mThumbTextPadding, mSwitchW, mSwitchH; private boolean mShowText = true, mSplitTrack;
    public Switch(Context c) { this(c, null); }
    public Switch(Context c, AttributeSet a) { this(c, a, 0x0101043f /* switchStyle */); }
    public Switch(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public Switch(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        XmlAttrs x = mJetAttrs; ResTable t = res(); String v;
        if (x != null) {
            try { if ((v = x.get("thumb")) != null) mThumb = t.drawable(v); } catch (RuntimeException ignored) {}
            try { if ((v = x.get("track")) != null) mTrack = t.drawable(v); } catch (RuntimeException ignored) {}
            if ((v = x.get("textOn")) != null) mTextOn = t.resolve(v);
            if ((v = x.get("textOff")) != null) mTextOff = t.resolve(v);
            if ((v = x.get("showText")) != null) mShowText = "true".equals(t.resolve(v));
            if ((v = x.get("splitTrack")) != null) mSplitTrack = "true".equals(t.resolve(v));
            try { if ((v = x.get("switchMinWidth")) != null) mSwitchMinWidth = Math.round(t.dimen(v)); } catch (RuntimeException ignored) {}
            try { if ((v = x.get("switchPadding")) != null) mSwitchPadding = Math.round(t.dimen(v)); } catch (RuntimeException ignored) {}
            try { if ((v = x.get("thumbTextPadding")) != null) mThumbTextPadding = Math.round(t.dimen(v)); } catch (RuntimeException ignored) {}
        }
        if (mThumb == null) { mShowText = false; mTrack = null; }   // Material fallback look is drawn in onDraw
        if (getButtonDrawable() != null) setButtonDrawable((Drawable) null);  // Switch has no CompoundButton button drawable
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private int themeColor(int attr, int dflt) {
        try {
            android.content.res.Resources.Theme th = getContext().getTheme(); if (th == null) return dflt;
            String r = th.raw(attr); int guard = 0;
            while (r != null && r.startsWith("?") && guard++ < 5) r = th.raw(ResTable.refId(r));
            if (r == null) return dflt;
            String s = res().resolve(r); if (s != null && s.startsWith("#")) return ResTable.parseColor(s);
            android.content.res.ColorStateList cs = getResources().getColorStateList(ResTable.refId(r), th);
            return cs == null ? dflt : cs.getDefaultColor();
        } catch (Throwable e) { return dflt; }
    }
    private void measureSwitch() {
        if (mThumb != null) {
            android.graphics.Rect pad = new android.graphics.Rect(); if (mTrack != null) mTrack.getPadding(pad);
            int tw = 0; Paint p = new Paint(); p.setTextSize(mTextSize);
            if (mShowText) tw = Math.round(Math.max(p.measureText(String.valueOf(mTextOn)), p.measureText(String.valueOf(mTextOff)))) + mThumbTextPadding * 2;
            int thumbW = Math.max(tw, Math.max(0, mThumb.getIntrinsicWidth()));
            mSwitchW = Math.max(mSwitchMinWidth, thumbW * 2 + pad.left + pad.right);
            mSwitchH = Math.max(mTrack != null ? mTrack.getIntrinsicHeight() : 0, mThumb.getIntrinsicHeight());
        } else { mSwitchW = Math.round(dp(34 + 2 * 4)); mSwitchH = Math.round(dp(48)); if (mSwitchPadding == 0) mSwitchPadding = Math.round(dp(16)); }
    }
    @Override public int getCompoundPaddingRight() { if (mSwitchW == 0) measureSwitch(); int p = super.getCompoundPaddingRight() + mSwitchW; if (getText() != null && getText().length() > 0) p += mSwitchPadding; return p; }
    @Override protected void onMeasure(int ws, int hs) {
        measureSwitch(); super.onMeasure(ws, hs);
        if (getMeasuredHeight() < mSwitchH) setMeasuredDimension(getMeasuredWidth(), resolveSize(mSwitchH, hs));
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (mSwitchW == 0) measureSwitch();
        int right = getWidth() - mPaddingRight, left = right - mSwitchW, cy = getHeight() / 2, top = cy - mSwitchH / 2, bottom = top + mSwitchH;
        boolean on = isChecked();
        if (mThumb != null) {
            int[] st = getDrawableState();
            android.graphics.Rect pad = new android.graphics.Rect();
            if (mTrack != null) { mTrack.setState(st); mTrack.getPadding(pad); mTrack.jetDraw(c, left, top, right, bottom, 0); }
            int inner = mSwitchW - pad.left - pad.right, tw = inner / 2;
            int tl = on ? right - pad.right - tw : left + pad.left;
            mThumb.setState(st); android.graphics.Rect tp = new android.graphics.Rect(); mThumb.getPadding(tp);
            mThumb.jetDraw(c, tl - tp.left, top, tl + tw + tp.right, bottom, 0);
            if (mShowText) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTextSize(mTextSize * 0.85f); p.setColor(mTextColor); p.setTextAlign(Paint.Align.CENTER);
                String s = String.valueOf(on ? mTextOn : mTextOff); Paint.FontMetrics fm = p.getFontMetrics();
                c.drawText(s, tl + tw / 2f, cy - (fm.ascent + fm.descent) / 2f, p);
            }
        } else {
            int act = themeColor(0x0101042a, themeColor(0x01010435, 0xFF009688)), fg = themeColor(0x01010030, 0xFF000000);
            boolean en = isEnabled(); float tr = dp(7), r = dp(10);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(on ? (act & 0x00FFFFFF) | 0x4D000000 : (fg & 0x00FFFFFF) | 0x4D000000); if (!en) p.setAlpha(p.getAlpha() / 2);
            c.drawRoundRect(new RectF(left + dp(4), cy - tr, right - dp(4), cy + tr), tr, tr, p);
            float tx = on ? right - dp(4) - r + dp(3) : left + dp(4) + r - dp(3);
            p.setColor(0x33000000); c.drawCircle(tx, cy + dp(1), r, p);
            p.setColor(on ? act : 0xFFF1F1F1); if (!en) p.setAlpha(160); c.drawCircle(tx, cy, r, p);
        }
    }
    public void setThumbDrawable(Drawable d) { mThumb = d; requestLayout(); } public Drawable getThumbDrawable() { return mThumb; }
    public void setTrackDrawable(Drawable d) { mTrack = d; requestLayout(); } public Drawable getTrackDrawable() { return mTrack; }
    public void setThumbResource(int id) { setThumbDrawable(getContext().getDrawable(id)); } public void setTrackResource(int id) { setTrackDrawable(getContext().getDrawable(id)); }
    public void setTextOn(CharSequence t) { mTextOn = t; } public CharSequence getTextOn() { return mTextOn; }
    public void setTextOff(CharSequence t) { mTextOff = t; } public CharSequence getTextOff() { return mTextOff; }
    public void setShowText(boolean s) { mShowText = s; requestLayout(); } public boolean getShowText() { return mShowText; }
    public void setSplitTrack(boolean s) { mSplitTrack = s; } public boolean getSplitTrack() { return mSplitTrack; }
    public void setSwitchMinWidth(int w) { mSwitchMinWidth = w; requestLayout(); } public int getSwitchMinWidth() { return mSwitchMinWidth; }
    public void setSwitchPadding(int p) { mSwitchPadding = p; requestLayout(); } public int getSwitchPadding() { return mSwitchPadding; }
    public void setThumbTextPadding(int p) { mThumbTextPadding = p; requestLayout(); } public int getThumbTextPadding() { return mThumbTextPadding; }
    public void setSwitchTextAppearance(Context c, int resid) {}
    public void setSwitchTypeface(android.graphics.Typeface tf) {} public void setSwitchTypeface(android.graphics.Typeface tf, int style) {}
    public void setThumbTintList(android.content.res.ColorStateList l) { if (mThumb != null) mThumb.setTintList(l); }
    public void setTrackTintList(android.content.res.ColorStateList l) { if (mTrack != null) mTrack.setTintList(l); }
    public void setThumbTintMode(android.graphics.PorterDuff.Mode m) {} public void setTrackTintMode(android.graphics.PorterDuff.Mode m) {}
    public int getSwitchMinimumWidth() { return mSwitchMinWidth; }
    @Override public CharSequence getAccessibilityClassName() { return Switch.class.getName(); }
}
