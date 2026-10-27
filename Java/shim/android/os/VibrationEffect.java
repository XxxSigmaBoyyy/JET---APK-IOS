package android.os;
public abstract class VibrationEffect { public static final int DEFAULT_AMPLITUDE = -1, EFFECT_CLICK = 0, EFFECT_DOUBLE_CLICK = 1, EFFECT_TICK = 2, EFFECT_HEAVY_CLICK = 5;
    private static final VibrationEffect E = new VibrationEffect() {};
    public static VibrationEffect createOneShot(long ms, int a) { return E; } public static VibrationEffect createWaveform(long[] t, int r) { return E; } public static VibrationEffect createWaveform(long[] t, int[] a, int r) { return E; } public static VibrationEffect createPredefined(int id) { return E; } }
