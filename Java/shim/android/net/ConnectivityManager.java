package android.net;
/** JET: one always-online, unmetered Wi-Fi network (Linux-only harness uses the host connection; iOS maps to NWPathMonitor). */
public class ConnectivityManager {
    /** AOSP (deprecated API 21, @removed from SDK 29 stubs but still in the framework as a no-op): legacy apps call it. */
    public void setNetworkPreference(int preference) {}
    public int getNetworkPreference() { return -1; }
    public static final int TYPE_NONE = -1, TYPE_MOBILE = 0, TYPE_WIFI = 1, TYPE_ETHERNET = 9, DEFAULT_NETWORK_PREFERENCE = 1;
    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1, RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2, RESTRICT_BACKGROUND_STATUS_ENABLED = 3;
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE", EXTRA_NO_CONNECTIVITY = "noConnectivity", EXTRA_NETWORK_INFO = "networkInfo", EXTRA_NETWORK_TYPE = "networkType", ACTION_RESTRICT_BACKGROUND_CHANGED = "android.net.conn.RESTRICT_BACKGROUND_CHANGED";
    public static class NetworkCallback {
        public NetworkCallback() {}
        public NetworkCallback(int flags) {}
        public void onAvailable(Network network) {}
        public void onLosing(Network network, int maxMsToLive) {}
        public void onLost(Network network) {}
        public void onUnavailable() {}
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {}
        public void onBlockedStatusChanged(Network network, boolean blocked) {}
    }
    public interface OnNetworkActiveListener { void onNetworkActive(); }
    private final java.util.List<NetworkCallback> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public ConnectivityManager() {}
    public Network getActiveNetwork() { return Network.JET_DEFAULT; }
    public Network[] getAllNetworks() { return new Network[]{ Network.JET_DEFAULT }; }
    public NetworkCapabilities getNetworkCapabilities(Network n) { return Network.JET_DEFAULT.equals(n) ? NetworkCapabilities.jetDefault() : null; }
    public NetworkInfo getActiveNetworkInfo() { return new NetworkInfo(TYPE_WIFI); }
    public NetworkInfo getNetworkInfo(int type) { return type == TYPE_WIFI ? new NetworkInfo(TYPE_WIFI) : null; }
    public NetworkInfo getNetworkInfo(Network n) { return Network.JET_DEFAULT.equals(n) ? new NetworkInfo(TYPE_WIFI) : null; }
    public NetworkInfo[] getAllNetworkInfo() { return new NetworkInfo[]{ new NetworkInfo(TYPE_WIFI) }; }
    public boolean isActiveNetworkMetered() { return false; }
    public boolean isDefaultNetworkActive() { return true; }
    public int getRestrictBackgroundStatus() { return RESTRICT_BACKGROUND_STATUS_DISABLED; }
    public boolean bindProcessToNetwork(Network n) { return true; }
    public Network getBoundNetworkForProcess() { return null; }
    public void addDefaultNetworkActiveListener(OnNetworkActiveListener l) {}
    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener l) {}
    private void deliver(NetworkCallback cb, android.os.Handler h) {
        Runnable r = () -> { cb.onAvailable(Network.JET_DEFAULT); cb.onCapabilitiesChanged(Network.JET_DEFAULT, NetworkCapabilities.jetDefault()); };
        android.os.Handler hh = h != null ? h : new android.os.Handler(android.os.Looper.getMainLooper()); hh.post(r);
    }
    public void registerNetworkCallback(NetworkRequest req, NetworkCallback cb) { registerNetworkCallback(req, cb, null); }
    public void registerNetworkCallback(NetworkRequest req, NetworkCallback cb, android.os.Handler h) { callbacks.add(cb); deliver(cb, h); }
    public void registerDefaultNetworkCallback(NetworkCallback cb) { registerDefaultNetworkCallback(cb, null); }
    public void registerDefaultNetworkCallback(NetworkCallback cb, android.os.Handler h) { callbacks.add(cb); deliver(cb, h); }
    public void requestNetwork(NetworkRequest req, NetworkCallback cb) { callbacks.add(cb); deliver(cb, null); }
    public void unregisterNetworkCallback(NetworkCallback cb) { callbacks.remove(cb); }
    
}
