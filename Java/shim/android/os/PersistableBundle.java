package android.os;
public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable {
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {}
    public PersistableBundle() {}
    public static final PersistableBundle EMPTY = new PersistableBundle();
}
