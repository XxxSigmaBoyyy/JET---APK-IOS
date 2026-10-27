package android.widget;
import android.content.Context;
import android.util.AttributeSet;
/** JET: android.widget.TextClock (AOSP semantics): shows the current time formatted with format12Hour/format24Hour
 *  (chosen by DateFormat.is24HourFormat), optional timeZone, ticking once per minute (per second if the
 *  pattern has seconds) while attached. */
public class TextClock extends TextView {
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";
    private CharSequence mFormat12 = DEFAULT_FORMAT_12_HOUR, mFormat24 = DEFAULT_FORMAT_24_HOUR, mDescFormat12, mDescFormat24;
    private String mTimeZone;
    private boolean mTicking;
    private final Runnable mTicker = new Runnable() { public void run() { refreshTime(); if (mTicking) { long now = System.currentTimeMillis(); long step = hasSeconds() ? 1000 : 60000; postDelayed(this, step - now % step); } } };
    public TextClock(Context c) { this(c, null); }
    public TextClock(Context c, AttributeSet a) { this(c, a, 0); }
    public TextClock(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public TextClock(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) {
        super(c, a, defStyleAttr, defStyleRes);
        if (mJetAttrs != null) {
            String v;
            if ((v = mJetAttrs.get("format12Hour")) != null) mFormat12 = res(v);
            if ((v = mJetAttrs.get("format24Hour")) != null) mFormat24 = res(v);
            if ((v = mJetAttrs.get("timeZone")) != null) mTimeZone = res(v);
        }
        refreshTime();
    }
    private String res(String v) { try { return v.startsWith("@") ? getContext().getResources().getString(jet.runtime.ResTable.refId(v)) : v; } catch (Throwable t) { return v; } }
    public CharSequence getFormat12Hour() { return mFormat12; }
    public void setFormat12Hour(CharSequence f) { mFormat12 = f; refreshTime(); }
    public CharSequence getFormat24Hour() { return mFormat24; }
    public void setFormat24Hour(CharSequence f) { mFormat24 = f; refreshTime(); }
    public void setContentDescriptionFormat12Hour(CharSequence f) { mDescFormat12 = f; }
    public void setContentDescriptionFormat24Hour(CharSequence f) { mDescFormat24 = f; }
    public CharSequence getContentDescriptionFormat12Hour() { return mDescFormat12; }
    public CharSequence getContentDescriptionFormat24Hour() { return mDescFormat24; }
    public String getTimeZone() { return mTimeZone; }
    public void setTimeZone(String tz) { mTimeZone = tz; refreshTime(); }
    public boolean is24HourModeEnabled() { return android.text.format.DateFormat.is24HourFormat(getContext()); }
    public void refreshTime() {
        CharSequence f = is24HourModeEnabled() ? (mFormat24 != null ? mFormat24 : mFormat12) : (mFormat12 != null ? mFormat12 : mFormat24);
        if (f == null) f = is24HourModeEnabled() ? DEFAULT_FORMAT_24_HOUR : DEFAULT_FORMAT_12_HOUR;
        java.util.Calendar cal = mTimeZone != null ? java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(mTimeZone)) : java.util.Calendar.getInstance();
        try { setText(android.text.format.DateFormat.format(f, cal)); } catch (Throwable t) { setText(f); }
    }
    private boolean hasSeconds() { CharSequence f = is24HourModeEnabled() ? mFormat24 : mFormat12; return f != null && f.toString().indexOf('s') >= 0; }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); mTicking = true; removeCallbacks(mTicker); mTicker.run(); }
    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); mTicking = false; removeCallbacks(mTicker); }
    @Override public CharSequence getAccessibilityClassName() { return TextClock.class.getName(); }
}
