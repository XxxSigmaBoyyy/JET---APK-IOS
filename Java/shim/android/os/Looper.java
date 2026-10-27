package android.os;
/** Replaces jet.runtime.Looper for framework compatibility; keeps dirty flag via jet.runtime. */
public final class Looper {
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();
    private static Looper sMain;
    final MessageQueue queue = new MessageQueue();
    private Thread thread = Thread.currentThread();
    private Looper() {}
    public static void prepare() { if (sThreadLocal.get() != null) throw new RuntimeException("Only one Looper may be created per thread"); Looper l = new Looper(); l.queue.blocking = true; sThreadLocal.set(l); }
    public static void prepareMainLooper() { prepare(); synchronized (Looper.class) { if (sMain != null) throw new IllegalStateException(); sMain = myLooper(); sMain.queue.blocking = false; } }
    public static Looper myLooper() { return sThreadLocal.get(); }
    public static MessageQueue myQueue() { Looper l = myLooper(); return l != null ? l.queue : getMainLooper().queue; }
    public static Looper getMainLooper() { return sMain; }
    public Thread getThread() { return thread; }
    public MessageQueue getQueue() { return queue; }
    public void quit() { queue.quit(false); }
    public void quitSafely() { queue.quit(true); }
    public boolean isCurrentThread() { return Thread.currentThread() == thread; }
    /** Harness: claim main looper for the current worker thread (static <clinit> may have bound another). */
    public static void jetAdoptCurrentThread() {
        Looper main = sMain;
        if (main == null) { prepareMainLooper(); main = sMain; }
        main.thread = Thread.currentThread();
        sThreadLocal.set(main);
    }
    public static void loop() {
        Looper me = myLooper();
        for (;;) {
            Message msg = me.queue.next();
            if (msg == null) return;
            msg.target.dispatch(msg);
        }
    }
    /** Harness: run queued messages, treating delays up to {@code horizonMs} as elapsed. */
    public static int jetPump(long horizonMs) {
        Looper me = sMain; int n = 0;
        Message m;
        long deadline = SystemClock.uptimeMillis() + horizonMs;  // fixed at pump start (virtual time may advance while pumping)
        while (n < 2000 && (m = me.queue.pollReady(Math.max(0, deadline - SystemClock.uptimeMillis()))) != null) { m.target.dispatch(m); n++; }
        return n;
    }
    /** Host: run the main looper for ms of virtual time (fast-forwarding through delayed messages), then set the clock to the end. */
    public static int jetRunFor(long ms) {
        long end = SystemClock.uptimeMillis() + ms; int n = 0;
        while (SystemClock.uptimeMillis() < end) {
            n += jetPump(Math.max(0, end - SystemClock.uptimeMillis()));
            if (n >= 20000) break;
            long left = end - SystemClock.uptimeMillis();
            if (left > 0) { SystemClock.jetAdvance(Math.min(left, 16)); try { Thread.sleep(1); } catch (InterruptedException e) { break; } }
        }
        return n;
    }
    static { prepareMainLooper(); }
}
