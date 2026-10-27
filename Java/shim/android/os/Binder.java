package android.os;
/** In-process Binder: local interface lookup and identity only (there is no IPC on this host). */
public class Binder implements IBinder {
    private IInterface mOwner; private String mDescriptor;
    public Binder() {} public Binder(String descriptor) { mDescriptor = descriptor; }
    public void attachInterface(IInterface owner, String descriptor) { mOwner = owner; mDescriptor = descriptor; }
    public String getInterfaceDescriptor() { return mDescriptor; }
    public IInterface queryLocalInterface(String descriptor) { return mDescriptor != null && mDescriptor.equals(descriptor) ? mOwner : null; }
    public boolean pingBinder() { return true; } public boolean isBinderAlive() { return true; }
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException { return false; }
    public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException { return onTransact(code, data, reply, flags); }
    public void dump(java.io.FileDescriptor fd, String[] args) {}
    protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter pw, String[] args) {}
    public static final int getCallingUid() { return Process.myUid(); }
    public static final int getCallingPid() { return Process.myPid(); }
    public static final UserHandle getCallingUserHandle() { return Process.myUserHandle(); }
    public static final int getCallingUidOrThrow() { return Process.myUid(); }
    public static final long clearCallingIdentity() { return ((long) Process.myUid() << 32) | (Process.myPid() & 0xffffffffL); }
    public static final void restoreCallingIdentity(long token) {}
    public static final void flushPendingCommands() {}
    public static final void joinThreadPool() {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public static long setCallingWorkSourceUid(int p0) { jet.runtime.ShimStubs.hit("android.os.Binder.setCallingWorkSourceUid(I)J"); return 0; }
    public static int getCallingWorkSourceUid() { jet.runtime.ShimStubs.hit("android.os.Binder.getCallingWorkSourceUid()I"); return 0; }
    public static long clearCallingWorkSource() { jet.runtime.ShimStubs.hit("android.os.Binder.clearCallingWorkSource()J"); return 0; }
    public static void restoreCallingWorkSource(long p0) { jet.runtime.ShimStubs.hit("android.os.Binder.restoreCallingWorkSource(J)V"); }
    public void dumpAsync(java.io.FileDescriptor p0, java.lang.String[] p1) { jet.runtime.ShimStubs.hit("android.os.Binder.dumpAsync(Ljava/io/FileDescriptor;[Ljava/lang/String;)V"); }
    // ---- END generated SDK surface ----
}
