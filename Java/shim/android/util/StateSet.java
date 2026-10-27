package android.util;
public class StateSet {
    public static final int[] WILD_CARD = new int[0], NOTHING = new int[]{0};
    public static boolean isWildCard(int[] s) { return s.length == 0 || s[0] == 0; }
    public static boolean stateSetMatches(int[] spec, int[] state) { for (int a : spec) { boolean want = a > 0, found = false; int v = Math.abs(a); for (int b : state) if (b == v) found = true; if (want != found) return false; } return true; }
    public static int[] trimStateSet(int[] s, int n) { return java.util.Arrays.copyOf(s, n); }
}
