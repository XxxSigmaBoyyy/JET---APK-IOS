package android.app;
/** JET: API 30 ApplicationExitInfo (never produced by the host; see ActivityManager.getHistoricalProcessExitReasons). */
public final class ApplicationExitInfo implements android.os.Parcelable {
    public static final int REASON_UNKNOWN = 0, REASON_EXIT_SELF = 1, REASON_SIGNALED = 2, REASON_LOW_MEMORY = 3, REASON_CRASH = 4, REASON_CRASH_NATIVE = 5, REASON_ANR = 6,
        REASON_INITIALIZATION_FAILURE = 7, REASON_PERMISSION_CHANGE = 8, REASON_EXCESSIVE_RESOURCE_USAGE = 9, REASON_USER_REQUESTED = 10, REASON_USER_STOPPED = 11,
        REASON_DEPENDENCY_DIED = 12, REASON_OTHER = 13, REASON_FREEZER = 14, REASON_PACKAGE_STATE_CHANGE = 15, REASON_PACKAGE_UPDATED = 16;
    public int getReason() { return REASON_UNKNOWN; } public int getStatus() { return 0; } public int getPid() { return 0; } public int getRealUid() { return 0; }
    public long getTimestamp() { return 0; } public String getDescription() { return null; } public String getProcessName() { return null; } public int getImportance() { return 0; }
    public long getPss() { return 0; } public long getRss() { return 0; } public java.io.InputStream getTraceInputStream() { return null; } public byte[] getProcessStateSummary() { return null; }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
}
