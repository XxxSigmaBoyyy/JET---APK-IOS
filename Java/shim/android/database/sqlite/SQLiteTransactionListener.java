package android.database.sqlite;
/** AOSP interface (API 11). */
public interface SQLiteTransactionListener {
    void onBegin();
    void onCommit();
    void onRollback();
}
