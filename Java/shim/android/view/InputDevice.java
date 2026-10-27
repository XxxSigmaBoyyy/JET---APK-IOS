package android.view;
/** JET: the device list of a phone with a touchscreen and the built-in virtual keyboard (id -1, like Android). */
public final class InputDevice implements android.os.Parcelable {
    public static final int SOURCE_CLASS_MASK = 0xff, SOURCE_CLASS_NONE = 0, SOURCE_CLASS_BUTTON = 1, SOURCE_CLASS_POINTER = 2, SOURCE_CLASS_TRACKBALL = 4, SOURCE_CLASS_POSITION = 8, SOURCE_CLASS_JOYSTICK = 16;
    public static final int SOURCE_UNKNOWN = 0, SOURCE_KEYBOARD = 0x101, SOURCE_DPAD = 0x201, SOURCE_GAMEPAD = 0x401, SOURCE_TOUCHSCREEN = 0x1002, SOURCE_MOUSE = 0x2002,
        SOURCE_STYLUS = 0x4002, SOURCE_BLUETOOTH_STYLUS = 0xC002, SOURCE_TRACKBALL = 0x10004, SOURCE_MOUSE_RELATIVE = 0x20004, SOURCE_TOUCHPAD = 0x100008, SOURCE_TOUCH_NAVIGATION = 0x200000,
        SOURCE_ROTARY_ENCODER = 0x400000, SOURCE_JOYSTICK = 0x1000010, SOURCE_HDMI = 0x2000001, SOURCE_SENSOR = 0x4000000, SOURCE_ANY = 0xffffff00;
    public static final int KEYBOARD_TYPE_NONE = 0, KEYBOARD_TYPE_NON_ALPHABETIC = 1, KEYBOARD_TYPE_ALPHABETIC = 2;
    public static final int MOTION_RANGE_X = 0, MOTION_RANGE_Y = 1, MOTION_RANGE_PRESSURE = 2, MOTION_RANGE_SIZE = 3;
    public static final class MotionRange {
        private final int axis, source; private final float min, max, flat, fuzz, res;
        MotionRange(int axis, int source, float min, float max, float flat, float fuzz, float res) { this.axis = axis; this.source = source; this.min = min; this.max = max; this.flat = flat; this.fuzz = fuzz; this.res = res; }
        public int getAxis() { return axis; } public int getSource() { return source; } public boolean isFromSource(int s) { return (source & s) == s; }
        public float getMin() { return min; } public float getMax() { return max; } public float getRange() { return max - min; }
        public float getFlat() { return flat; } public float getFuzz() { return fuzz; } public float getResolution() { return res; }
    }
    private final int id, sources, keyboardType; private final String name, descriptor; private final boolean external, virtual;
    private final java.util.List<MotionRange> ranges = new java.util.ArrayList<>();
    private InputDevice(int id, String name, int sources, int kbType, boolean virtual) {
        this.id = id; this.name = name; this.sources = sources; this.keyboardType = kbType; this.virtual = virtual; this.external = false;
        this.descriptor = "jet-" + (id < 0 ? "virtual" : Integer.toString(id));
        if ((sources & SOURCE_CLASS_POINTER) != 0) { ranges.add(new MotionRange(MotionEvent.AXIS_X, sources, 0, 749, 0, 0, 0)); ranges.add(new MotionRange(MotionEvent.AXIS_Y, sources, 0, 1333, 0, 0, 0)); }
    }
    private static final InputDevice VIRTUAL = new InputDevice(-1, "Virtual", SOURCE_KEYBOARD, KEYBOARD_TYPE_ALPHABETIC, true);
    private static final InputDevice TOUCH = new InputDevice(1, "jet-touchscreen", SOURCE_TOUCHSCREEN, KEYBOARD_TYPE_NONE, false);
    public static int[] getDeviceIds() { return new int[] { VIRTUAL.id, TOUCH.id }; }
    public static InputDevice getDevice(int id) { return id == -1 ? VIRTUAL : id == 1 ? TOUCH : null; }
    public int getId() { return id; } public int getControllerNumber() { return 0; } public int getGeneration() { return 1; }
    public int getVendorId() { return 0; } public int getProductId() { return 0; }
    public String getDescriptor() { return descriptor; } public String getName() { return name; }
    public boolean isVirtual() { return virtual; } public boolean isExternal() { return external; } public boolean isFullKeyboard() { return keyboardType == KEYBOARD_TYPE_ALPHABETIC && !virtual; }
    public boolean isEnabled() { return true; }
    public int getSources() { return sources; } public boolean supportsSource(int s) { return (sources & s) == s; }
    public int getKeyboardType() { return keyboardType; }
    public KeyCharacterMap getKeyCharacterMap() { return KeyCharacterMap.load(id); }
    public boolean[] hasKeys(int... keys) { boolean[] r = new boolean[keys.length]; if (keyboardType == KEYBOARD_TYPE_ALPHABETIC) java.util.Arrays.fill(r, true); return r; }
    public MotionRange getMotionRange(int axis) { for (MotionRange r : ranges) if (r.axis == axis) return r; return null; }
    public MotionRange getMotionRange(int axis, int source) { for (MotionRange r : ranges) if (r.axis == axis && r.source == source) return r; return null; }
    public java.util.List<MotionRange> getMotionRanges() { return ranges; }
    public boolean hasMicrophone() { return false; }
    public android.os.Vibrator getVibrator() { return null; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel out, int flags) {}
    @Override public String toString() { return "Input Device " + id + ": " + name; }
}
