package android.view;

/** KeyCharacterMap for the host's virtual keyboard (VIRTUAL_KEYBOARD, FULL type; ASCII for letter/digit keycodes). */
public class KeyCharacterMap implements android.os.Parcelable {
    public static final int VIRTUAL_KEYBOARD = -1, BUILT_IN_KEYBOARD = 0;
    public static final int NUMERIC = 1, PREDICTIVE = 2, ALPHA = 3, FULL = 4, SPECIAL_FUNCTION = 5;
    public static final char HEX_INPUT = '\uEF00', PICKER_DIALOG_INPUT = '\uEF01';
    public static final int MODIFIER_BEHAVIOR_CHORDED = 0, MODIFIER_BEHAVIOR_CHORDED_OR_TOGGLED = 1;
    public static final int COMBINING_ACCENT = 0x80000000, COMBINING_ACCENT_MASK = 0x7FFFFFFF;
    private static final KeyCharacterMap VIRTUAL = new KeyCharacterMap();
    private KeyCharacterMap() {}
    public static KeyCharacterMap load(int deviceId) { return VIRTUAL; }
    public int getKeyboardType() { return FULL; }
    public int getModifierBehavior() { return MODIFIER_BEHAVIOR_CHORDED; }
    public static boolean deviceHasKey(int keyCode) { return true; }
    public static boolean[] deviceHasKeys(int[] keyCodes) { boolean[] r = new boolean[keyCodes.length]; java.util.Arrays.fill(r, true); return r; }
    public int get(int keyCode, int metaState) {
        boolean shift = (metaState & 1) != 0;
        if (keyCode >= 29 && keyCode <= 54) return (shift ? 'A' : 'a') + (keyCode - 29);
        if (keyCode >= 7 && keyCode <= 16) return '0' + (keyCode - 7);
        switch (keyCode) { case 62: return ' '; case 66: return '\n'; case 55: return ','; case 56: return '.'; case 76: return '/'; case 69: return '-'; case 70: return '='; default: return 0; }
    }
    public char getDisplayLabel(int keyCode) { int c = get(keyCode, 1); return (char) c; }
    public char getNumber(int keyCode) { int c = get(keyCode, 0); return c >= '0' && c <= '9' ? (char) c : 0; }
    public char getMatch(int keyCode, char[] chars) { return getMatch(keyCode, chars, 0); }
    public char getMatch(int keyCode, char[] chars, int meta) { int c = get(keyCode, meta); for (char x : chars) if (x == c) return x; return 0; }
    public boolean isPrintingKey(int keyCode) { return get(keyCode, 0) != 0; }
    public KeyEvent[] getEvents(char[] chars) { return null; }
    public static int getDeadChar(int accent, int c) { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
