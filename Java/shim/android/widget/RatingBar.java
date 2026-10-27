package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import jet.runtime.XmlAttrs;
/** AOSP RatingBar: numStars stars, progress in stepSize units (max = numStars/stepSize). Stars drawn as filled/empty when no progressDrawable. */
public class RatingBar extends AbsSeekBar {
    public interface OnRatingBarChangeListener { void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser); }
    private int mNumStars = 5, mProgressOnStartTracking; private float mStepSize = 0.5f; private OnRatingBarChangeListener mListener; private boolean mIndicator;
    public RatingBar(Context c) { this(c, null); }
    public RatingBar(Context c, AttributeSet a) { this(c, a, 0x0101007c /*ratingBarStyle*/); }
    public RatingBar(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public RatingBar(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        XmlAttrs x = XmlAttrs.of(a); float rating = -1;
        if (x != null) { try { String v; if ((v = x.get("numStars")) != null) mNumStars = Math.max(1, jet.runtime.ResTable.intVal(res().resolve(v))); if ((v = x.get("stepSize")) != null) mStepSize = Float.parseFloat(res().resolve(v)); if ((v = x.get("rating")) != null) rating = Float.parseFloat(res().resolve(v)); if ((v = x.get("isIndicator")) != null) setIsIndicator("true".equals(res().resolve(v))); } catch (RuntimeException ignored) {} }
        setStepSize(mStepSize); if (rating >= 0) setRating(rating);
    }
    public void setOnRatingBarChangeListener(OnRatingBarChangeListener l) { mListener = l; } public OnRatingBarChangeListener getOnRatingBarChangeListener() { return mListener; }
    public void setIsIndicator(boolean i) { mIndicator = i; mIsUserSeekable = !i; setFocusable(!i); } public boolean isIndicator() { return mIndicator; }
    public void setNumStars(int n) { if (n <= 0) return; mNumStars = n; setStepSize(mStepSize); requestLayout(); } public int getNumStars() { return mNumStars; }
    private float perStep() { return getMax() / (float) mNumStars; }
    public void setRating(float rating) { setProgress(Math.round(rating * perStep())); }
    public float getRating() { return getProgress() / perStep(); }
    public void setStepSize(float stepSize) { if (stepSize <= 0) return; float r = getRating(); mStepSize = stepSize; setMax((int) Math.ceil(mNumStars / stepSize)); if (!Float.isNaN(r)) setProgress(Math.round(r * perStep())); }
    public float getStepSize() { return mNumStars / (float) getMax(); }
    @Override public synchronized void setMax(int max) { if (max <= 0) return; super.setMax(max); }
    @Override void onProgressRefresh(float scale, boolean fromUser, int progress) { super.onProgressRefresh(scale, fromUser, progress); if (mListener != null && !fromUser) mListener.onRatingChanged(this, getRating(), false); }
    @Override void onStartTrackingTouch() { mProgressOnStartTracking = getProgress(); super.onStartTrackingTouch(); }
    @Override void onStopTrackingTouch() { super.onStopTrackingTouch(); if (getProgress() != mProgressOnStartTracking && mListener != null) mListener.onRatingChanged(this, getRating(), true); }
    @Override void onKeyChange() { if (mListener != null) mListener.onRatingChanged(this, getRating(), true); }
    @Override protected void onMeasure(int ws, int hs) { int star = (int) (48 * res().density); setMeasuredDimension(resolveSize(star * mNumStars + mPaddingLeft + mPaddingRight, ws), resolveSize(star + mPaddingTop + mPaddingBottom, hs)); }
    @Override protected void onDraw(android.graphics.Canvas g) {
        if (getProgressDrawable() != null) { super.onDraw(g); return; }
        float sw = (getWidth() - mPaddingLeft - mPaddingRight) / (float) mNumStars, r = Math.min(sw, getHeight() - mPaddingTop - mPaddingBottom) * 0.45f, rating = getRating();
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < mNumStars; i++) {
            float cx = mPaddingLeft + sw * (i + 0.5f), cy = getHeight() / 2f; android.graphics.Path star = new android.graphics.Path();
            for (int k = 0; k < 10; k++) { double ang = Math.PI / 2 + k * Math.PI / 5; float rr = k % 2 == 0 ? r : r * 0.45f; float px = cx + (float) (rr * Math.cos(ang)), py = cy - (float) (rr * Math.sin(ang)); if (k == 0) star.moveTo(px, py); else star.lineTo(px, py); }
            star.close(); float fill = Math.max(0f, Math.min(1f, rating - i));
            p.setColor(0x42000000); g.drawPath(star, p);
            if (fill > 0) { g.save(); g.clipRect(cx - r, cy - r, cx - r + 2 * r * fill, cy + r); p.setColor(0xFFFFC107); g.drawPath(star, p); g.restore(); }
        }
    }
    @Override public CharSequence getAccessibilityClassName() { return RatingBar.class.getName(); }
}
