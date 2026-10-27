package android.os;
/** AOSP semantics: open() releases all waiters and stays open until close(). */
public class ConditionVariable {
    private volatile boolean mCondition;
    public ConditionVariable() {} public ConditionVariable(boolean state) { mCondition = state; }
    public void open() { synchronized (this) { boolean old = mCondition; mCondition = true; if (!old) notifyAll(); } }
    public void close() { synchronized (this) { mCondition = false; } }
    public void block() { synchronized (this) { while (!mCondition) { try { wait(); } catch (InterruptedException e) {} } } }
    public boolean block(long timeoutMs) {
        if (timeoutMs == 0) { block(); return true; }
        synchronized (this) { long end = System.currentTimeMillis() + timeoutMs, now = System.currentTimeMillis();
            while (!mCondition && now < end) { try { wait(end - now); } catch (InterruptedException e) {} now = System.currentTimeMillis(); }
            return mCondition; }
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
