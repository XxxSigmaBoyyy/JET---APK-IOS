package android.app;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
/** Prototype FragmentManager: immediate transactions, lifecycle driven by the owning Activity. */
public class FragmentManager {
    final Activity mHost; final ArrayList<Fragment> mAdded = new ArrayList<>(); int mHostState;
    FragmentManager(Activity host) { mHost = host; }
    public Fragment findFragmentByTag(String tag) {
        for (Fragment f : mAdded) if (tag != null && tag.equals(f.mTag)) return f;
        return null;
    }
    public Fragment findFragmentById(int id) { return null; }
    public boolean executePendingTransactions() { return false; }
    public List<Fragment> getFragments() { return new ArrayList<>(mAdded); }
    public FragmentTransaction beginTransaction() {
        final ArrayList<Runnable> ops = new ArrayList<>();
        return new FragmentTransaction() {
            public FragmentTransaction add(Fragment f, String tag) { ops.add(() -> attach(f, tag)); return this; }
            public FragmentTransaction add(int c, Fragment f) { return add(f, null); }
            public FragmentTransaction add(int c, Fragment f, String tag) { return add(f, tag); }
            public FragmentTransaction replace(int c, Fragment f) { return add(f, null); }
            public FragmentTransaction replace(int c, Fragment f, String tag) { return add(f, tag); }
            public FragmentTransaction remove(Fragment f) { ops.add(() -> { mAdded.remove(f); f.mActivity = null; }); return this; }
            public int commit() { for (Runnable r : ops) r.run(); ops.clear(); return 0; }
            public int commitAllowingStateLoss() { return commit(); }
            public void commitNow() { commit(); }
            public void commitNowAllowingStateLoss() { commit(); }
        };
    }
    void attach(Fragment f, String tag) {
        f.mTag = tag; f.mActivity = mHost; mAdded.add(f);
        f.onAttach((android.content.Context) mHost); f.onAttach(mHost);
        f.onCreate(null);
        moveTo(f, mHostState);
    }
    /** states: 0 none, 1 created, 2 activityCreated, 4 started, 5 resumed */
    void dispatch(int state) { mHostState = state; for (Fragment f : new ArrayList<>(mAdded)) moveTo(f, state); }
    private static void moveTo(Fragment f, int state) {
        try {
            if (f.mState < 2 && state >= 2) { f.mState = 2; f.onActivityCreated(null); }
            if (f.mState < 4 && state >= 4) { f.mState = 4; f.onStart(); }
            if (f.mState < 5 && state >= 5) { f.mState = 5; f.onResume(); }
        } catch (Throwable t) { System.err.println("[jet] fragment " + f.getClass().getName() + ": " + t); }
    }
}
