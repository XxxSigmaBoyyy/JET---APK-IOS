package android.os;
public final class UserHandle implements Parcelable {
    private static final int PER_USER_RANGE = 100000;
    private final int mHandle;
    public UserHandle(Parcel in) { this(0); }
    private UserHandle(int h) { mHandle = h; }
    public static UserHandle getUserHandleForUid(int uid) { return new UserHandle(uid / PER_USER_RANGE); }
    public static UserHandle readFromParcel(Parcel in) { return new UserHandle(0); }
    public static void writeToParcel(UserHandle h, Parcel out) {}
    public int jetId() { return mHandle; }
    @Override public boolean equals(Object o) { return o instanceof UserHandle && ((UserHandle) o).mHandle == mHandle; }
    @Override public int hashCode() { return mHandle; }
    @Override public String toString() { return "UserHandle{" + mHandle + "}"; }
    public int describeContents() { return 0; }
    public void writeToParcel(Parcel out, int flags) {}
    public static final Parcelable.Creator<UserHandle> CREATOR = new Parcelable.Creator<UserHandle>() { public UserHandle createFromParcel(Parcel in) { return new UserHandle(0); } public UserHandle[] newArray(int n) { return new UserHandle[n]; } };
}
