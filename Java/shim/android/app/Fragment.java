package android.app;
import android.content.Context;
import android.os.Bundle;
/** Prototype platform Fragment (deprecated framework API, still used by androidx ReportFragment on SDK<29). */
public class Fragment implements android.content.ComponentCallbacks2 {
    Activity mActivity; String mTag; int mState; Bundle mArgs;
    public Fragment() {}
    public final Activity getActivity() { return mActivity; }
    public Context getContext() { return mActivity; }
    public final String getTag() { return mTag; }
    public final boolean isAdded() { return mActivity != null; }
    public final boolean isResumed() { return mState >= 5; }
    public void setArguments(Bundle b) { mArgs = b; }
    public final Bundle getArguments() { return mArgs; }
    public final FragmentManager getFragmentManager() { return mActivity == null ? null : mActivity.getFragmentManager(); }
    public void onAttach(Context c) {}
    public void onAttach(Activity a) {}
    public void onCreate(Bundle b) {}
    public void onActivityCreated(Bundle b) {}
    public void onStart() {}
    public void onResume() {}
    public void onPause() {}
    public void onStop() {}
    public void onDestroy() {}
    public void onDetach() {}
    public void onSaveInstanceState(Bundle out) {}
    @Override public void onConfigurationChanged(android.content.res.Configuration c) {}
    @Override public void onLowMemory() {}
    @Override public void onTrimMemory(int level) {}
}
