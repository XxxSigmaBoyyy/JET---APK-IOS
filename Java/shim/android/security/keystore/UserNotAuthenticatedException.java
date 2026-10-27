package android.security.keystore;
public class UserNotAuthenticatedException extends java.security.InvalidKeyException {
    public UserNotAuthenticatedException() { super("User not authenticated"); }
    public UserNotAuthenticatedException(String m) { super(m); }
    public UserNotAuthenticatedException(String m, Throwable c) { super(m, c); }
}
