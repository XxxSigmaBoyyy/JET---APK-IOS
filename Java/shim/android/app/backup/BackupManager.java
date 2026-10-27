package android.app.backup;
/** No backup transport on this host: change notifications are accepted and dropped, as when backup is disabled. */
public class BackupManager {
    public BackupManager(android.content.Context c) {}
    public void dataChanged() {}
    public static void dataChanged(String pkg) {}
    public boolean isBackupEnabled() { return false; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public android.os.UserHandle getUserForAncestralSerialNumber(long p0) { jet.runtime.ShimStubs.hit("android.app.backup.BackupManager.getUserForAncestralSerialNumber(J)Landroid/os/UserHandle;"); return null; }
    // ---- END generated SDK surface ----
}
