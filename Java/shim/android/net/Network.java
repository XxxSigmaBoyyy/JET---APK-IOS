package android.net;
/** JET: the single host network (Linux-only harness: the host's connection; iOS: NWPathMonitor). */
public class Network implements android.os.Parcelable {
    final int netId; Network(int id) { netId = id; }
    public static final Network JET_DEFAULT = new Network(100);
    public long getNetworkHandle() { return ((long) netId << 32) | 0xfacade; }
    public java.net.InetAddress[] getAllByName(String host) throws java.net.UnknownHostException { return java.net.InetAddress.getAllByName(host); }
    public java.net.InetAddress getByName(String host) throws java.net.UnknownHostException { return java.net.InetAddress.getByName(host); }
    public javax.net.SocketFactory getSocketFactory() { return javax.net.SocketFactory.getDefault(); }
    public java.net.URLConnection openConnection(java.net.URL url) throws java.io.IOException { return url.openConnection(); }
    public void bindSocket(java.net.Socket s) {}
    @Override public boolean equals(Object o) { return o instanceof Network && ((Network) o).netId == netId; }
    @Override public int hashCode() { return netId * 11; }
    @Override public String toString() { return Integer.toString(netId); }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
