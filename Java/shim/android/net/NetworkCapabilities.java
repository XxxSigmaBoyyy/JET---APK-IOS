package android.net;
public final class NetworkCapabilities {
    public static final int NET_CAPABILITY_MMS = 0, NET_CAPABILITY_SUPL = 1, NET_CAPABILITY_DUN = 2, NET_CAPABILITY_FOTA = 3, NET_CAPABILITY_IMS = 4,
        NET_CAPABILITY_CBS = 5, NET_CAPABILITY_WIFI_P2P = 6, NET_CAPABILITY_IA = 7, NET_CAPABILITY_RCS = 8, NET_CAPABILITY_XCAP = 9, NET_CAPABILITY_EIMS = 10,
        NET_CAPABILITY_NOT_METERED = 11, NET_CAPABILITY_INTERNET = 12, NET_CAPABILITY_NOT_RESTRICTED = 13, NET_CAPABILITY_TRUSTED = 14, NET_CAPABILITY_NOT_VPN = 15,
        NET_CAPABILITY_VALIDATED = 16, NET_CAPABILITY_CAPTIVE_PORTAL = 17, NET_CAPABILITY_NOT_ROAMING = 18, NET_CAPABILITY_FOREGROUND = 19, NET_CAPABILITY_NOT_CONGESTED = 20,
        NET_CAPABILITY_NOT_SUSPENDED = 21, NET_CAPABILITY_TEMPORARILY_NOT_METERED = 25;
    public static final int TRANSPORT_CELLULAR = 0, TRANSPORT_WIFI = 1, TRANSPORT_BLUETOOTH = 2, TRANSPORT_ETHERNET = 3, TRANSPORT_VPN = 4, TRANSPORT_WIFI_AWARE = 5, TRANSPORT_LOWPAN = 6, TRANSPORT_USB = 8;
    long caps; long transports;
    public NetworkCapabilities() {}
    public NetworkCapabilities(NetworkCapabilities o) { if (o != null) { caps = o.caps; transports = o.transports; } }
    static NetworkCapabilities jetDefault() {
        NetworkCapabilities c = new NetworkCapabilities();
        for (int k : new int[]{NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED, NET_CAPABILITY_NOT_METERED, NET_CAPABILITY_NOT_RESTRICTED, NET_CAPABILITY_TRUSTED,
                NET_CAPABILITY_NOT_VPN, NET_CAPABILITY_NOT_ROAMING, NET_CAPABILITY_FOREGROUND, NET_CAPABILITY_NOT_CONGESTED, NET_CAPABILITY_NOT_SUSPENDED}) c.caps |= 1L << k;
        c.transports = 1L << TRANSPORT_WIFI; return c;
    }
    public boolean hasCapability(int c) { return c >= 0 && c < 64 && (caps & (1L << c)) != 0; }
    public boolean hasTransport(int t) { return t >= 0 && t < 64 && (transports & (1L << t)) != 0; }
    public int[] getCapabilities() { java.util.ArrayList<Integer> l = new java.util.ArrayList<>(); for (int i = 0; i < 64; i++) if (hasCapability(i)) l.add(i); int[] r = new int[l.size()]; for (int i = 0; i < r.length; i++) r[i] = l.get(i); return r; }
    public int getLinkDownstreamBandwidthKbps() { return 100_000; }
    public int getLinkUpstreamBandwidthKbps() { return 50_000; }
    public int getSignalStrength() { return -50; }
    @Override public String toString() { return "[ Transports: WIFI Capabilities: 0x" + Long.toHexString(caps) + " ]"; }
}
