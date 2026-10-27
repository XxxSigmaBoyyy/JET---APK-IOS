package jet.gfx;
/** Skija 0.143 load race: Library.load() (synchronized) sets the volatile _loaded flag BEFORE _nAfterLoad() fills the native JNI caches
 *  (jclass/jmethodID for ImageInfo, ColorInfo, ...), while every Skija class's static init calls Library.staticLoad(), which only reads
 *  _loaded unsynchronized. A second thread (e.g. a Glide decode thread) that first touches Skija while the main thread is inside load()
 *  sees _loaded=true, skips loading and calls a native whose cache is still null -> SIGSEGV in jni_NewObjectV
 *  (-Xcheck:jni: "Wrong object class or methodID passed to JNI call").
 *  Fix: every JET entry point into Skija first calls the synchronized Library.load(); a caller arriving mid-load blocks on the monitor
 *  until _nAfterLoad() has finished. */
public final class SkiaInit {
    private SkiaInit() {}
    private static volatile boolean sDone;
    public static void ensure() {
        if (sDone) return;
        io.github.humbleui.skija.impl.Library.load();
        sDone = true;
    }
}
