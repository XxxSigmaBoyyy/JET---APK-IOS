package android.system;
public final class ErrnoException extends Exception {
    public final String functionName; public final int errno;
    public ErrnoException(String functionName, int errno) { super(functionName + " failed: errno " + errno); this.functionName = functionName; this.errno = errno; }
    public ErrnoException(String functionName, int errno, Throwable cause) { this(functionName, errno); initCause(cause); }
    public java.io.IOException rethrowAsIOException() throws java.io.IOException { throw new java.io.IOException(getMessage(), this); }
    public java.net.SocketException rethrowAsSocketException() throws java.net.SocketException { java.net.SocketException e = new java.net.SocketException(getMessage()); e.initCause(this); throw e; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
