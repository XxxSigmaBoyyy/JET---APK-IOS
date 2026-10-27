package android.net;
public class NetworkRequest {
    final NetworkCapabilities nc;
    NetworkRequest(NetworkCapabilities nc) { this.nc = nc; }
    public boolean hasCapability(int c) { return nc.hasCapability(c); }
    public boolean hasTransport(int t) { return nc.hasTransport(t); }
    public static class Builder {
        private final NetworkCapabilities nc = new NetworkCapabilities();
        public Builder() { nc.caps |= (1L << NetworkCapabilities.NET_CAPABILITY_INTERNET) | (1L << NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) | (1L << NetworkCapabilities.NET_CAPABILITY_TRUSTED) | (1L << NetworkCapabilities.NET_CAPABILITY_NOT_VPN); }
        public Builder addCapability(int c) { nc.caps |= 1L << c; return this; }
        public Builder removeCapability(int c) { nc.caps &= ~(1L << c); return this; }
        public Builder addTransportType(int t) { nc.transports |= 1L << t; return this; }
        public Builder removeTransportType(int t) { nc.transports &= ~(1L << t); return this; }
        public Builder clearCapabilities() { nc.caps = 0; return this; }
        public NetworkRequest build() { return new NetworkRequest(new NetworkCapabilities(nc)); }
    }
}
