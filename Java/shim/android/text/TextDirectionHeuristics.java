package android.text;
/** JET: android.text.TextDirectionHeuristics. Direction from the first strong character (Character.getDirectionality), as AOSP FirstStrong. */
public final class TextDirectionHeuristics {
    private TextDirectionHeuristics() {}
    private static final class H implements TextDirectionHeuristic {
        final int mode; final boolean defaultRtl; H(int mode, boolean defaultRtl) { this.mode = mode; this.defaultRtl = defaultRtl; }
        public boolean isRtl(char[] a, int s, int n) { return isRtl(java.nio.CharBuffer.wrap(a), s, n); }
        public boolean isRtl(CharSequence cs, int s, int n) {
            if (mode == 0) return defaultRtl;              // fixed LTR/RTL
            if (cs == null || n == 0) return defaultRtl;
            boolean sawRtl = false, sawLtr = false;
            for (int i = s; i < s + n && i < cs.length(); i++) {
                byte d = Character.getDirectionality(cs.charAt(i));
                boolean rtl = d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
                boolean ltr = d == Character.DIRECTIONALITY_LEFT_TO_RIGHT;
                if (mode == 1) { if (rtl) return true; if (ltr) return false; }   // first strong
                else { sawRtl |= rtl; sawLtr |= ltr; }                             // any RTL
            }
            return mode == 1 ? defaultRtl : (sawRtl || (!sawLtr && defaultRtl));
        }
    }
    public static final TextDirectionHeuristic LTR = new H(0, false), RTL = new H(0, true),
        FIRSTSTRONG_LTR = new H(1, false), FIRSTSTRONG_RTL = new H(1, true), ANYRTL_LTR = new H(2, false), LOCALE = new H(0, false);
}
