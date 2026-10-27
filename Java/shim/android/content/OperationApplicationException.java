package android.content;
public class OperationApplicationException extends Exception {
    private final int mNumSuccessfulYieldPoints;
    public OperationApplicationException() { this((String) null); }
    public OperationApplicationException(String m) { super(m); mNumSuccessfulYieldPoints = 0; }
    public OperationApplicationException(String m, Throwable c) { super(m, c); mNumSuccessfulYieldPoints = 0; }
    public OperationApplicationException(Throwable c) { super(c); mNumSuccessfulYieldPoints = 0; }
    public OperationApplicationException(int n) { super(); mNumSuccessfulYieldPoints = n; }
    public OperationApplicationException(String m, int n) { super(m); mNumSuccessfulYieldPoints = n; }
    public int getNumSuccessfulYieldPoints() { return mNumSuccessfulYieldPoints; }
}
