package android.view.autofill;
public final class AutofillValue implements android.os.Parcelable {
    private final int mType; private final Object mValue;
    private AutofillValue(int type, Object v) { mType = type; mValue = v; }
    public static AutofillValue forText(CharSequence v) { return v == null ? null : new AutofillValue(1, v); }
    public static AutofillValue forToggle(boolean v) { return new AutofillValue(2, v); }
    public static AutofillValue forList(int v) { return new AutofillValue(3, v); }
    public static AutofillValue forDate(long v) { return new AutofillValue(4, v); }
    public CharSequence getTextValue() { if (mType != 1) throw new IllegalStateException("value is not text: " + this); return (CharSequence) mValue; }
    public boolean isText() { return mType == 1; }
    public boolean getToggleValue() { if (mType != 2) throw new IllegalStateException("value is not a toggle: " + this); return (Boolean) mValue; }
    public boolean isToggle() { return mType == 2; }
    public int getListValue() { if (mType != 3) throw new IllegalStateException("value is not a list: " + this); return (Integer) mValue; }
    public boolean isList() { return mType == 3; }
    public long getDateValue() { if (mType != 4) throw new IllegalStateException("value is not a date: " + this); return (Long) mValue; }
    public boolean isDate() { return mType == 4; }
    @Override public int hashCode() { return mType + java.util.Objects.hashCode(mValue); }
    @Override public boolean equals(Object o) { return o instanceof AutofillValue && ((AutofillValue) o).mType == mType && java.util.Objects.equals(String.valueOf(((AutofillValue) o).mValue), String.valueOf(mValue)); }
    @Override public String toString() { return "[type=" + mType + ", value=" + mValue + "]"; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
