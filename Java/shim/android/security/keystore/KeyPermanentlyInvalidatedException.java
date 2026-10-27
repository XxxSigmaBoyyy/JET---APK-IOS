package android.security.keystore;
public class KeyPermanentlyInvalidatedException extends java.security.InvalidKeyException {
    public KeyPermanentlyInvalidatedException() { super("Key permanently invalidated"); }
    public KeyPermanentlyInvalidatedException(String m) { super(m); }
    public KeyPermanentlyInvalidatedException(String m, Throwable c) { super(m, c); }
}
