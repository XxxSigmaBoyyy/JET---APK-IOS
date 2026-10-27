package android.view.inputmethod;
public class EditorInfo implements android.text.InputType, android.os.Parcelable {
    public static final int IME_MASK_ACTION = 0xff, IME_ACTION_UNSPECIFIED = 0, IME_ACTION_NONE = 1, IME_ACTION_GO = 2, IME_ACTION_SEARCH = 3, IME_ACTION_SEND = 4, IME_ACTION_NEXT = 5, IME_ACTION_DONE = 6, IME_ACTION_PREVIOUS = 7;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000, IME_FLAG_NO_FULLSCREEN = 0x2000000, IME_FLAG_NAVIGATE_PREVIOUS = 0x4000000, IME_FLAG_NAVIGATE_NEXT = 0x8000000,
        IME_FLAG_NO_EXTRACT_UI = 0x10000000, IME_FLAG_NO_ACCESSORY_ACTION = 0x20000000, IME_FLAG_NO_ENTER_ACTION = 0x40000000, IME_FLAG_FORCE_ASCII = 0x80000000, IME_NULL = 0;
    public int inputType = TYPE_NULL, imeOptions = IME_NULL, actionId, initialSelStart = -1, initialSelEnd = -1, initialCapsMode, fieldId;
    public String privateImeOptions, packageName, fieldName; public CharSequence actionLabel, hintText, label; public android.os.Bundle extras; public android.os.LocaleList hintLocales; public String[] contentMimeTypes;
    private CharSequence mSurrounding; private int mSurroundingOffset;
    public void setInitialSurroundingText(CharSequence t) { setInitialSurroundingSubText(t, 0); }
    public void setInitialSurroundingSubText(CharSequence t, int off) { mSurrounding = t == null ? null : t.toString(); mSurroundingOffset = off; }
    private int rs() { return Math.min(initialSelStart, initialSelEnd) - mSurroundingOffset; } private int re() { return Math.max(initialSelStart, initialSelEnd) - mSurroundingOffset; }
    private boolean ok() { return mSurrounding != null && rs() >= 0 && re() <= mSurrounding.length(); }
    public CharSequence getInitialTextBeforeCursor(int n, int f) { if (!ok()) return null; int s = rs(); return mSurrounding.subSequence(Math.max(0, s - n), s); }
    public CharSequence getInitialSelectedText(int f) { if (!ok()) return null; return mSurrounding.subSequence(rs(), re()); }
    public CharSequence getInitialTextAfterCursor(int n, int f) { if (!ok()) return null; int e = re(); return mSurrounding.subSequence(e, Math.min(mSurrounding.length(), e + n)); }
    private java.util.List mHwGestures = java.util.Collections.emptyList(); private java.util.Set mHwPreviews = java.util.Collections.emptySet(); private int mToolType;
    public void setSupportedHandwritingGestures(java.util.List g) { mHwGestures = g == null ? java.util.Collections.emptyList() : g; }
    public java.util.List getSupportedHandwritingGestures() { return mHwGestures; }
    public void setSupportedHandwritingGesturePreviews(java.util.Set g) { mHwPreviews = g == null ? java.util.Collections.emptySet() : g; }
    public java.util.Set getSupportedHandwritingGesturePreviews() { return mHwPreviews; }
    public int getInitialToolType() { return mToolType; } public void setInitialToolType(int t) { mToolType = t; }
    public final void makeCompatible(int targetSdkVersion) {}
    public void dump(android.util.Printer pw, String prefix) { pw.println(prefix + "inputType=0x" + Integer.toHexString(inputType) + " imeOptions=0x" + Integer.toHexString(imeOptions)); }
    public void writeToParcel(android.os.Parcel p, int f) {} public int describeContents() { return 0; }
}
