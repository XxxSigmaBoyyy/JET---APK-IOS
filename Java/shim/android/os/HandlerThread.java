package android.os;
/** Real HandlerThread over the shim Looper. */
public class HandlerThread extends Thread {
    private final int priority; private volatile Looper looper; private int tid = -1; private Handler handler;
    public HandlerThread(String name) { this(name, 0); }
    public HandlerThread(String name, int priority) { super(name); this.priority = priority; setDaemon(true); }
    protected void onLooperPrepared() {}
    @Override public void run() {
        tid = (int) getId(); Looper.prepare();
        synchronized (this) { looper = Looper.myLooper(); notifyAll(); }
        onLooperPrepared(); Looper.loop(); tid = -1;
    }
    public Looper getLooper() {
        if (!isAlive()) return null;
        synchronized (this) { while (isAlive() && looper == null) try { wait(); } catch (InterruptedException e) { } }
        return looper;
    }
    public Handler getThreadHandler() { if (handler == null) handler = new Handler(getLooper()); return handler; }
    public boolean quit() { Looper l = getLooper(); if (l != null) { l.quit(); return true; } return false; }
    public boolean quitSafely() { Looper l = getLooper(); if (l != null) { l.quitSafely(); return true; } return false; }
    public int getThreadId() { return tid; }
}
