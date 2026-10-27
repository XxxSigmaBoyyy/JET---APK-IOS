package dalvik.system;
/** libcore hidden API used by Conscrypt/OkHttp platform code (StrictMode hooks). Headless host: permissive policy. */
public final class BlockGuard {
    private BlockGuard() {}
    public interface Policy {
        void onWriteToDisk(); void onReadFromDisk(); void onNetwork(); void onUnbufferedIO();
        default void onExplicitGc() {}
        int getPolicyMask();
    }
    public interface VmPolicy { void onPathAccess(String path); }
    public static final Policy LAX_POLICY = new Policy() {
        public void onWriteToDisk() {} public void onReadFromDisk() {} public void onNetwork() {} public void onUnbufferedIO() {}
        public int getPolicyMask() { return 0; }
    };
    public static final VmPolicy LAX_VM_POLICY = p -> {};
    private static final ThreadLocal<Policy> TP = ThreadLocal.withInitial(() -> LAX_POLICY);
    private static volatile VmPolicy vm = LAX_VM_POLICY;
    public static Policy getThreadPolicy() { return TP.get(); }
    public static void setThreadPolicy(Policy p) { TP.set(p == null ? LAX_POLICY : p); }
    public static VmPolicy getVmPolicy() { return vm; }
    public static void setVmPolicy(VmPolicy p) { vm = p == null ? LAX_VM_POLICY : p; }
}
