package android.os;
import java.util.concurrent.*;
public class Handler {
    private final Looper looper;
    private final Callback callback;
    public interface Callback { boolean handleMessage(Message msg); }
    public Handler() { this(Looper.myLooper()); }
    public Handler(Callback c) { this(Looper.myLooper(), c); }
    public Handler(Looper l) { this(l, null); }
    public Handler(Looper l, Callback c) { looper = l == null ? Looper.getMainLooper() : l; callback = c; }
    public final Looper getLooper() { return looper; }
    public void handleMessage(Message msg) {}
    public final boolean post(Runnable r) { return looper.queue.enqueue(Message.obtain(this, r), 0); }
    public final boolean postDelayed(Runnable r, long delayMs) { return looper.queue.enqueue(Message.obtain(this, r), delayMs); }
    public final boolean sendMessage(Message msg) { msg.target = this; return looper.queue.enqueue(msg, 0); }
    public final boolean sendEmptyMessage(int what) { return sendMessage(Message.obtain(this, what)); }
    public final boolean sendMessageDelayed(Message msg, long delayMs) { msg.target = this; return looper.queue.enqueue(msg, delayMs); }
    public final void removeCallbacksAndMessages(Object token) { looper.queue.remove(this, token); }
    public final void removeCallbacks(Runnable r) { looper.queue.remove(this, r); }
    public final void removeMessages(int what) { looper.queue.removeWhat(this, what); }
    final void dispatch(Message msg) {
        if (msg.callback != null) msg.callback.run();
        else if (callback != null && callback.handleMessage(msg)) {}
        else handleMessage(msg);
    }
    public Handler(Callback c, boolean async) { this(Looper.myLooper(), c); } public Handler(boolean async) { this(); }
    public static Handler createAsync(Looper l) { return new Handler(l); } public static Handler createAsync(Looper l, Callback c) { return new Handler(l, c); }
    public static Handler getMain() { return new Handler(Looper.getMainLooper()); }
    public final Message obtainMessage() { return Message.obtain(this); } public final Message obtainMessage(int what) { return Message.obtain(this, what); }
    public final Message obtainMessage(int what, Object obj) { return Message.obtain(this, what, obj); } public final Message obtainMessage(int what, int a1, int a2) { return Message.obtain(this, what, a1, a2); }
    public final Message obtainMessage(int what, int a1, int a2, Object obj) { return Message.obtain(this, what, a1, a2, obj); }
    public final boolean sendEmptyMessageDelayed(int what, long d) { return sendMessageDelayed(Message.obtain(this, what), d); }
    public final boolean sendEmptyMessageAtTime(int what, long t) { return sendMessageAtTime(Message.obtain(this, what), t); }
    public boolean sendMessageAtTime(Message m, long uptime) { m.target = this; return looper.queue.enqueueAt(m, uptime); }
    public final boolean sendMessageAtFrontOfQueue(Message m) { m.target = this; return looper.queue.enqueue(m, -1); }
    public final boolean postAtFrontOfQueue(Runnable r) { return looper.queue.enqueue(Message.obtain(this, r), -1); }
    public final boolean postAtTime(Runnable r, long uptime) { return looper.queue.enqueueAt(Message.obtain(this, r), uptime); }
    public final boolean postAtTime(Runnable r, Object token, long uptime) { Message m = Message.obtain(this, r); m.obj = token; return looper.queue.enqueueAt(m, uptime); }
    public final boolean postDelayed(Runnable r, Object token, long d) { Message m = Message.obtain(this, r); m.obj = token; return looper.queue.enqueue(m, d); }
    public final boolean hasMessages(int what) { return looper.queue.hasWhat(this, what, null); } public final boolean hasMessages(int what, Object obj) { return looper.queue.hasWhat(this, what, obj); }
    public final boolean hasCallbacks(Runnable r) { return looper.queue.hasCallback(this, r); }
    public final void removeMessages(int what, Object obj) { looper.queue.removeWhat(this, what, obj); }
    public final void removeCallbacks(Runnable r, Object token) { looper.queue.removeCallback(this, r, token); }
    public void dispatchMessage(Message msg) { dispatch(msg); }
    public final String getMessageName(Message m) { return m.callback != null ? m.callback.getClass().getName() : "0x" + Integer.toHexString(m.what); }
    @Override public String toString() { return "Handler (" + getClass().getName() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}"; }
}
