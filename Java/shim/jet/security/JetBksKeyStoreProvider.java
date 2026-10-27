package jet.security;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.io.*;
import java.util.Date;
import java.util.Enumeration;
/** Android apps (Signal) request KeyStore.getInstance("BKS") — BouncyCastle's format. When the real BouncyCastle provider
 *  is present (tools/bcprov, as Android ships a BC-derived provider) its BKS implementation is used and this alias is not
 *  installed. Without BC, BKS is mapped to PKCS12 and an unreadable BKS stream fails loudly (no silent empty store). */
public final class JetBksKeyStoreProvider extends Provider {
    public static final String NAME = "JET-BKS";
    private static boolean installed;
    public JetBksKeyStoreProvider() {
        super(NAME, "1.0", "JET BKS→PKCS12 alias (Linux host)");
        put("KeyStore.BKS", Spi.class.getName());
    }
    public static synchronized void install() {
        if (!installed && Security.getProvider(NAME) == null && Security.getProvider("BC") == null) Security.insertProviderAt(new JetBksKeyStoreProvider(), 1);
        installed = true;
    }
    public static final class Spi extends KeyStoreSpi {
        private final KeyStore inner;
        public Spi() {
            try { inner = KeyStore.getInstance("PKCS12"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override public Key engineGetKey(String a, char[] p) throws NoSuchAlgorithmException, UnrecoverableKeyException { try { return inner.getKey(a, p); } catch (KeyStoreException e) { throw new NoSuchAlgorithmException(e); } }
        @Override public Certificate[] engineGetCertificateChain(String a) { try { return inner.getCertificateChain(a); } catch (KeyStoreException e) { return null; } }
        @Override public Certificate engineGetCertificate(String a) { try { return inner.getCertificate(a); } catch (KeyStoreException e) { return null; } }
        @Override public Date engineGetCreationDate(String a) { try { return inner.getCreationDate(a); } catch (KeyStoreException e) { return null; } }
        @Override public void engineSetKeyEntry(String a, Key k, char[] p, Certificate[] c) throws KeyStoreException { inner.setKeyEntry(a, k, p, c); }
        @Override public void engineSetKeyEntry(String a, byte[] k, Certificate[] c) throws KeyStoreException { inner.setKeyEntry(a, k, c); }
        @Override public void engineSetCertificateEntry(String a, Certificate c) throws KeyStoreException { inner.setCertificateEntry(a, c); }
        @Override public void engineDeleteEntry(String a) throws KeyStoreException { inner.deleteEntry(a); }
        @Override public Enumeration<String> engineAliases() { try { return inner.aliases(); } catch (KeyStoreException e) { return java.util.Collections.emptyEnumeration(); } }
        @Override public boolean engineContainsAlias(String a) { try { return inner.containsAlias(a); } catch (KeyStoreException e) { return false; } }
        @Override public int engineSize() { try { return inner.size(); } catch (KeyStoreException e) { return 0; } }
        @Override public boolean engineIsKeyEntry(String a) { try { return inner.isKeyEntry(a); } catch (KeyStoreException e) { return false; } }
        @Override public boolean engineIsCertificateEntry(String a) { try { return inner.isCertificateEntry(a); } catch (KeyStoreException e) { return false; } }
        @Override public String engineGetCertificateAlias(Certificate c) { try { return inner.getCertificateAlias(c); } catch (KeyStoreException e) { return null; } }
        @Override public void engineStore(OutputStream s, char[] p) throws IOException, NoSuchAlgorithmException, CertificateException { try { inner.store(s, p); } catch (KeyStoreException e) { throw new IOException(e); } }
        @Override public void engineLoad(InputStream s, char[] p) throws IOException, NoSuchAlgorithmException, CertificateException {
            if (s == null) inner.load(null, p);
            else inner.load(s, p); // BKS bytes are not PKCS12: report the failure like a missing provider would
        }
    }
}
