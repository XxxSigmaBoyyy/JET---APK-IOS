package android.os;
/** Systrace markers: no tracer is attached on the host (or on iOS), so sections are balanced no-ops like an untraced device. */
public final class Trace {
    private Trace() {}
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    public static boolean isEnabled() { return false; }
    public static void beginSection(String sectionName) { if (sectionName != null && sectionName.length() > 127) throw new IllegalArgumentException("sectionName is too long"); DEPTH.get()[0]++; }
    public static void endSection() { int[] d = DEPTH.get(); if (d[0] > 0) d[0]--; }
    public static void beginAsyncSection(String methodName, int cookie) {}
    public static void endAsyncSection(String methodName, int cookie) {}
    public static void setCounter(String counterName, long counterValue) {}
}
