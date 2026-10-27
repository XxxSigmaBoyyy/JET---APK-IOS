package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
public class ProgressBar extends View {
    public ProgressBar(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    /** AOSP semantics: defStyleAttr android:attr/progressBarStyle; framework Widget.ProgressBar is indeterminateOnly (spinner),
     *  *Horizontal styles / AbsSeekBar are determinate bars. Reads indeterminate[Only], max, min, progress, secondaryProgress,
     *  progressDrawable, indeterminateDrawable and the tint attributes from the effective attributes (element/style/defStyle). */
    public ProgressBar(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        jet.runtime.XmlAttrs x = mJetAttrs; String st = x == null ? null : x.get("style");
        boolean horiz = this instanceof AbsSeekBar || defStyleAttr == 0x01010078 /*progressBarStyleHorizontal*/ || (st != null && (st.contains("orizontal") || st.equals("?16843088") || st.equalsIgnoreCase("@android:01030078") || st.equals("?android:attr/progressBarStyleHorizontal")));
        indeterminate = onlyIndeterminate = !horiz;
        if (x == null) return;
        try {
            String v;
            if ((v = x.get("progressDrawable")) != null) { progDrawable = res().drawable(v); if (x.get("indeterminateOnly") == null && x.get("indeterminate") == null) indeterminate = onlyIndeterminate = false; }
            if ((v = x.get("indeterminateOnly")) != null) onlyIndeterminate = "true".equals(v);
            if ((v = x.get("indeterminate")) != null) indeterminate = "true".equals(v) || onlyIndeterminate; else indeterminate = onlyIndeterminate;
            if ((v = x.get("max")) != null) max = Integer.parseInt(res().resolve(v).trim());
            if ((v = x.get("min")) != null) min = Integer.parseInt(res().resolve(v).trim());
            if ((v = x.get("progress")) != null) progress = Math.max(min, Math.min(max, Integer.parseInt(res().resolve(v).trim())));
            if ((v = x.get("secondaryProgress")) != null) secondary = Integer.parseInt(res().resolve(v).trim());
        } catch (RuntimeException ignored) {}
        try { String v = x.get("indeterminateDrawable"); if (v != null) indDrawable = res().drawable(v); } catch (RuntimeException ignored) {}
        progTint = csl(x.get("progressTint")); indTint = csl(x.get("indeterminateTint")); progBgTint = csl(x.get("progressBackgroundTint")); secTint = csl(x.get("secondaryProgressTint"));
    }
    private android.content.res.ColorStateList csl(String v) {
        if (v == null) return null;
        try { if (v.startsWith("@") && !v.startsWith("@android:")) { android.content.res.ColorStateList c = getContext().getResources().getColorStateList(jet.runtime.ResTable.refId(v), null); if (c != null) return c; }
              return android.content.res.ColorStateList.valueOf(jet.runtime.ResTable.parseColor(res().resolve(v))); } catch (RuntimeException e) { return null; }
    }
    /** Theme accent: android:colorControlActivated -> android:colorAccent -> app (AppCompat/Material) colorControlActivated/colorAccent/colorPrimary. */
    private int accent() {
        try {
            android.content.res.Resources.Theme th = getContext().getTheme(); android.content.res.Resources r = getContext().getResources();
            int[] ids = { 0x01010433, 0x01010435, r.getIdentifier("colorControlActivated", "attr", null), r.getIdentifier("colorAccent", "attr", null), r.getIdentifier("colorPrimary", "attr", null) };
            for (int id : ids) { if (id == 0) continue; String v = th.raw(id); if (v == null) continue;
                android.content.res.ColorStateList c = csl(v); if (c != null) return c.getDefaultColor(); }
        } catch (Throwable ignored) {}
        return 0xFF009688; // device-default Material teal accent
    }

    private int max = 100, progress;
    public ProgressBar(Context c) { this(c, null); }
    public ProgressBar(Context c, AttributeSet a) { this(c, a, 0x01010077 /*android:attr/progressBarStyle*/, 0); }
    private int min, secondary; private boolean indeterminate, onlyIndeterminate;
    private android.graphics.drawable.Drawable indDrawable, progDrawable; private android.view.animation.Interpolator interp;
    private android.content.res.ColorStateList progTint, indTint, progBgTint, secTint;
    public synchronized void setMax(int m) { max = Math.max(m, min); if (progress > max) progress = max; invalidate(); }
    public synchronized int getMax() { return max; }
    public synchronized void setMin(int m) { min = Math.min(m, max); if (progress < min) progress = min; invalidate(); }
    public synchronized int getMin() { return min; }
    public synchronized void setProgress(int p) { setProgressInternal(p, false); }
    /** AOSP: clamps, and notifies onProgressRefresh (SeekBar listeners) only when the value changes. */
    synchronized boolean setProgressInternal(int p, boolean fromUser) { int v = indeterminate ? progress : Math.max(min, Math.min(max, p)); if (v == progress) return false; progress = v; invalidate(); onProgressRefresh(max > min ? (v - min) / (float) (max - min) : 0f, fromUser, v); return true; }
    void onProgressRefresh(float scale, boolean fromUser, int progress) {}
    public void setProgress(int p, boolean animate) { setProgress(p); }
    public synchronized int getProgress() { return indeterminate ? 0 : progress; }
    public synchronized void setSecondaryProgress(int p) { secondary = Math.max(min, Math.min(max, p)); invalidate(); }
    public synchronized int getSecondaryProgress() { return indeterminate ? 0 : secondary; }
    public final synchronized void incrementProgressBy(int d) { setProgress(progress + d); }
    public final synchronized void incrementSecondaryProgressBy(int d) { setSecondaryProgress(secondary + d); }
    public synchronized boolean isIndeterminate() { return indeterminate; }
    public synchronized void setIndeterminate(boolean b) { if (onlyIndeterminate && !b) return; indeterminate = b; invalidate(); }
    public android.graphics.drawable.Drawable getIndeterminateDrawable() { return indDrawable; }
    public void setIndeterminateDrawable(android.graphics.drawable.Drawable d) { if (indDrawable != d) { indDrawable = d; invalidate(); } }
    public void setIndeterminateDrawableTiled(android.graphics.drawable.Drawable d) { setIndeterminateDrawable(d); }
    public android.graphics.drawable.Drawable getProgressDrawable() { return progDrawable; }
    public void setProgressDrawable(android.graphics.drawable.Drawable d) { if (progDrawable != d) { progDrawable = d; invalidate(); } }
    public void setProgressDrawableTiled(android.graphics.drawable.Drawable d) { setProgressDrawable(d); }
    public android.graphics.drawable.Drawable getCurrentDrawable() { return indeterminate ? indDrawable : progDrawable; }
    public void setInterpolator(android.view.animation.Interpolator i) { interp = i; }
    public void setInterpolator(Context c, int resID) {}
    public android.view.animation.Interpolator getInterpolator() { return interp; }
    public boolean isAnimating() { return false; }
    public void setProgressTintList(android.content.res.ColorStateList t) { progTint = t; } public android.content.res.ColorStateList getProgressTintList() { return progTint; }
    public void setIndeterminateTintList(android.content.res.ColorStateList t) { indTint = t; } public android.content.res.ColorStateList getIndeterminateTintList() { return indTint; }
    public void setProgressBackgroundTintList(android.content.res.ColorStateList t) { progBgTint = t; } public android.content.res.ColorStateList getProgressBackgroundTintList() { return progBgTint; }
    public void setSecondaryProgressTintList(android.content.res.ColorStateList t) { secTint = t; } public android.content.res.ColorStateList getSecondaryProgressTintList() { return secTint; }
    public void setProgressTintMode(android.graphics.PorterDuff.Mode m) {} public void setIndeterminateTintMode(android.graphics.PorterDuff.Mode m) {}
    public void setProgressBackgroundTintMode(android.graphics.PorterDuff.Mode m) {} public void setSecondaryProgressTintMode(android.graphics.PorterDuff.Mode m) {}
    public void setMinWidth(int w) {} public void setMaxWidth(int w) {} public void setMinHeight(int h) {} public void setMaxHeight(int h) {}
    @Override public CharSequence getAccessibilityClassName() { return ProgressBar.class.getName(); }
    @Override protected void onMeasure(int ws, int hs) {
        float d = res().density;
        if (indeterminate && progDrawable == null || onlyIndeterminate) { int sz = (int) (48 * d); setMeasuredDimension(resolveSize(sz + mPaddingLeft + mPaddingRight, ws), resolveSize(sz + mPaddingTop + mPaddingBottom, hs)); return; }
        setMeasuredDimension(resolveSize((int) (100 * d), ws), resolveSize((int) (4 * d) + mPaddingTop + mPaddingBottom, hs));
    }
    @Override protected void onDraw(android.graphics.Canvas g) {
        int l = mPaddingLeft, t = mPaddingTop, r = getWidth() - mPaddingRight, b = getHeight() - mPaddingBottom;
        if (r <= l || b <= t) return;
        int acc = accent();
        if (indeterminate) {
            if (indDrawable != null && !(indDrawable instanceof android.graphics.drawable.ColorDrawable)) { indDrawable.setBounds(l, t, r, b); if (indTint != null) indDrawable.setTintList(indTint); indDrawable.draw(g); return; }
            // Material circular indeterminate spinner (static frame of the rotating arc), AOSP stroke = 4dp at 48dp.
            float sz = Math.min(r - l, b - t), sw = Math.max(1f, sz / 12f), cx = (l + r) / 2f, cy = (t + b) / 2f, rad = sz / 2f - sw / 2f - sz * 0.1f;
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            p.setStyle(android.graphics.Paint.Style.STROKE); p.setStrokeWidth(sw); p.setStrokeCap(android.graphics.Paint.Cap.SQUARE);
            p.setColor(indTint != null ? indTint.getDefaultColor() : acc);
            g.drawArc(new android.graphics.RectF(cx - rad, cy - rad, cx + rad, cy + rad), -90f, 270f, false, p);
            return;
        }
        if (progDrawable != null) { progDrawable.setBounds(l, t, r, b); progDrawable.setLevel(max > min ? (int) (10000L * (progress - min) / (max - min)) : 0); progDrawable.draw(g); return; }
        int fg = progTint != null ? progTint.getDefaultColor() : acc;
        int bg = progBgTint != null ? progBgTint.getDefaultColor() : (fg & 0x00FFFFFF) | 0x4C000000;
        android.graphics.Paint p = new android.graphics.Paint(); p.setColor(bg); g.drawRect(l, t, r, b, p);
        if (secondary > min && max > min) { p.setColor(secTint != null ? secTint.getDefaultColor() : (fg & 0x00FFFFFF) | 0x80000000); g.drawRect(l, t, l + (r - l) * (secondary - min) / (max - min), b, p); }
        int w = max <= min ? 0 : (r - l) * (progress - min) / (max - min);
        p.setColor(fg); g.drawRect(l, t, l + w, b, p);
    }
}
