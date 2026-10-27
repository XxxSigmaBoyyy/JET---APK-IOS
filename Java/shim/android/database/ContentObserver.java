package android.database;
public abstract class ContentObserver {
    public ContentObserver(android.os.Handler h) {}
    public boolean deliverSelfNotifications() { return false; }
    public void onChange(boolean self) {}
    public void onChange(boolean self, android.net.Uri uri) { onChange(self); }
    public final void dispatchChange(boolean self) { onChange(self); }
}
