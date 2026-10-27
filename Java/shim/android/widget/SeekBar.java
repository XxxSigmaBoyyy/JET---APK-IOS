package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class SeekBar extends AbsSeekBar {
    public interface OnSeekBarChangeListener { void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser); void onStartTrackingTouch(SeekBar seekBar); void onStopTrackingTouch(SeekBar seekBar); }
    private OnSeekBarChangeListener mListener;
    public SeekBar(Context c) { this(c, null); }
    public SeekBar(Context c, AttributeSet a) { this(c, a, 0x0101007b /*seekBarStyle*/); }
    public SeekBar(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public SeekBar(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a, defStyleAttr, defStyleRes); }
    @Override void onProgressRefresh(float scale, boolean fromUser, int progress) { super.onProgressRefresh(scale, fromUser, progress); if (mListener != null) mListener.onProgressChanged(this, progress, fromUser); }
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) { mListener = l; }
    @Override void onStartTrackingTouch() { super.onStartTrackingTouch(); if (mListener != null) mListener.onStartTrackingTouch(this); }
    @Override void onStopTrackingTouch() { super.onStopTrackingTouch(); if (mListener != null) mListener.onStopTrackingTouch(this); }
    @Override public CharSequence getAccessibilityClassName() { return SeekBar.class.getName(); }
}
