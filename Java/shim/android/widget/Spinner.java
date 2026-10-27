package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class Spinner extends AbsSpinner implements android.content.DialogInterface.OnClickListener {
    public static final int MODE_DIALOG = 0, MODE_DROPDOWN = 1;
    private int mMode = MODE_DROPDOWN, mGravity = 0x800013; private CharSequence mPrompt; private android.graphics.drawable.Drawable mPopupBg;
    private int mDropW = -2, mVOff, mHOff;
    public Spinner(Context c) { this(c, (AttributeSet) null); }
    public Spinner(Context c, int mode) { this(c, null); mMode = mode; }
    public Spinner(Context c, AttributeSet a) { this(c, a, 0x01010081 /* spinnerStyle */); }
    public Spinner(Context c, AttributeSet a, int s) { super(c, a, s); } public Spinner(Context c, AttributeSet a, int s, int mode) { super(c, a, s); mMode = mode; }
    public Spinner(Context c, AttributeSet a, int s, int r, int mode) { super(c, a, s, r); mMode = mode; }
    public Spinner(Context c, AttributeSet a, int s, int r, int mode, android.content.res.Resources.Theme t) { super(c, a, s, r); mMode = mode; }
    public int getMode() { return mMode; } public Context getPopupContext() { return getContext(); }
    public void setPrompt(CharSequence p) { mPrompt = p; } public void setPromptId(int id) { mPrompt = getContext().getText(id); } public CharSequence getPrompt() { return mPrompt; }
    public void setGravity(int g) { mGravity = g; } public int getGravity() { return mGravity; }
    public void setPopupBackgroundDrawable(android.graphics.drawable.Drawable d) { mPopupBg = d; } public void setPopupBackgroundResource(int id) {} public android.graphics.drawable.Drawable getPopupBackground() { return mPopupBg; }
    public void setDropDownWidth(int w) { mDropW = w; } public int getDropDownWidth() { return mDropW; }
    public void setDropDownVerticalOffset(int o) { mVOff = o; } public int getDropDownVerticalOffset() { return mVOff; }
    public void setDropDownHorizontalOffset(int o) { mHOff = o; } public int getDropDownHorizontalOffset() { return mHOff; }
    public void onClick(android.content.DialogInterface d, int which) { setSelection(which); }
    @Override public boolean performClick() { return super.performClick(); }
}
