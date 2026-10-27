package android.view.inputmethod;
public final class CompletionInfo implements android.os.Parcelable {
    private final long mId; private final int mPosition; private final CharSequence mText, mLabel;
    public CompletionInfo(long id, int index, CharSequence text) { this(id, index, text, null); }
    public CompletionInfo(long id, int index, CharSequence text, CharSequence label) { mId = id; mPosition = index; mText = text; mLabel = label; }
    public long getId() { return mId; } public int getPosition() { return mPosition; } public CharSequence getText() { return mText; } public CharSequence getLabel() { return mLabel; }
    @Override public String toString() { return "CompletionInfo{#" + mPosition + " \"" + mText + "\" id=" + mId + " label=" + mLabel + "}"; }
    public void writeToParcel(android.os.Parcel p, int f) {} public int describeContents() { return 0; }
}
