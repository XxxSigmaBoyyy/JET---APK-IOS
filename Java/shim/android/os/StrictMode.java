package android.os;
import java.util.*;
/** StrictMode policies are real value objects (get/set round-trips, allow* returns the previous policy);
 *  violations are not detected on this host, so penalties never trigger. */
public final class StrictMode {
    private StrictMode() {}
    public interface OnThreadViolationListener { void onThreadViolation(android.os.strictmode.Violation v); }
    public interface OnVmViolationListener { void onVmViolation(android.os.strictmode.Violation v); }
    public static final class ThreadPolicy {
        public static final ThreadPolicy LAX = new ThreadPolicy(Collections.<String>emptySet());
        final Set<String> m; ThreadPolicy(Set<String> m) { this.m = m; }
        @Override public String toString() { return "[StrictMode.ThreadPolicy; mask=" + m + "]"; }
        public static final class Builder {
            private final Set<String> m = new LinkedHashSet<>();
            public Builder() {} public Builder(ThreadPolicy p) { m.addAll(p.m); }
            public Builder detectAll() { m.add("detectAll"); return this; }
            public Builder detectCustomSlowCalls() { m.add("detectCustomSlowCalls"); return this; }
            public Builder detectDiskReads() { m.add("detectDiskReads"); return this; }
            public Builder detectDiskWrites() { m.add("detectDiskWrites"); return this; }
            public Builder detectExplicitGc() { m.add("detectExplicitGc"); return this; }
            public Builder detectNetwork() { m.add("detectNetwork"); return this; }
            public Builder detectResourceMismatches() { m.add("detectResourceMismatches"); return this; }
            public Builder detectUnbufferedIo() { m.add("detectUnbufferedIo"); return this; }
            public Builder penaltyDeath() { m.add("penaltyDeath"); return this; }
            public Builder penaltyDeathOnNetwork() { m.add("penaltyDeathOnNetwork"); return this; }
            public Builder penaltyDialog() { m.add("penaltyDialog"); return this; }
            public Builder penaltyDropBox() { m.add("penaltyDropBox"); return this; }
            public Builder penaltyFlashScreen() { m.add("penaltyFlashScreen"); return this; }
            public Builder penaltyListener(java.util.concurrent.Executor p0, android.os.StrictMode.OnThreadViolationListener p1) { m.add("penaltyListener"); return this; }
            public Builder penaltyLog() { m.add("penaltyLog"); return this; }
            public Builder permitAll() { m.add("permitAll"); return this; }
            public Builder permitCustomSlowCalls() { m.add("permitCustomSlowCalls"); return this; }
            public Builder permitDiskReads() { m.add("permitDiskReads"); return this; }
            public Builder permitDiskWrites() { m.add("permitDiskWrites"); return this; }
            public Builder permitExplicitGc() { m.add("permitExplicitGc"); return this; }
            public Builder permitNetwork() { m.add("permitNetwork"); return this; }
            public Builder permitResourceMismatches() { m.add("permitResourceMismatches"); return this; }
            public Builder permitUnbufferedIo() { m.add("permitUnbufferedIo"); return this; }
            public ThreadPolicy build() { return new ThreadPolicy(new LinkedHashSet<>(m)); }
        }
    }
    public static final class VmPolicy {
        public static final VmPolicy LAX = new VmPolicy(Collections.<String>emptySet());
        final Set<String> m; VmPolicy(Set<String> m) { this.m = m; }
        @Override public String toString() { return "[StrictMode.VmPolicy; mask=" + m + "]"; }
        public static final class Builder {
            private final Set<String> m = new LinkedHashSet<>();
            public Builder() {} public Builder(VmPolicy p) { m.addAll(p.m); }
            public Builder detectActivityLeaks() { m.add("detectActivityLeaks"); return this; }
            public Builder detectAll() { m.add("detectAll"); return this; }
            public Builder detectCleartextNetwork() { m.add("detectCleartextNetwork"); return this; }
            public Builder detectContentUriWithoutPermission() { m.add("detectContentUriWithoutPermission"); return this; }
            public Builder detectCredentialProtectedWhileLocked() { m.add("detectCredentialProtectedWhileLocked"); return this; }
            public Builder detectFileUriExposure() { m.add("detectFileUriExposure"); return this; }
            public Builder detectImplicitDirectBoot() { m.add("detectImplicitDirectBoot"); return this; }
            public Builder detectIncorrectContextUse() { m.add("detectIncorrectContextUse"); return this; }
            public Builder detectLeakedClosableObjects() { m.add("detectLeakedClosableObjects"); return this; }
            public Builder detectLeakedRegistrationObjects() { m.add("detectLeakedRegistrationObjects"); return this; }
            public Builder detectLeakedSqlLiteObjects() { m.add("detectLeakedSqlLiteObjects"); return this; }
            public Builder detectNonSdkApiUsage() { m.add("detectNonSdkApiUsage"); return this; }
            public Builder detectUnsafeIntentLaunch() { m.add("detectUnsafeIntentLaunch"); return this; }
            public Builder detectUntaggedSockets() { m.add("detectUntaggedSockets"); return this; }
            public Builder penaltyDeath() { m.add("penaltyDeath"); return this; }
            public Builder penaltyDeathOnCleartextNetwork() { m.add("penaltyDeathOnCleartextNetwork"); return this; }
            public Builder penaltyDeathOnFileUriExposure() { m.add("penaltyDeathOnFileUriExposure"); return this; }
            public Builder penaltyDropBox() { m.add("penaltyDropBox"); return this; }
            public Builder penaltyListener(java.util.concurrent.Executor p0, android.os.StrictMode.OnVmViolationListener p1) { m.add("penaltyListener"); return this; }
            public Builder penaltyLog() { m.add("penaltyLog"); return this; }
            public Builder permitNonSdkApiUsage() { m.add("permitNonSdkApiUsage"); return this; }
            public Builder permitUnsafeIntentLaunch() { m.add("permitUnsafeIntentLaunch"); return this; }
            public Builder setClassInstanceLimit(java.lang.Class p0, int p1) { m.add("setClassInstanceLimit"); return this; }
            public VmPolicy build() { return new VmPolicy(new LinkedHashSet<>(m)); }
        }
    }
    private static final ThreadLocal<ThreadPolicy> TP = ThreadLocal.withInitial(() -> ThreadPolicy.LAX);
    private static volatile VmPolicy sVm = VmPolicy.LAX;
    public static void setThreadPolicy(ThreadPolicy p) { TP.set(p == null ? ThreadPolicy.LAX : p); }
    public static ThreadPolicy getThreadPolicy() { return TP.get(); }
    public static ThreadPolicy allowThreadDiskWrites() { return TP.get(); }
    public static ThreadPolicy allowThreadDiskReads() { return TP.get(); }
    public static void setVmPolicy(VmPolicy p) { sVm = p == null ? VmPolicy.LAX : p; }
    public static VmPolicy getVmPolicy() { return sVm; }
    public static void enableDefaults() { setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().build()); setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build()); }
    public static void noteSlowCall(String name) {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
