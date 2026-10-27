package jet.runtime;
/** Dirty-flag for the host redraw loop. Framework code uses android.os.Looper. */
public final class Looper { public static volatile boolean dirty = true; private Looper() {} }
