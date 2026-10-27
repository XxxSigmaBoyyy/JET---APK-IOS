package android.text;
import java.util.*;
/** Shared span bookkeeping for the shim's spannable types. {@link #edit} shifts positions like AOSP SpannableStringBuilder. */
final class SpanSet {
    final LinkedHashMap<Object, int[]> spans = new LinkedHashMap<>();
    void set(Object w, int s, int e, int f) { spans.put(w, new int[]{s, e, f}); }
    void remove(Object w) { spans.remove(w); }
    @SuppressWarnings("unchecked") <T> T[] get(int s, int e, Class<T> type) {
        ArrayList<T> out = new ArrayList<>();
        for (Map.Entry<Object, int[]> en : spans.entrySet()) { int[] r = en.getValue(); if ((type == null || type.isInstance(en.getKey())) && r[0] <= e && r[1] >= s) out.add((T) en.getKey()); }
        return out.toArray((T[]) java.lang.reflect.Array.newInstance(type == null ? Object.class : type, out.size()));
    }
    int start(Object w) { int[] r = spans.get(w); return r == null ? -1 : r[0]; }
    int end(Object w) { int[] r = spans.get(w); return r == null ? -1 : r[1]; }
    int flags(Object w) { int[] r = spans.get(w); return r == null ? 0 : r[2]; }
    int next(int s, int limit, Class<?> type) { int best = limit; for (Map.Entry<Object, int[]> en : spans.entrySet()) { if (type != null && !type.isInstance(en.getKey())) continue; for (int p : new int[]{en.getValue()[0], en.getValue()[1]}) if (p > s && p < best) best = p; } return best; }
    /** Text [st,en) replaced by len chars: move every span endpoint the way AOSP does for MARK/POINT ends. */
    void edit(int st, int en, int len) {
        int delta = len - (en - st);
        for (int[] r : spans.values()) {
            r[0] = move(r[0], st, en, len, delta, (r[2] & 0xF0) == 0x20);
            r[1] = move(r[1], st, en, len, delta, (r[2] & 0x0F) == 0x02);
            if (r[1] < r[0]) r[1] = r[0];
        }
    }
    private static int move(int p, int st, int en, int len, int delta, boolean point) {
        if (p < st) return p;
        if (p > en) return p + delta;
        if (st == en) return point ? p + len : p;          // insertion at p: POINT moves after the new text, MARK stays
        if (p == st) return point && p == en ? st + len : st;
        return point || p == en ? st + len : st;           // inside / at end of the replaced range
    }
}
