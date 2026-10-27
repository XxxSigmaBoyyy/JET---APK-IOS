package android.view.accessibility;
public final class AccessibilityEvent extends AccessibilityRecord implements android.os.Parcelable {
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 4;
    public static final int CONTENT_CHANGE_TYPE_DRAG_CANCELLED = 512;
    public static final int CONTENT_CHANGE_TYPE_DRAG_DROPPED = 256;
    public static final int CONTENT_CHANGE_TYPE_DRAG_STARTED = 128;
    public static final int CONTENT_CHANGE_TYPE_PANE_APPEARED = 16;
    public static final int CONTENT_CHANGE_TYPE_PANE_DISAPPEARED = 32;
    public static final int CONTENT_CHANGE_TYPE_PANE_TITLE = 8;
    public static final int CONTENT_CHANGE_TYPE_STATE_DESCRIPTION = 64;
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 1;
    public static final int CONTENT_CHANGE_TYPE_TEXT = 2;
    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0;
    public static final int INVALID_POSITION = -1;
    public static final int MAX_TEXT_LENGTH = 500;
    public static final int SPEECH_STATE_LISTENING_END = 8;
    public static final int SPEECH_STATE_LISTENING_START = 4;
    public static final int SPEECH_STATE_SPEAKING_END = 2;
    public static final int SPEECH_STATE_SPEAKING_START = 1;
    public static final int TYPES_ALL_MASK = -1;
    public static final int TYPE_ANNOUNCEMENT = 16384;
    public static final int TYPE_ASSIST_READING_CONTEXT = 16777216;
    public static final int TYPE_GESTURE_DETECTION_END = 524288;
    public static final int TYPE_GESTURE_DETECTION_START = 262144;
    public static final int TYPE_NOTIFICATION_STATE_CHANGED = 64;
    public static final int TYPE_SPEECH_STATE_CHANGE = 33554432;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END = 1024;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START = 512;
    public static final int TYPE_TOUCH_INTERACTION_END = 2097152;
    public static final int TYPE_TOUCH_INTERACTION_START = 1048576;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUSED = 32768;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED = 65536;
    public static final int TYPE_VIEW_CLICKED = 1;
    public static final int TYPE_VIEW_CONTEXT_CLICKED = 8388608;
    public static final int TYPE_VIEW_FOCUSED = 8;
    public static final int TYPE_VIEW_HOVER_ENTER = 128;
    public static final int TYPE_VIEW_HOVER_EXIT = 256;
    public static final int TYPE_VIEW_LONG_CLICKED = 2;
    public static final int TYPE_VIEW_SCROLLED = 4096;
    public static final int TYPE_VIEW_SELECTED = 4;
    public static final int TYPE_VIEW_TEXT_CHANGED = 16;
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 8192;
    public static final int TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY = 131072;
    public static final int TYPE_WINDOWS_CHANGED = 4194304;
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 2048;
    public static final int TYPE_WINDOW_STATE_CHANGED = 32;
    public static final int WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED = 128;
    public static final int WINDOWS_CHANGE_ACTIVE = 32;
    public static final int WINDOWS_CHANGE_ADDED = 1;
    public static final int WINDOWS_CHANGE_BOUNDS = 8;
    public static final int WINDOWS_CHANGE_CHILDREN = 512;
    public static final int WINDOWS_CHANGE_FOCUSED = 64;
    public static final int WINDOWS_CHANGE_LAYER = 16;
    public static final int WINDOWS_CHANGE_PARENT = 256;
    public static final int WINDOWS_CHANGE_PIP = 1024;
    public static final int WINDOWS_CHANGE_REMOVED = 2;
    public static final int WINDOWS_CHANGE_TITLE = 4;

    private int mEventType, mAction, mGranularity, mContentChangeTypes, mWindowChanges; private long mEventTime; private CharSequence mPackageName;
    private final java.util.ArrayList<AccessibilityRecord> mRecords = new java.util.ArrayList<>();
    public AccessibilityEvent() {}
    public AccessibilityEvent(int type) { mEventType = type; }
    public AccessibilityEvent(AccessibilityEvent e) { super(e); mEventType = e.mEventType; mAction = e.mAction; mEventTime = e.mEventTime; mPackageName = e.mPackageName; }
    public static AccessibilityEvent obtain(int type) { return new AccessibilityEvent(type); }
    public static AccessibilityEvent obtain(AccessibilityEvent e) { return new AccessibilityEvent(e); }
    public static AccessibilityEvent obtain() { return new AccessibilityEvent(); }
    public int getEventType() { return mEventType; } public void setEventType(int t) { mEventType = t; }
    public long getEventTime() { return mEventTime; } public void setEventTime(long t) { mEventTime = t; }
    public CharSequence getPackageName() { return mPackageName; } public void setPackageName(CharSequence p) { mPackageName = p; }
    public int getAction() { return mAction; } public void setAction(int a) { mAction = a; }
    public int getMovementGranularity() { return mGranularity; } public void setMovementGranularity(int g) { mGranularity = g; }
    public int getContentChangeTypes() { return mContentChangeTypes; } public void setContentChangeTypes(int c) { mContentChangeTypes = c; }
    public int getWindowChanges() { return mWindowChanges; }
    public int getRecordCount() { return mRecords.size(); } public void appendRecord(AccessibilityRecord r) { mRecords.add(r); } public AccessibilityRecord getRecord(int i) { return mRecords.get(i); }
    public boolean isAccessibilityDataSensitive() { return false; } public void setAccessibilityDataSensitive(boolean b) {}
    public static String eventTypeToString(int t) { return "TYPE_" + Integer.toHexString(t); }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
    @Override public void recycle() {}
}
