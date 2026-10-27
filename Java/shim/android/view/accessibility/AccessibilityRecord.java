package android.view.accessibility;
/** Host AccessibilityRecord: plain data holder (no accessibility service on host). */
public class AccessibilityRecord {
    private CharSequence mClassName, mBeforeText, mContentDescription; private android.os.Parcelable mParcelable; private final java.util.List<CharSequence> mText = new java.util.ArrayList<>();
    private android.view.View mSource;
    public AccessibilityRecord() {}
    public AccessibilityRecord(AccessibilityRecord r) { if (r != null) { mClassName = r.mClassName; mBeforeText = r.mBeforeText; mContentDescription = r.mContentDescription; mText.addAll(r.mText); mSource = r.mSource; } }
    private int mItemCount = -1; public int getItemCount() { return mItemCount; } public void setItemCount(int v) { mItemCount = v; }
    private int mCurrentItemIndex = -1; public int getCurrentItemIndex() { return mCurrentItemIndex; } public void setCurrentItemIndex(int v) { mCurrentItemIndex = v; }
    private int mFromIndex = -1; public int getFromIndex() { return mFromIndex; } public void setFromIndex(int v) { mFromIndex = v; }
    private int mToIndex = -1; public int getToIndex() { return mToIndex; } public void setToIndex(int v) { mToIndex = v; }
    private int mScrollX = 0; public int getScrollX() { return mScrollX; } public void setScrollX(int v) { mScrollX = v; }
    private int mScrollY = 0; public int getScrollY() { return mScrollY; } public void setScrollY(int v) { mScrollY = v; }
    private int mScrollDeltaX = -1; public int getScrollDeltaX() { return mScrollDeltaX; } public void setScrollDeltaX(int v) { mScrollDeltaX = v; }
    private int mScrollDeltaY = -1; public int getScrollDeltaY() { return mScrollDeltaY; } public void setScrollDeltaY(int v) { mScrollDeltaY = v; }
    private int mMaxScrollX = 0; public int getMaxScrollX() { return mMaxScrollX; } public void setMaxScrollX(int v) { mMaxScrollX = v; }
    private int mMaxScrollY = 0; public int getMaxScrollY() { return mMaxScrollY; } public void setMaxScrollY(int v) { mMaxScrollY = v; }
    private int mAddedCount = 0; public int getAddedCount() { return mAddedCount; } public void setAddedCount(int v) { mAddedCount = v; }
    private int mRemovedCount = 0; public int getRemovedCount() { return mRemovedCount; } public void setRemovedCount(int v) { mRemovedCount = v; }
    private boolean mChecked; public boolean isChecked() { return mChecked; } public void setChecked(boolean v) { mChecked = v; }
    private boolean mEnabled = true; public boolean isEnabled() { return mEnabled; } public void setEnabled(boolean v) { mEnabled = v; }
    private boolean mPassword; public boolean isPassword() { return mPassword; } public void setPassword(boolean v) { mPassword = v; }
    private boolean mFullScreen; public boolean isFullScreen() { return mFullScreen; } public void setFullScreen(boolean v) { mFullScreen = v; }
    private boolean mScrollable; public boolean isScrollable() { return mScrollable; } public void setScrollable(boolean v) { mScrollable = v; }
    public void setSource(android.view.View v) { mSource = v; } public void setSource(android.view.View v, int virtualId) { mSource = v; }
    public AccessibilityNodeInfo getSource() { return null; } public AccessibilityNodeInfo getSource(int prefetch) { return null; }
    public int getDisplayId() { return 0; } public int getWindowId() { return -1; }
    public CharSequence getClassName() { return mClassName; } public void setClassName(CharSequence c) { mClassName = c; }
    public java.util.List<CharSequence> getText() { return mText; }
    public CharSequence getBeforeText() { return mBeforeText; } public void setBeforeText(CharSequence c) { mBeforeText = c; }
    public CharSequence getContentDescription() { return mContentDescription; } public void setContentDescription(CharSequence c) { mContentDescription = c; }
    public android.os.Parcelable getParcelableData() { return mParcelable; } public void setParcelableData(android.os.Parcelable p) { mParcelable = p; }
    public static AccessibilityRecord obtain(AccessibilityRecord r) { return new AccessibilityRecord(r); }
    public static AccessibilityRecord obtain() { return new AccessibilityRecord(); }
    public void recycle() {}
}
