package android.graphics.drawable;
import java.util.ArrayList;
/**
 * StateListDrawable: first matching state set wins (AOSP StateSet.stateSetMatches semantics).
 * Host view state bits: 1=pressed, 2=selected; enabled/window_focused are assumed true, focused/checked/activated false.
 */
public class StateListDrawable extends DrawableContainer {
    private final ArrayList<int[]> sets = new ArrayList<>();
    private final ArrayList<Drawable> ds = new ArrayList<>();
    static final int PRESSED = 16842919, SELECTED = 16842913, ENABLED = 16842910, WINDOW_FOCUSED = 16842909;
    public StateListDrawable() {}
    /** legacy host form: required internal-bit mask */
    public void addState(int requiredMask, Drawable d) {
        int[] s = new int[2]; int n = 0;
        if ((requiredMask & 1) != 0) s[n++] = PRESSED;
        if ((requiredMask & 2) != 0) s[n++] = SELECTED;
        sets.add(java.util.Arrays.copyOf(s, n)); ds.add(d);
    }
    public void addState(int[] stateSet, Drawable d) { sets.add(stateSet == null ? new int[0] : stateSet.clone()); ds.add(d); }
    static boolean has(int attr, int state) {
        if (attr == PRESSED) return (state & 1) != 0;
        if (attr == SELECTED) return (state & 2) != 0;
        return attr == ENABLED || attr == WINDOW_FOCUSED;
    }
    static boolean matches(int[] set, int state) {
        for (int a : set) {
            if (a == 0) break;
            if (a > 0 ? !has(a, state) : has(-a, state)) return false;
        }
        return true;
    }
    public int getStateCount() { return ds.size(); }
    public int[] getStateSet(int i) { return sets.get(i); }
    public Drawable getStateDrawable(int i) { return ds.get(i); }
    public int findStateDrawableIndex(int[] stateSet) { for (int i = 0; i < sets.size(); i++) if (java.util.Arrays.equals(sets.get(i), stateSet)) return i; return -1; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] st) { boolean ch = false; for (Drawable d : ds) if (d != null) ch |= d.setState(st); return true; }
    /** AOSP StateSet.stateSetMatches against the real drawable state (set via setState); positive ids required, negative forbidden. */
    static boolean matchesFull(int[] set, int[] st) {
        for (int a : set) {
            if (a == 0) break;
            int id = a > 0 ? a : -a; boolean has = false;
            for (int x : st) { if (x == 0) break; if (x == id) { has = true; break; } }
            if (a > 0 ? !has : has) return false;
        }
        return true;
    }
    /** Index the drawable state selects: the real state set when a host called setState, else the legacy pressed/selected bits. */
    int jetIndex(int state) {
        int[] st = getState(); boolean full = st != null && st.length > 0;
        for (int i = 0; i < ds.size(); i++) if (full ? matchesFull(sets.get(i), st) : matches(sets.get(i), state)) return i;
        return -1;
    }
    @Override public void jetDraw(android.graphics.Canvas g, int l, int t, int r, int b, int state) {
        int i = jetIndex(state);
        if (i >= 0 && ds.get(i) != null) { Drawable d = ds.get(i); if (d.isStateful()) d.setState(getState()); d.jetDraw(g, l, t, r, b, state); }
    }
    @Override public Drawable getCurrent() { int i = jetIndex(0); return i >= 0 && ds.get(i) != null ? ds.get(i) : this; }
    @Override public boolean getPadding(android.graphics.Rect p) { Drawable c = getCurrent(); if (c != this && c != null) return c.getPadding(p); return super.getPadding(p); }
    @Override public int getIntrinsicWidth() { Drawable c = getCurrent(); return c != this && c != null ? c.getIntrinsicWidth() : -1; }
    @Override public int getIntrinsicHeight() { Drawable c = getCurrent(); return c != this && c != null ? c.getIntrinsicHeight() : -1; }
}
