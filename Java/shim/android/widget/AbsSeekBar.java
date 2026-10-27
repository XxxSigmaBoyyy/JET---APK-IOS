package android.widget;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import jet.runtime.XmlAttrs;
/** AOSP AbsSeekBar: thumb + optional tick marks over the ProgressBar track; touch drags set progress (fromUser=true). */
public abstract class AbsSeekBar extends ProgressBar {
    private Drawable mThumb, mTickMark; private ColorStateList mThumbTint, mTickTint; private android.graphics.PorterDuff.Mode mThumbMode, mTickMode; private android.graphics.BlendMode mThumbBlend, mTickBlend;
    private int mThumbOffset, mKeyInc = 1; private boolean mSplitTrack; boolean mIsUserSeekable = true; private boolean mDragging;
    public AbsSeekBar(Context c) { this(c, null); }
    public AbsSeekBar(Context c, AttributeSet a) { this(c, a, 0); }
    public AbsSeekBar(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public AbsSeekBar(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes); setFocusable(true); setClickable(true);
        XmlAttrs x = XmlAttrs.of(a);
        if (x != null) {
            try { String v; if ((v = x.get("thumb")) != null && v.startsWith("@") && !"@null".equals(v)) setThumb(res().drawable(v)); if ((v = x.get("tickMark")) != null && v.startsWith("@") && !"@null".equals(v)) setTickMark(res().drawable(v));
                if ((v = x.get("max")) != null) setMax(jet.runtime.ResTable.intVal(res().resolve(v))); if ((v = x.get("min")) != null) setMin(jet.runtime.ResTable.intVal(res().resolve(v))); if ((v = x.get("progress")) != null) setProgress(jet.runtime.ResTable.intVal(res().resolve(v))); } catch (RuntimeException ignored) {}
        }
    }
    public void setThumb(Drawable thumb) { mThumb = thumb; if (thumb != null) { mThumbOffset = thumb.getIntrinsicWidth() / 2; applyTint(thumb, mThumbTint, mThumbMode); } requestLayout(); invalidate(); }
    public Drawable getThumb() { return mThumb; }
    private void applyTint(Drawable d, ColorStateList t, android.graphics.PorterDuff.Mode m) { if (d == null) return; if (t != null) d.setTintList(t); if (m != null) d.setTintMode(m); if (d.isStateful()) d.setState(getDrawableState()); }
    public void setThumbTintList(ColorStateList t) { mThumbTint = t; applyTint(mThumb, t, mThumbMode); } public ColorStateList getThumbTintList() { return mThumbTint; }
    public void setThumbTintMode(android.graphics.PorterDuff.Mode m) { mThumbMode = m; applyTint(mThumb, mThumbTint, m); } public void setThumbTintBlendMode(android.graphics.BlendMode m) { mThumbBlend = m; }
    public android.graphics.PorterDuff.Mode getThumbTintMode() { return mThumbMode; } public android.graphics.BlendMode getThumbTintBlendMode() { return mThumbBlend; }
    public int getThumbOffset() { return mThumbOffset; } public void setThumbOffset(int o) { mThumbOffset = o; invalidate(); }
    public void setSplitTrack(boolean s) { mSplitTrack = s; } public boolean getSplitTrack() { return mSplitTrack; }
    public void setTickMark(Drawable d) { mTickMark = d; applyTint(d, mTickTint, mTickMode); invalidate(); } public Drawable getTickMark() { return mTickMark; }
    public void setTickMarkTintList(ColorStateList t) { mTickTint = t; applyTint(mTickMark, t, mTickMode); } public ColorStateList getTickMarkTintList() { return mTickTint; }
    public void setTickMarkTintMode(android.graphics.PorterDuff.Mode m) { mTickMode = m; applyTint(mTickMark, mTickTint, m); } public void setTickMarkTintBlendMode(android.graphics.BlendMode m) { mTickBlend = m; }
    public android.graphics.PorterDuff.Mode getTickMarkTintMode() { return mTickMode; } public android.graphics.BlendMode getTickMarkTintBlendMode() { return mTickBlend; }
    public void setKeyProgressIncrement(int i) { mKeyInc = i < 0 ? -i : i; } public int getKeyProgressIncrement() { return mKeyInc; }
    @Override public synchronized void setMin(int m) { super.setMin(m); int r = getMax() - getMin(); if (mKeyInc == 0 || r / mKeyInc > 20) setKeyProgressIncrement(Math.max(1, Math.round(r / 20f))); }
    @Override public synchronized void setMax(int m) { super.setMax(m); int r = getMax() - getMin(); if (mKeyInc == 0 || r / mKeyInc > 20) setKeyProgressIncrement(Math.max(1, Math.round(r / 20f))); }
    public void jumpDrawablesToCurrentState() { super.jumpDrawablesToCurrentState(); if (mThumb != null) mThumb.jumpToCurrentState(); if (mTickMark != null) mTickMark.jumpToCurrentState(); }
    public void drawableHotspotChanged(float x, float y) { super.drawableHotspotChanged(x, y); if (mThumb != null) mThumb.setHotspot(x, y); }
    void onStartTrackingTouch() { mDragging = true; } void onStopTrackingTouch() { mDragging = false; } void onKeyChange() {}
    private int trackLeft() { return mPaddingLeft + mThumbOffset; } private int trackWidth() { return Math.max(1, getWidth() - mPaddingLeft - mPaddingRight - 2 * mThumbOffset); }
    private void trackTouch(MotionEvent e) { float s = Math.max(0f, Math.min(1f, (e.getX() - trackLeft()) / trackWidth())); if (getLayoutDirection() == 1) s = 1 - s; int r = getMax() - getMin(); setProgressInternal(Math.round(getMin() + s * r), true); }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!mIsUserSeekable || !isEnabled()) return false;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: setPressed(true); onStartTrackingTouch(); trackTouch(e); return true;
            case MotionEvent.ACTION_MOVE: if (mDragging) trackTouch(e); return true;
            case MotionEvent.ACTION_UP: if (mDragging) { trackTouch(e); onStopTrackingTouch(); } setPressed(false); invalidate(); return true;
            case MotionEvent.ACTION_CANCEL: if (mDragging) onStopTrackingTouch(); setPressed(false); return true;
        }
        return true;
    }
    @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (isEnabled()) { int inc = keyCode == 21 /*DPAD_LEFT*/ || keyCode == 69 /*MINUS*/ ? -mKeyInc : keyCode == 22 || keyCode == 81 /*PLUS*/ || keyCode == 70 ? mKeyInc : 0;
            if (inc != 0) { if (getLayoutDirection() == 1) inc = -inc; if (setProgressInternal(getProgress() + inc, true)) onKeyChange(); return true; } }
        return super.onKeyDown(keyCode, event);
    }
    @Override protected void onMeasure(int ws, int hs) {
        int th = mThumb == null ? 0 : mThumb.getIntrinsicHeight(), dh = Math.max(th, (int) (4 * res().density)) + mPaddingTop + mPaddingBottom;
        setMeasuredDimension(resolveSize((int) (100 * res().density), ws), resolveSize(Math.max(dh, (int) (32 * res().density)), hs));
    }
    @Override protected void onDraw(android.graphics.Canvas g) {
        int h = getHeight(), cy = h / 2, l = trackLeft(), w = trackWidth(); int r = getMax() - getMin(); float s = r <= 0 ? 0 : (getProgress() - getMin()) / (float) r;
        Drawable pd = getProgressDrawable();
        if (pd != null) { int ih = pd.getIntrinsicHeight() > 0 ? Math.min(pd.getIntrinsicHeight(), h) : (int) (4 * res().density); pd.setBounds(l, cy - ih / 2, l + w, cy - ih / 2 + ih); pd.setLevel(Math.round(s * 10000)); pd.draw(g); }
        else { android.graphics.Paint p = new android.graphics.Paint(); int t = Math.max(1, Math.round(2 * res().density)); p.setColor(0x42000000); g.drawRect(l, cy - t, l + w, cy + t, p); p.setColor(0xFF33B5E5); g.drawRect(l, cy - t, l + s * w, cy + t, p); }
        if (mTickMark != null && r > 1) { int tw = mTickMark.getIntrinsicWidth(), thh = mTickMark.getIntrinsicHeight(); int hw = tw >= 0 ? tw / 2 : 1, hh = thh >= 0 ? thh / 2 : 1; for (int i = 0; i <= r; i++) { int x = l + Math.round(i * w / (float) r); mTickMark.setBounds(x - hw, cy - hh, x + hw, cy + hh); mTickMark.draw(g); } }
        if (mThumb != null) { int tw = mThumb.getIntrinsicWidth(), thh = mThumb.getIntrinsicHeight(); int x = l + Math.round(s * w); if (mThumb.isStateful()) mThumb.setState(getDrawableState()); mThumb.setBounds(x - tw / 2, cy - thh / 2, x - tw / 2 + tw, cy - thh / 2 + thh); mThumb.draw(g); }
    }
    @Override public CharSequence getAccessibilityClassName() { return AbsSeekBar.class.getName(); }
    public void onRtlPropertiesChanged(int layoutDirection) { invalidate(); }
}
