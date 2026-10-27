package jet.runtime;
import android.window.*;
import java.util.*;
/** Back callbacks by priority; harness "back" invokes the highest-priority, most recently registered one. */
public final class JetBackDispatcher implements OnBackInvokedDispatcher {
    private final List<Object[]> cbs = new ArrayList<>();
    public synchronized void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback c) {
        if (priority < 0) throw new IllegalArgumentException("Application registered OnBackInvokedCallback cannot have negative priority.");
        unregisterOnBackInvokedCallback(c); cbs.add(new Object[] { priority, c });
    }
    public synchronized void unregisterOnBackInvokedCallback(OnBackInvokedCallback c) { cbs.removeIf(e -> e[1] == c); }
    public synchronized OnBackInvokedCallback jetTop() { Object[] best = null; for (Object[] e : cbs) if (best == null || (Integer) e[0] >= (Integer) best[0]) best = e; return best == null ? null : (OnBackInvokedCallback) best[1]; }
    public boolean jetDispatchBack() { OnBackInvokedCallback c = jetTop(); if (c == null) return false; c.onBackInvoked(); return true; }
}
