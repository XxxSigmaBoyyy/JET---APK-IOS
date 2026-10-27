package android.net;
/** Per-UID/interface byte counters are not available on this host: UNSUPPORTED (-1), as AOSP returns when the kernel lacks them. Tagging is per-thread state. */
public class TrafficStats {
    public static final int UNSUPPORTED = -1;
    private static final ThreadLocal<Integer> TAG = ThreadLocal.withInitial(() -> -1);
    public TrafficStats() {}
    public static void setThreadStatsTag(int tag) { TAG.set(tag); } public static int getThreadStatsTag() { return TAG.get(); }
    public static int getAndSetThreadStatsTag(int tag) { int o = TAG.get(); TAG.set(tag); return o; }
    public static void clearThreadStatsTag() { TAG.set(-1); }
    public static void setThreadStatsUid(int uid) {} public static void clearThreadStatsUid() {}
    public static void tagSocket(java.net.Socket s) {} public static void untagSocket(java.net.Socket s) {}
    public static void tagDatagramSocket(java.net.DatagramSocket s) {} public static void untagDatagramSocket(java.net.DatagramSocket s) {}
    public static void tagFileDescriptor(java.io.FileDescriptor fd) {} public static void untagFileDescriptor(java.io.FileDescriptor fd) {}
    public static void incrementOperationCount(int c) {} public static void incrementOperationCount(int tag, int c) {}
    public static long getMobileTxPackets() { return UNSUPPORTED; } public static long getMobileRxPackets() { return UNSUPPORTED; }
    public static long getMobileTxBytes() { return UNSUPPORTED; } public static long getMobileRxBytes() { return UNSUPPORTED; }
    public static long getTotalTxPackets() { return UNSUPPORTED; } public static long getTotalRxPackets() { return UNSUPPORTED; }
    public static long getTotalTxBytes() { return UNSUPPORTED; } public static long getTotalRxBytes() { return UNSUPPORTED; }
    public static long getUidTxBytes(int uid) { return UNSUPPORTED; } public static long getUidRxBytes(int uid) { return UNSUPPORTED; }
    public static long getUidTxPackets(int uid) { return UNSUPPORTED; } public static long getUidRxPackets(int uid) { return UNSUPPORTED; }
    public static long getTxBytes(String iface) { return UNSUPPORTED; } public static long getRxBytes(String iface) { return UNSUPPORTED; }
    public static long getTxPackets(String iface) { return UNSUPPORTED; } public static long getRxPackets(String iface) { return UNSUPPORTED; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public static int getThreadStatsUid() { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getThreadStatsUid()I"); return 0; }
    public static long getUidTcpTxBytes(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidTcpTxBytes(I)J"); return 0; }
    public static long getUidTcpRxBytes(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidTcpRxBytes(I)J"); return 0; }
    public static long getUidUdpTxBytes(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidUdpTxBytes(I)J"); return 0; }
    public static long getUidUdpRxBytes(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidUdpRxBytes(I)J"); return 0; }
    public static long getUidTcpTxSegments(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidTcpTxSegments(I)J"); return 0; }
    public static long getUidTcpRxSegments(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidTcpRxSegments(I)J"); return 0; }
    public static long getUidUdpTxPackets(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidUdpTxPackets(I)J"); return 0; }
    public static long getUidUdpRxPackets(int p0) { jet.runtime.ShimStubs.hit("android.net.TrafficStats.getUidUdpRxPackets(I)J"); return 0; }
    // ---- END generated SDK surface ----
}
