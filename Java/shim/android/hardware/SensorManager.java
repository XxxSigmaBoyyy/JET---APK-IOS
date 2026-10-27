package android.hardware;
/** Headless device: no sensors present (getDefaultSensor returns null, as on real devices lacking the sensor). */
public class SensorManager {
    public static final int SENSOR_DELAY_FASTEST = 0, SENSOR_DELAY_GAME = 1, SENSOR_DELAY_UI = 2, SENSOR_DELAY_NORMAL = 3, SENSOR_STATUS_ACCURACY_HIGH = 3, SENSOR_STATUS_UNRELIABLE = 0;
    public static final float GRAVITY_EARTH = 9.80665f, STANDARD_GRAVITY = GRAVITY_EARTH; public static final int AXIS_X = 1, AXIS_Y = 2, AXIS_Z = 3, AXIS_MINUS_X = 129, AXIS_MINUS_Y = 130, AXIS_MINUS_Z = 131;
    public Sensor getDefaultSensor(int type) { return null; } public Sensor getDefaultSensor(int type, boolean wake) { return null; }
    public java.util.List<Sensor> getSensorList(int type) { return new java.util.ArrayList<>(); }
    public boolean registerListener(SensorEventListener l, Sensor s, int rate) { return false; } public boolean registerListener(SensorEventListener l, Sensor s, int rate, int lat) { return false; }
    public boolean registerListener(SensorEventListener l, Sensor s, int rate, android.os.Handler h) { return false; }
    public void unregisterListener(SensorEventListener l) {} public void unregisterListener(SensorEventListener l, Sensor s) {}
    public static boolean getRotationMatrix(float[] R, float[] I, float[] g, float[] m) { return false; }
    public static void getRotationMatrixFromVector(float[] R, float[] v) { java.util.Arrays.fill(R, 0); }
    public static float[] getOrientation(float[] R, float[] v) { java.util.Arrays.fill(v, 0); return v; }
    public static boolean remapCoordinateSystem(float[] in, int x, int y, float[] out) { System.arraycopy(in, 0, out, 0, Math.min(in.length, out.length)); return true; }
    public static float getAltitude(float p0, float p) { return (float) (44330.0 * (1.0 - Math.pow(p / p0, 1.0 / 5.255))); }
    public static float getInclination(float[] I) { return 0; } public static void getQuaternionFromVector(float[] q, float[] rv) { java.util.Arrays.fill(q, 0); q[0] = 1; }
}
