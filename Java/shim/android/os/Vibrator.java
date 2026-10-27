package android.os;
/** Headless host: no vibrator hardware. API 26-34 query surface answers "unsupported" like a device without haptics. */
public class Vibrator {
    public static final int VIBRATION_EFFECT_SUPPORT_UNKNOWN = 0, VIBRATION_EFFECT_SUPPORT_YES = 1, VIBRATION_EFFECT_SUPPORT_NO = 2;
    public boolean hasVibrator() { return false; }
    public boolean hasAmplitudeControl() { return false; }
    public boolean hasFrequencyControl() { return false; }
    public int getId() { return -1; }
    public float getResonantFrequency() { return Float.NaN; }
    public float getQFactor() { return Float.NaN; }
    public int areAllEffectsSupported(int... ids) { return VIBRATION_EFFECT_SUPPORT_NO; }
    public int[] areEffectsSupported(int... ids) { int[] r = new int[ids.length]; java.util.Arrays.fill(r, VIBRATION_EFFECT_SUPPORT_NO); return r; }
    public boolean areAllPrimitivesSupported(int... ids) { return false; }
    public boolean[] arePrimitivesSupported(int... ids) { return new boolean[ids.length]; }
    public int[] getPrimitiveDurations(int... ids) { return new int[ids.length]; }
    public void vibrate(long ms) {}
    public void vibrate(long ms, android.media.AudioAttributes a) {}
    public void vibrate(long[] p, int r) {}
    public void vibrate(long[] p, int r, android.media.AudioAttributes a) {}
    public void vibrate(VibrationEffect e) {}
    public void vibrate(VibrationEffect e, android.media.AudioAttributes a) {}
    public void cancel() {}
}
