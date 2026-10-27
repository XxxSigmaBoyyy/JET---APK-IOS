package android.util;
public class AndroidRuntimeException extends RuntimeException { public AndroidRuntimeException() {} public AndroidRuntimeException(String n) { super(n); } public AndroidRuntimeException(String n, Throwable c) { super(n, c); } public AndroidRuntimeException(Exception c) { super(c); } }
