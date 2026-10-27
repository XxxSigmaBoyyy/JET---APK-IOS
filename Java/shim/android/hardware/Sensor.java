package android.hardware;
public final class Sensor {
    public static final int TYPE_ACCELEROMETER = 1, TYPE_MAGNETIC_FIELD = 2, TYPE_ORIENTATION = 3, TYPE_GYROSCOPE = 4, TYPE_LIGHT = 5, TYPE_PRESSURE = 6, TYPE_PROXIMITY = 8,
        TYPE_GRAVITY = 9, TYPE_LINEAR_ACCELERATION = 10, TYPE_ROTATION_VECTOR = 11, TYPE_RELATIVE_HUMIDITY = 12, TYPE_AMBIENT_TEMPERATURE = 13, TYPE_GAME_ROTATION_VECTOR = 15,
        TYPE_STEP_DETECTOR = 18, TYPE_STEP_COUNTER = 19, TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20, TYPE_ALL = -1;
    private final int type; Sensor(int t) { type = t; }
    public int getType() { return type; } public String getName() { return "JET virtual sensor " + type; } public String getVendor() { return "JET"; }
    public float getMaximumRange() { return 0; } public float getResolution() { return 0; } public float getPower() { return 0; } public int getMinDelay() { return 0; } public int getVersion() { return 1; }
}
