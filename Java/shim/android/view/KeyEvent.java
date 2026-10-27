package android.view;
/** JET KeyEvent (AOSP shape; constants generated from android.jar). */
public class KeyEvent extends InputEvent implements android.os.Parcelable {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_MULTIPLE = 2;
    public static final int ACTION_UP = 1;
    public static final int FLAG_CANCELED = 32;
    public static final int FLAG_CANCELED_LONG_PRESS = 256;
    public static final int FLAG_EDITOR_ACTION = 16;
    public static final int FLAG_FALLBACK = 1024;
    public static final int FLAG_FROM_SYSTEM = 8;
    public static final int FLAG_KEEP_TOUCH_MODE = 4;
    public static final int FLAG_LONG_PRESS = 128;
    public static final int FLAG_SOFT_KEYBOARD = 2;
    public static final int FLAG_TRACKING = 512;
    public static final int FLAG_VIRTUAL_HARD_KEY = 64;
    public static final int FLAG_WOKE_HERE = 1;
    public static final int KEYCODE_0 = 7;
    public static final int KEYCODE_1 = 8;
    public static final int KEYCODE_11 = 227;
    public static final int KEYCODE_12 = 228;
    public static final int KEYCODE_2 = 9;
    public static final int KEYCODE_3 = 10;
    public static final int KEYCODE_3D_MODE = 206;
    public static final int KEYCODE_4 = 11;
    public static final int KEYCODE_5 = 12;
    public static final int KEYCODE_6 = 13;
    public static final int KEYCODE_7 = 14;
    public static final int KEYCODE_8 = 15;
    public static final int KEYCODE_9 = 16;
    public static final int KEYCODE_A = 29;
    public static final int KEYCODE_ALL_APPS = 284;
    public static final int KEYCODE_ALT_LEFT = 57;
    public static final int KEYCODE_ALT_RIGHT = 58;
    public static final int KEYCODE_APOSTROPHE = 75;
    public static final int KEYCODE_APP_SWITCH = 187;
    public static final int KEYCODE_ASSIST = 219;
    public static final int KEYCODE_AT = 77;
    public static final int KEYCODE_AVR_INPUT = 182;
    public static final int KEYCODE_AVR_POWER = 181;
    public static final int KEYCODE_B = 30;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_BACKSLASH = 73;
    public static final int KEYCODE_BOOKMARK = 174;
    public static final int KEYCODE_BREAK = 121;
    public static final int KEYCODE_BRIGHTNESS_DOWN = 220;
    public static final int KEYCODE_BRIGHTNESS_UP = 221;
    public static final int KEYCODE_BUTTON_1 = 188;
    public static final int KEYCODE_BUTTON_10 = 197;
    public static final int KEYCODE_BUTTON_11 = 198;
    public static final int KEYCODE_BUTTON_12 = 199;
    public static final int KEYCODE_BUTTON_13 = 200;
    public static final int KEYCODE_BUTTON_14 = 201;
    public static final int KEYCODE_BUTTON_15 = 202;
    public static final int KEYCODE_BUTTON_16 = 203;
    public static final int KEYCODE_BUTTON_2 = 189;
    public static final int KEYCODE_BUTTON_3 = 190;
    public static final int KEYCODE_BUTTON_4 = 191;
    public static final int KEYCODE_BUTTON_5 = 192;
    public static final int KEYCODE_BUTTON_6 = 193;
    public static final int KEYCODE_BUTTON_7 = 194;
    public static final int KEYCODE_BUTTON_8 = 195;
    public static final int KEYCODE_BUTTON_9 = 196;
    public static final int KEYCODE_BUTTON_A = 96;
    public static final int KEYCODE_BUTTON_B = 97;
    public static final int KEYCODE_BUTTON_C = 98;
    public static final int KEYCODE_BUTTON_L1 = 102;
    public static final int KEYCODE_BUTTON_L2 = 104;
    public static final int KEYCODE_BUTTON_MODE = 110;
    public static final int KEYCODE_BUTTON_R1 = 103;
    public static final int KEYCODE_BUTTON_R2 = 105;
    public static final int KEYCODE_BUTTON_SELECT = 109;
    public static final int KEYCODE_BUTTON_START = 108;
    public static final int KEYCODE_BUTTON_THUMBL = 106;
    public static final int KEYCODE_BUTTON_THUMBR = 107;
    public static final int KEYCODE_BUTTON_X = 99;
    public static final int KEYCODE_BUTTON_Y = 100;
    public static final int KEYCODE_BUTTON_Z = 101;
    public static final int KEYCODE_C = 31;
    public static final int KEYCODE_CALCULATOR = 210;
    public static final int KEYCODE_CALENDAR = 208;
    public static final int KEYCODE_CALL = 5;
    public static final int KEYCODE_CAMERA = 27;
    public static final int KEYCODE_CAPS_LOCK = 115;
    public static final int KEYCODE_CAPTIONS = 175;
    public static final int KEYCODE_CHANNEL_DOWN = 167;
    public static final int KEYCODE_CHANNEL_UP = 166;
    public static final int KEYCODE_CLEAR = 28;
    public static final int KEYCODE_COMMA = 55;
    public static final int KEYCODE_CONTACTS = 207;
    public static final int KEYCODE_COPY = 278;
    public static final int KEYCODE_CTRL_LEFT = 113;
    public static final int KEYCODE_CTRL_RIGHT = 114;
    public static final int KEYCODE_CUT = 277;
    public static final int KEYCODE_D = 32;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_DEMO_APP_1 = 301;
    public static final int KEYCODE_DEMO_APP_2 = 302;
    public static final int KEYCODE_DEMO_APP_3 = 303;
    public static final int KEYCODE_DEMO_APP_4 = 304;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_DOWN_LEFT = 269;
    public static final int KEYCODE_DPAD_DOWN_RIGHT = 271;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_UP_LEFT = 268;
    public static final int KEYCODE_DPAD_UP_RIGHT = 270;
    public static final int KEYCODE_DVR = 173;
    public static final int KEYCODE_E = 33;
    public static final int KEYCODE_EISU = 212;
    public static final int KEYCODE_ENDCALL = 6;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_ENVELOPE = 65;
    public static final int KEYCODE_EQUALS = 70;
    public static final int KEYCODE_ESCAPE = 111;
    public static final int KEYCODE_EXPLORER = 64;
    public static final int KEYCODE_F = 34;
    public static final int KEYCODE_F1 = 131;
    public static final int KEYCODE_F10 = 140;
    public static final int KEYCODE_F11 = 141;
    public static final int KEYCODE_F12 = 142;
    public static final int KEYCODE_F2 = 132;
    public static final int KEYCODE_F3 = 133;
    public static final int KEYCODE_F4 = 134;
    public static final int KEYCODE_F5 = 135;
    public static final int KEYCODE_F6 = 136;
    public static final int KEYCODE_F7 = 137;
    public static final int KEYCODE_F8 = 138;
    public static final int KEYCODE_F9 = 139;
    public static final int KEYCODE_FEATURED_APP_1 = 297;
    public static final int KEYCODE_FEATURED_APP_2 = 298;
    public static final int KEYCODE_FEATURED_APP_3 = 299;
    public static final int KEYCODE_FEATURED_APP_4 = 300;
    public static final int KEYCODE_FOCUS = 80;
    public static final int KEYCODE_FORWARD = 125;
    public static final int KEYCODE_FORWARD_DEL = 112;
    public static final int KEYCODE_FUNCTION = 119;
    public static final int KEYCODE_G = 35;
    public static final int KEYCODE_GRAVE = 68;
    public static final int KEYCODE_GUIDE = 172;
    public static final int KEYCODE_H = 36;
    public static final int KEYCODE_HEADSETHOOK = 79;
    public static final int KEYCODE_HELP = 259;
    public static final int KEYCODE_HENKAN = 214;
    public static final int KEYCODE_HOME = 3;
    public static final int KEYCODE_I = 37;
    public static final int KEYCODE_INFO = 165;
    public static final int KEYCODE_INSERT = 124;
    public static final int KEYCODE_J = 38;
    public static final int KEYCODE_K = 39;
    public static final int KEYCODE_KANA = 218;
    public static final int KEYCODE_KATAKANA_HIRAGANA = 215;
    public static final int KEYCODE_L = 40;
    public static final int KEYCODE_LANGUAGE_SWITCH = 204;
    public static final int KEYCODE_LAST_CHANNEL = 229;
    public static final int KEYCODE_LEFT_BRACKET = 71;
    public static final int KEYCODE_M = 41;
    public static final int KEYCODE_MANNER_MODE = 205;
    public static final int KEYCODE_MEDIA_AUDIO_TRACK = 222;
    public static final int KEYCODE_MEDIA_CLOSE = 128;
    public static final int KEYCODE_MEDIA_EJECT = 129;
    public static final int KEYCODE_MEDIA_FAST_FORWARD = 90;
    public static final int KEYCODE_MEDIA_NEXT = 87;
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    public static final int KEYCODE_MEDIA_PLAY = 126;
    public static final int KEYCODE_MEDIA_PLAY_PAUSE = 85;
    public static final int KEYCODE_MEDIA_PREVIOUS = 88;
    public static final int KEYCODE_MEDIA_RECORD = 130;
    public static final int KEYCODE_MEDIA_REWIND = 89;
    public static final int KEYCODE_MEDIA_SKIP_BACKWARD = 273;
    public static final int KEYCODE_MEDIA_SKIP_FORWARD = 272;
    public static final int KEYCODE_MEDIA_STEP_BACKWARD = 275;
    public static final int KEYCODE_MEDIA_STEP_FORWARD = 274;
    public static final int KEYCODE_MEDIA_STOP = 86;
    public static final int KEYCODE_MEDIA_TOP_MENU = 226;
    public static final int KEYCODE_MENU = 82;
    public static final int KEYCODE_META_LEFT = 117;
    public static final int KEYCODE_META_RIGHT = 118;
    public static final int KEYCODE_MINUS = 69;
    public static final int KEYCODE_MOVE_END = 123;
    public static final int KEYCODE_MOVE_HOME = 122;
    public static final int KEYCODE_MUHENKAN = 213;
    public static final int KEYCODE_MUSIC = 209;
    public static final int KEYCODE_MUTE = 91;
    public static final int KEYCODE_N = 42;
    public static final int KEYCODE_NAVIGATE_IN = 262;
    public static final int KEYCODE_NAVIGATE_NEXT = 261;
    public static final int KEYCODE_NAVIGATE_OUT = 263;
    public static final int KEYCODE_NAVIGATE_PREVIOUS = 260;
    public static final int KEYCODE_NOTIFICATION = 83;
    public static final int KEYCODE_NUM = 78;
    public static final int KEYCODE_NUMPAD_0 = 144;
    public static final int KEYCODE_NUMPAD_1 = 145;
    public static final int KEYCODE_NUMPAD_2 = 146;
    public static final int KEYCODE_NUMPAD_3 = 147;
    public static final int KEYCODE_NUMPAD_4 = 148;
    public static final int KEYCODE_NUMPAD_5 = 149;
    public static final int KEYCODE_NUMPAD_6 = 150;
    public static final int KEYCODE_NUMPAD_7 = 151;
    public static final int KEYCODE_NUMPAD_8 = 152;
    public static final int KEYCODE_NUMPAD_9 = 153;
    public static final int KEYCODE_NUMPAD_ADD = 157;
    public static final int KEYCODE_NUMPAD_COMMA = 159;
    public static final int KEYCODE_NUMPAD_DIVIDE = 154;
    public static final int KEYCODE_NUMPAD_DOT = 158;
    public static final int KEYCODE_NUMPAD_ENTER = 160;
    public static final int KEYCODE_NUMPAD_EQUALS = 161;
    public static final int KEYCODE_NUMPAD_LEFT_PAREN = 162;
    public static final int KEYCODE_NUMPAD_MULTIPLY = 155;
    public static final int KEYCODE_NUMPAD_RIGHT_PAREN = 163;
    public static final int KEYCODE_NUMPAD_SUBTRACT = 156;
    public static final int KEYCODE_NUM_LOCK = 143;
    public static final int KEYCODE_O = 43;
    public static final int KEYCODE_P = 44;
    public static final int KEYCODE_PAGE_DOWN = 93;
    public static final int KEYCODE_PAGE_UP = 92;
    public static final int KEYCODE_PAIRING = 225;
    public static final int KEYCODE_PASTE = 279;
    public static final int KEYCODE_PERIOD = 56;
    public static final int KEYCODE_PICTSYMBOLS = 94;
    public static final int KEYCODE_PLUS = 81;
    public static final int KEYCODE_POUND = 18;
    public static final int KEYCODE_POWER = 26;
    public static final int KEYCODE_PROFILE_SWITCH = 288;
    public static final int KEYCODE_PROG_BLUE = 186;
    public static final int KEYCODE_PROG_GREEN = 184;
    public static final int KEYCODE_PROG_RED = 183;
    public static final int KEYCODE_PROG_YELLOW = 185;
    public static final int KEYCODE_Q = 45;
    public static final int KEYCODE_R = 46;
    public static final int KEYCODE_REFRESH = 285;
    public static final int KEYCODE_RIGHT_BRACKET = 72;
    public static final int KEYCODE_RO = 217;
    public static final int KEYCODE_S = 47;
    public static final int KEYCODE_SCROLL_LOCK = 116;
    public static final int KEYCODE_SEARCH = 84;
    public static final int KEYCODE_SEMICOLON = 74;
    public static final int KEYCODE_SETTINGS = 176;
    public static final int KEYCODE_SHIFT_LEFT = 59;
    public static final int KEYCODE_SHIFT_RIGHT = 60;
    public static final int KEYCODE_SLASH = 76;
    public static final int KEYCODE_SLEEP = 223;
    public static final int KEYCODE_SOFT_LEFT = 1;
    public static final int KEYCODE_SOFT_RIGHT = 2;
    public static final int KEYCODE_SOFT_SLEEP = 276;
    public static final int KEYCODE_SPACE = 62;
    public static final int KEYCODE_STAR = 17;
    public static final int KEYCODE_STB_INPUT = 180;
    public static final int KEYCODE_STB_POWER = 179;
    public static final int KEYCODE_STEM_1 = 265;
    public static final int KEYCODE_STEM_2 = 266;
    public static final int KEYCODE_STEM_3 = 267;
    public static final int KEYCODE_STEM_PRIMARY = 264;
    public static final int KEYCODE_SWITCH_CHARSET = 95;
    public static final int KEYCODE_SYM = 63;
    public static final int KEYCODE_SYSRQ = 120;
    public static final int KEYCODE_SYSTEM_NAVIGATION_DOWN = 281;
    public static final int KEYCODE_SYSTEM_NAVIGATION_LEFT = 282;
    public static final int KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283;
    public static final int KEYCODE_SYSTEM_NAVIGATION_UP = 280;
    public static final int KEYCODE_T = 48;
    public static final int KEYCODE_TAB = 61;
    public static final int KEYCODE_THUMBS_DOWN = 287;
    public static final int KEYCODE_THUMBS_UP = 286;
    public static final int KEYCODE_TV = 170;
    public static final int KEYCODE_TV_ANTENNA_CABLE = 242;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION = 252;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253;
    public static final int KEYCODE_TV_CONTENTS_MENU = 256;
    public static final int KEYCODE_TV_DATA_SERVICE = 230;
    public static final int KEYCODE_TV_INPUT = 178;
    public static final int KEYCODE_TV_INPUT_COMPONENT_1 = 249;
    public static final int KEYCODE_TV_INPUT_COMPONENT_2 = 250;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_1 = 247;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_2 = 248;
    public static final int KEYCODE_TV_INPUT_HDMI_1 = 243;
    public static final int KEYCODE_TV_INPUT_HDMI_2 = 244;
    public static final int KEYCODE_TV_INPUT_HDMI_3 = 245;
    public static final int KEYCODE_TV_INPUT_HDMI_4 = 246;
    public static final int KEYCODE_TV_INPUT_VGA_1 = 251;
    public static final int KEYCODE_TV_MEDIA_CONTEXT_MENU = 257;
    public static final int KEYCODE_TV_NETWORK = 241;
    public static final int KEYCODE_TV_NUMBER_ENTRY = 234;
    public static final int KEYCODE_TV_POWER = 177;
    public static final int KEYCODE_TV_RADIO_SERVICE = 232;
    public static final int KEYCODE_TV_SATELLITE = 237;
    public static final int KEYCODE_TV_SATELLITE_BS = 238;
    public static final int KEYCODE_TV_SATELLITE_CS = 239;
    public static final int KEYCODE_TV_SATELLITE_SERVICE = 240;
    public static final int KEYCODE_TV_TELETEXT = 233;
    public static final int KEYCODE_TV_TERRESTRIAL_ANALOG = 235;
    public static final int KEYCODE_TV_TERRESTRIAL_DIGITAL = 236;
    public static final int KEYCODE_TV_TIMER_PROGRAMMING = 258;
    public static final int KEYCODE_TV_ZOOM_MODE = 255;
    public static final int KEYCODE_U = 49;
    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_V = 50;
    public static final int KEYCODE_VIDEO_APP_1 = 289;
    public static final int KEYCODE_VIDEO_APP_2 = 290;
    public static final int KEYCODE_VIDEO_APP_3 = 291;
    public static final int KEYCODE_VIDEO_APP_4 = 292;
    public static final int KEYCODE_VIDEO_APP_5 = 293;
    public static final int KEYCODE_VIDEO_APP_6 = 294;
    public static final int KEYCODE_VIDEO_APP_7 = 295;
    public static final int KEYCODE_VIDEO_APP_8 = 296;
    public static final int KEYCODE_VOICE_ASSIST = 231;
    public static final int KEYCODE_VOLUME_DOWN = 25;
    public static final int KEYCODE_VOLUME_MUTE = 164;
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_W = 51;
    public static final int KEYCODE_WAKEUP = 224;
    public static final int KEYCODE_WINDOW = 171;
    public static final int KEYCODE_X = 52;
    public static final int KEYCODE_Y = 53;
    public static final int KEYCODE_YEN = 216;
    public static final int KEYCODE_Z = 54;
    public static final int KEYCODE_ZENKAKU_HANKAKU = 211;
    public static final int KEYCODE_ZOOM_IN = 168;
    public static final int KEYCODE_ZOOM_OUT = 169;
    public static final int MAX_KEYCODE = 84;
    public static final int META_ALT_LEFT_ON = 16;
    public static final int META_ALT_MASK = 50;
    public static final int META_ALT_ON = 2;
    public static final int META_ALT_RIGHT_ON = 32;
    public static final int META_CAPS_LOCK_ON = 1048576;
    public static final int META_CTRL_LEFT_ON = 8192;
    public static final int META_CTRL_MASK = 28672;
    public static final int META_CTRL_ON = 4096;
    public static final int META_CTRL_RIGHT_ON = 16384;
    public static final int META_FUNCTION_ON = 8;
    public static final int META_META_LEFT_ON = 131072;
    public static final int META_META_MASK = 458752;
    public static final int META_META_ON = 65536;
    public static final int META_META_RIGHT_ON = 262144;
    public static final int META_NUM_LOCK_ON = 2097152;
    public static final int META_SCROLL_LOCK_ON = 4194304;
    public static final int META_SHIFT_LEFT_ON = 64;
    public static final int META_SHIFT_MASK = 193;
    public static final int META_SHIFT_ON = 1;
    public static final int META_SHIFT_RIGHT_ON = 128;
    public static final int META_SYM_ON = 4;
    public interface Callback {
        boolean onKeyDown(int keyCode, KeyEvent event);
        boolean onKeyLongPress(int keyCode, KeyEvent event);
        boolean onKeyUp(int keyCode, KeyEvent event);
        boolean onKeyMultiple(int keyCode, int count, KeyEvent event);
    }
    private final int action, code, repeat, meta, deviceId, scanCode, flags; private int source = InputDevice.SOURCE_KEYBOARD;
    private final long downTime, eventTime; private final String characters;
    public KeyEvent(int action, int code) { this(0, 0, action, code, 0, 0, KeyCharacterMapVIRTUAL, 0, 0); }
    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat) { this(downTime, eventTime, action, code, repeat, 0, KeyCharacterMapVIRTUAL, 0, 0); }
    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState) { this(downTime, eventTime, action, code, repeat, metaState, KeyCharacterMapVIRTUAL, 0, 0); }
    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scancode) { this(downTime, eventTime, action, code, repeat, metaState, deviceId, scancode, 0); }
    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scancode, int flags) {
        this.downTime = downTime; this.eventTime = eventTime; this.action = action; this.code = code; this.repeat = repeat; this.meta = metaState; this.deviceId = deviceId; this.scanCode = scancode; this.flags = flags; this.characters = null;
    }
    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scancode, int flags, int source) { this(downTime, eventTime, action, code, repeat, metaState, deviceId, scancode, flags); this.source = source; }
    public KeyEvent(long time, String characters, int deviceId, int flags) { this.downTime = time; this.eventTime = time; this.action = ACTION_MULTIPLE; this.code = KEYCODE_UNKNOWN; this.repeat = 0; this.meta = 0; this.deviceId = deviceId; this.scanCode = 0; this.flags = flags; this.characters = characters; }
    public KeyEvent(KeyEvent o) { this(o.downTime, o.eventTime, o.action, o.code, o.repeat, o.meta, o.deviceId, o.scanCode, o.flags, o.source); }
    public KeyEvent(KeyEvent o, long eventTime, int newRepeat) { this(o.downTime, eventTime, o.action, o.code, newRepeat, o.meta, o.deviceId, o.scanCode, o.flags, o.source); }
    private static final int KeyCharacterMapVIRTUAL = -1;
    public static KeyEvent changeAction(KeyEvent e, int action) { return new KeyEvent(e.downTime, e.eventTime, action, e.code, e.repeat, e.meta, e.deviceId, e.scanCode, e.flags, e.source); }
    public static KeyEvent changeFlags(KeyEvent e, int flags) { return new KeyEvent(e.downTime, e.eventTime, e.action, e.code, e.repeat, e.meta, e.deviceId, e.scanCode, flags, e.source); }
    public static KeyEvent changeTimeRepeat(KeyEvent e, long eventTime, int newRepeat) { return new KeyEvent(e, eventTime, newRepeat); }
    public final int getAction() { return action; }
    public final int getKeyCode() { return code; }
    public final int getRepeatCount() { return repeat; }
    public final int getMetaState() { return meta; }
    public final int getModifiers() { return meta & (META_SHIFT_MASK | META_ALT_MASK | META_CTRL_MASK | META_META_MASK | META_SYM_ON | META_FUNCTION_ON); }
    public final int getFlags() { return flags; }
    public final int getScanCode() { return scanCode; }
    public final long getDownTime() { return downTime; }
    @Override public final long getEventTime() { return eventTime; }
    @Override public final int getDeviceId() { return deviceId; }
    @Override public final int getSource() { return source; }
    public final void setSource(int s) { source = s; }
    public final String getCharacters() { return characters; }
    public final boolean isCanceled() { return (flags & FLAG_CANCELED) != 0; }
    public final boolean isLongPress() { return (flags & FLAG_LONG_PRESS) != 0; }
    public final boolean isTracking() { return (flags & FLAG_TRACKING) != 0; }
    public final void startTracking() {}
    public final boolean isShiftPressed() { return (meta & META_SHIFT_ON) != 0; }
    public final boolean isAltPressed() { return (meta & META_ALT_ON) != 0; }
    public final boolean isCtrlPressed() { return (meta & META_CTRL_ON) != 0; }
    public final boolean isMetaPressed() { return (meta & META_META_ON) != 0; }
    public final boolean isSymPressed() { return (meta & META_SYM_ON) != 0; }
    public final boolean isFunctionPressed() { return (meta & META_FUNCTION_ON) != 0; }
    public final boolean isCapsLockOn() { return (meta & META_CAPS_LOCK_ON) != 0; }
    public final boolean isNumLockOn() { return (meta & META_NUM_LOCK_ON) != 0; }
    public final boolean isScrollLockOn() { return (meta & META_SCROLL_LOCK_ON) != 0; }
    public final boolean hasNoModifiers() { return getModifiers() == 0; }
    public final boolean hasModifiers(int m) { return getModifiers() == m; }
    public final boolean isSystem() { return isSystemKey(code); }
    public static boolean isModifierKey(int k) { switch (k) { case KEYCODE_SHIFT_LEFT: case KEYCODE_SHIFT_RIGHT: case KEYCODE_ALT_LEFT: case KEYCODE_ALT_RIGHT: case KEYCODE_CTRL_LEFT: case KEYCODE_CTRL_RIGHT: case KEYCODE_META_LEFT: case KEYCODE_META_RIGHT: case KEYCODE_SYM: case KEYCODE_NUM: case KEYCODE_FUNCTION: return true; default: return false; } }
    private static boolean isSystemKey(int k) { switch (k) { case KEYCODE_MENU: case KEYCODE_SOFT_RIGHT: case KEYCODE_HOME: case KEYCODE_BACK: case KEYCODE_CALL: case KEYCODE_ENDCALL: case KEYCODE_VOLUME_UP: case KEYCODE_VOLUME_DOWN: case KEYCODE_VOLUME_MUTE: case KEYCODE_MUTE: case KEYCODE_POWER: case KEYCODE_HEADSETHOOK: case KEYCODE_CAMERA: case KEYCODE_FOCUS: case KEYCODE_SEARCH: return true; default: return false; } }
    public static boolean isGamepadButton(int k) { return (k >= KEYCODE_BUTTON_A && k <= KEYCODE_BUTTON_MODE) || (k >= KEYCODE_BUTTON_1 && k <= KEYCODE_BUTTON_16); }
    public static boolean metaStateHasNoModifiers(int m) { return (m & (META_SHIFT_MASK | META_ALT_MASK | META_CTRL_MASK | META_META_MASK | META_SYM_ON | META_FUNCTION_ON)) == 0; }
    public static boolean metaStateHasModifiers(int m, int mods) { return (m & (META_SHIFT_MASK | META_ALT_MASK | META_CTRL_MASK | META_META_MASK | META_SYM_ON | META_FUNCTION_ON)) == mods; }
    public static int normalizeMetaState(int m) { return m; }
    public static int getMaxKeyCode() { return LAST_KEYCODE_JET; }
    private static final int LAST_KEYCODE_JET = 304;
    public int getUnicodeChar() { return getUnicodeChar(meta); }
    public int getUnicodeChar(int metaState) {
        boolean sh = (metaState & META_SHIFT_ON) != 0;
        if (code >= KEYCODE_A && code <= KEYCODE_Z) return (sh ? 'A' : 'a') + (code - KEYCODE_A);
        if (code >= KEYCODE_0 && code <= KEYCODE_9) return sh ? ")!@#$%^&*(".charAt(code - KEYCODE_0) : '0' + (code - KEYCODE_0);
        switch (code) { case KEYCODE_SPACE: return ' '; case KEYCODE_ENTER: return '\n'; case KEYCODE_TAB: return '\t'; case KEYCODE_COMMA: return sh ? '<' : ','; case KEYCODE_PERIOD: return sh ? '>' : '.';
            case KEYCODE_MINUS: return sh ? '_' : '-'; case KEYCODE_EQUALS: return sh ? '+' : '='; case KEYCODE_SLASH: return sh ? '?' : '/'; case KEYCODE_SEMICOLON: return sh ? ':' : ';';
            case KEYCODE_APOSTROPHE: return sh ? '"' : '\''; case KEYCODE_AT: return '@'; case KEYCODE_PLUS: return '+'; case KEYCODE_STAR: return '*'; case KEYCODE_POUND: return '#'; default: return 0; }
    }
    public char getDisplayLabel() { int c = getUnicodeChar(META_SHIFT_ON); return (char) c; }
    public char getNumber() { return code >= KEYCODE_0 && code <= KEYCODE_9 ? (char) ('0' + code - KEYCODE_0) : 0; }
    public boolean isPrintingKey() { return getUnicodeChar() != 0; }
    public static String keyCodeToString(int k) {
        for (java.lang.reflect.Field f : KeyEvent.class.getFields()) try { if (f.getName().startsWith("KEYCODE_") && f.getType() == int.class && f.getInt(null) == k) return f.getName(); } catch (Exception ignored) {}
        return Integer.toString(k);
    }
    public static int keyCodeFromString(String s) { try { return KeyEvent.class.getField(s.startsWith("KEYCODE_") ? s : "KEYCODE_" + s).getInt(null); } catch (Exception e) { try { return Integer.parseInt(s); } catch (NumberFormatException n) { return KEYCODE_UNKNOWN; } } }
    public final boolean dispatch(Callback receiver) { return dispatch(receiver, null, null); }
    public final boolean dispatch(Callback receiver, DispatcherState state, Object target) {
        switch (action) { case ACTION_DOWN: return receiver.onKeyDown(code, this); case ACTION_UP: return receiver.onKeyUp(code, this); default: return receiver.onKeyMultiple(code, repeat, this); }
    }
    public static class DispatcherState { public void reset() {} public void reset(Object t) {} public void startTracking(KeyEvent e, Object t) {} public boolean isTracking(KeyEvent e) { return false; } public void performedLongPress(KeyEvent e) {} public void handleUpEvent(KeyEvent e) {} }
    @Override public String toString() { return "KeyEvent { action=" + action + ", keyCode=" + keyCodeToString(code) + ", repeatCount=" + repeat + ", metaState=" + meta + " }"; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
