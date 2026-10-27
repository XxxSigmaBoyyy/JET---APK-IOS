package android.net;
@Deprecated public class NetworkInfo {
    public enum State { CONNECTING, CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN }
    public enum DetailedState { IDLE, SCANNING, CONNECTING, AUTHENTICATING, OBTAINING_IPADDR, CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, FAILED, BLOCKED, VERIFYING_POOR_LINK, CAPTIVE_PORTAL_CHECK }
    private final int type;
    NetworkInfo(int type) { this.type = type; }
    public int getType() { return type; }
    public int getSubtype() { return 0; }
    public String getTypeName() { return type == ConnectivityManager.TYPE_WIFI ? "WIFI" : "MOBILE"; }
    public String getSubtypeName() { return ""; }
    public boolean isConnectedOrConnecting() { return true; }
    public boolean isConnected() { return true; }
    public boolean isAvailable() { return true; }
    public boolean isFailover() { return false; }
    public boolean isRoaming() { return false; }
    public State getState() { return State.CONNECTED; }
    public DetailedState getDetailedState() { return DetailedState.CONNECTED; }
    public String getExtraInfo() { return null; }
    public String getReason() { return null; }
}
