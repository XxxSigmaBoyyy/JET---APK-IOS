package android.content.pm;
public class Signature {
    private final byte[] mSig;
    public Signature(byte[] s) { mSig = s.clone(); }
    public Signature(String hex) { int n = hex.length() / 2; mSig = new byte[n]; for (int i = 0; i < n; i++) mSig[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16); }
    public byte[] toByteArray() { return mSig.clone(); }
    public char[] toChars() { char[] c = new char[mSig.length * 2]; String h = "0123456789abcdef"; for (int i = 0; i < mSig.length; i++) { c[2 * i] = h.charAt((mSig[i] >> 4) & 15); c[2 * i + 1] = h.charAt(mSig[i] & 15); } return c; }
    public String toCharsString() { return new String(toChars()); }
    @Override public boolean equals(Object o) { return o instanceof Signature && java.util.Arrays.equals(mSig, ((Signature) o).mSig); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(mSig); }
}
