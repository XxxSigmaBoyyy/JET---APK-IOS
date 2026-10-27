package android.database.sqlite;
/** AOSP-shaped reference-counted closable (SQLiteDatabase, SQLiteProgram, ...). */
public abstract class SQLiteClosable implements java.io.Closeable {
    private int mReferenceCount = 1;
    protected abstract void onAllReferencesReleased();
    @Deprecated protected void onAllReferencesReleasedFromContainer() { onAllReferencesReleased(); }
    public void acquireReference() {
        synchronized (this) { if (mReferenceCount <= 0) throw new IllegalStateException("attempt to re-open an already-closed object: " + this); mReferenceCount++; }
    }
    public void releaseReference() { boolean last; synchronized (this) { last = --mReferenceCount == 0; } if (last) onAllReferencesReleased(); }
    @Deprecated public void releaseReferenceFromContainer() { releaseReference(); }
    public void close() { releaseReference(); }
}
