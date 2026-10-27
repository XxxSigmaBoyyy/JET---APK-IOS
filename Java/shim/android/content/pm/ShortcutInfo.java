package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.PersistableBundle;
import java.util.Set;

/** Launcher shortcut value object (data only on the host; iOS maps to UIApplicationShortcutItem later). */
public final class ShortcutInfo implements android.os.Parcelable {
    String mId, mPackage; CharSequence mShort, mLong, mDisabledMsg; Icon mIcon; Intent[] mIntents; ComponentName mActivity;
    Set<String> mCategories; PersistableBundle mExtras; int mRank; boolean mLongLived;
    private ShortcutInfo() {}
    public String getId() { return mId; }
    public String getPackage() { return mPackage; }
    public CharSequence getShortLabel() { return mShort; }
    public CharSequence getLongLabel() { return mLong; }
    public CharSequence getDisabledMessage() { return mDisabledMsg; }
    public Intent getIntent() { return mIntents == null || mIntents.length == 0 ? null : mIntents[mIntents.length - 1]; }
    public Intent[] getIntents() { return mIntents; }
    public ComponentName getActivity() { return mActivity; }
    public Set<String> getCategories() { return mCategories; }
    public PersistableBundle getExtras() { return mExtras; }
    public int getRank() { return mRank; }
    public boolean isEnabled() { return true; } public boolean isPinned() { return false; } public boolean isDynamic() { return true; }
    public boolean isDeclaredInManifest() { return false; } public boolean isImmutable() { return false; } public boolean isCached() { return false; }
    public long getLastChangedTimestamp() { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
    public static final class Builder {
        private final ShortcutInfo s = new ShortcutInfo();
        public Builder(Context context, String id) { s.mId = id; s.mPackage = context != null ? context.getPackageName() : null; }
        public Builder setShortLabel(CharSequence l) { s.mShort = l; return this; }
        public Builder setLongLabel(CharSequence l) { s.mLong = l; return this; }
        public Builder setDisabledMessage(CharSequence m) { s.mDisabledMsg = m; return this; }
        public Builder setIcon(Icon i) { s.mIcon = i; return this; }
        public Builder setIntent(Intent i) { return setIntents(new Intent[]{i}); }
        public Builder setIntents(Intent[] i) { s.mIntents = i; return this; }
        public Builder setActivity(ComponentName a) { s.mActivity = a; return this; }
        public Builder setCategories(Set<String> c) { s.mCategories = c; return this; }
        public Builder setExtras(PersistableBundle e) { s.mExtras = e; return this; }
        public Builder setRank(int r) { s.mRank = r; return this; }
        public Builder setLongLived(boolean b) { s.mLongLived = b; return this; }
        public Builder setLocusId(Object l) { return this; }
        public Builder setPerson(Object p) { return this; }
        public Builder setPersons(Object[] p) { return this; }
        public Builder setExcludedFromSurfaces(int f) { return this; }
        public ShortcutInfo build() {
            if (s.mId == null || s.mId.isEmpty()) throw new IllegalArgumentException("Shortcut ID must be provided");
            return s;
        }
    }
}
