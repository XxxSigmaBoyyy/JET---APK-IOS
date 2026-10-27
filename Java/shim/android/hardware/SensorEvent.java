package android.hardware;
public class SensorEvent { public final float[] values; public Sensor sensor; public int accuracy; public long timestamp; public SensorEvent(int n) { values = new float[n]; } }
