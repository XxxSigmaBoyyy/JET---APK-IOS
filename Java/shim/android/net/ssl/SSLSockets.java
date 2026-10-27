package android.net.ssl;
/** Platform (Conscrypt-backed) TLS sockets do not exist on this host; JDK sockets are reported as unsupported, as AOSP does for foreign sockets. */
public class SSLSockets {
    private SSLSockets() {}
    public static boolean isSupportedSocket(javax.net.ssl.SSLSocket s) { return false; }
    public static void setUseSessionTickets(javax.net.ssl.SSLSocket s, boolean b) { throw new IllegalArgumentException("not a platform SSLSocket"); }
    public static byte[] exportKeyingMaterial(javax.net.ssl.SSLSocket s, String label, byte[] ctx, int len) throws javax.net.ssl.SSLException { throw new IllegalArgumentException("not a platform SSLSocket"); }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
