package android.os;
import java.util.*;
public final class MessageQueue {
    private static final class Entry { final Message msg; final long when; Entry(Message m, long w){msg=m;when=w;} }
    private final PriorityQueue<Entry> q = new PriorityQueue<>(Comparator.comparingLong(e -> e.when));
    private boolean quit; boolean blocking; // blocking: background Looper threads wait for work until quit()
    synchronized void quit(boolean safe) { quit = true; if (!safe) q.clear(); else { long now = SystemClock.uptimeMillis(); q.removeIf(e -> e.when > now); } notifyAll(); }
    synchronized boolean enqueue(Message msg, long delayMs) {
        if (quit) return false;
        q.add(new Entry(msg, SystemClock.uptimeMillis() + delayMs));
        notifyAll();
        jet.runtime.Looper.dirty = true;
        return true;
    }
    synchronized Message next() {
        for (;;) {
            long now = SystemClock.uptimeMillis();
            Entry e = q.peek();
            if (e != null && e.when <= now) { q.poll(); return e.msg; }
            if (quit && q.isEmpty()) return null;
            long wait = e == null ? 50 : Math.max(1, e.when - now);
            try { wait(Math.min(wait, 50)); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); return null; }
            // harness: don't block forever — if empty briefly, return null to end loop in tests
            if (q.isEmpty() && !blocking) return null;
        }
    }
    synchronized Message pollReady(long horizonMs) {
        Entry e = q.peek();
        long now = SystemClock.uptimeMillis();
        if (e != null && e.when <= now + horizonMs) { if (e.when > now) SystemClock.jetAdvance(e.when - now); q.poll(); return e.msg; }
        return null;
    }
    synchronized void remove(Handler h, Object token) {
        q.removeIf(e -> e.msg.target == h && (token == null || e.msg.obj == token || e.msg.callback == token));
    }
    synchronized void removeWhat(Handler h, int what) {
        q.removeIf(e -> e.msg.target == h && e.msg.what == what);
    }
    synchronized boolean enqueueAt(Message msg, long uptime) { return enqueue(msg, Math.max(0, uptime - SystemClock.uptimeMillis())); }
    synchronized boolean hasWhat(Handler h, int what, Object obj) { for (Entry e : q) if (e.msg.target == h && e.msg.what == what && e.msg.callback == null && (obj == null || e.msg.obj == obj)) return true; return false; }
    synchronized boolean hasCallback(Handler h, Runnable r) { for (Entry e : q) if (e.msg.target == h && e.msg.callback == r) return true; return false; }
    synchronized void removeWhat(Handler h, int what, Object obj) { q.removeIf(e -> e.msg.target == h && e.msg.what == what && (obj == null || e.msg.obj == obj)); }
    synchronized void removeCallback(Handler h, Runnable r, Object token) { q.removeIf(e -> e.msg.target == h && e.msg.callback == r && (token == null || e.msg.obj == token)); }
    public boolean isIdle() { return q.isEmpty(); }
    public interface IdleHandler { boolean queueIdle(); }
    public void addIdleHandler(IdleHandler h) {} public void removeIdleHandler(IdleHandler h) {}
}
