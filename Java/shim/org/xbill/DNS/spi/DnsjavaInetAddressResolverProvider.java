package org.xbill.DNS.spi;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.stream.Stream;
/** Stand-in when app META-INF lists dnsjava SPI but the class is missing (Signal + Java2D/AWT fonts). */
public class DnsjavaInetAddressResolverProvider extends InetAddressResolverProvider {
    @Override public InetAddressResolver get(Configuration configuration) {
        final InetAddressResolver builtin = configuration.builtinResolver();
        return builtin != null ? builtin : new InetAddressResolver() {
            @Override public Stream<InetAddress> lookupByName(String host, LookupPolicy policy) throws UnknownHostException {
                if (host == null) throw new UnknownHostException();
                if ("localhost".equalsIgnoreCase(host)) return Stream.of(InetAddress.getLoopbackAddress());
                try { return Stream.of(InetAddress.getByAddress(host, new byte[]{127,0,0,1})); }
                catch (Exception e) { throw new UnknownHostException(host); }
            }
            @Override public String lookupByAddress(byte[] addr) { return "localhost"; }
        };
    }
    @Override public String name() { return "jet-dnsjava-stub"; }
}
