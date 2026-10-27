package android.content;
import java.util.ArrayList;
import java.util.List;
/** AOSP UriMatcher algorithm (authority node, then path segments; "#" digits, "*" any text). */
public class UriMatcher {
    public static final int NO_MATCH = -1;
    private static final int EXACT = 0, NUMBER = 1, TEXT = 2;
    private int mCode; private final int mWhich; private final String mText; private final ArrayList<UriMatcher> mChildren;
    public UriMatcher(int code) { mCode = code; mWhich = -1; mChildren = new ArrayList<>(); mText = null; }
    private UriMatcher(int which, String text) { mCode = NO_MATCH; mWhich = which; mChildren = new ArrayList<>(); mText = text; }
    public void addURI(String authority, String path, int code) {
        if (code < 0) throw new IllegalArgumentException("code " + code + " is invalid: it must be positive");
        String[] tokens = null;
        if (path != null) { String newPath = path; if (path.length() > 1 && path.charAt(0) == '/') newPath = path.substring(1); tokens = newPath.split("/"); }
        int numTokens = tokens != null ? tokens.length : 0;
        UriMatcher node = this;
        for (int i = -1; i < numTokens; i++) {
            String token = i < 0 ? authority : tokens[i];
            ArrayList<UriMatcher> children = node.mChildren; int numChildren = children.size(); UriMatcher child; int j;
            for (j = 0; j < numChildren; j++) { child = children.get(j); if (token.equals(child.mText)) { node = child; break; } }
            if (j == numChildren) { child = createChild(token); node.mChildren.add(child); node = child; }
        }
        node.mCode = code;
    }
    private static UriMatcher createChild(String token) { switch (token) { case "#": return new UriMatcher(NUMBER, "#"); case "*": return new UriMatcher(TEXT, "*"); default: return new UriMatcher(EXACT, token); } }
    public int match(android.net.Uri uri) {
        final List<String> pathSegments = uri.getPathSegments(); final int li = pathSegments.size();
        UriMatcher node = this;
        if (li == 0 && uri.getAuthority() == null) return this.mCode;
        for (int i = -1; i < li; i++) {
            String u = i < 0 ? uri.getAuthority() : pathSegments.get(i);
            ArrayList<UriMatcher> list = node.mChildren; if (list == null) break;
            node = null;
            for (UriMatcher n : list) {
                which_switch:
                switch (n.mWhich) {
                    case EXACT: if (n.mText.equals(u)) node = n; break;
                    case NUMBER: { int lk = u.length(); for (int k = 0; k < lk; k++) { char c = u.charAt(k); if (c < '0' || c > '9') break which_switch; } node = n; break; }
                    case TEXT: node = n; break;
                }
                if (node != null) break;
            }
            if (node == null) return NO_MATCH;
        }
        return node.mCode;
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
