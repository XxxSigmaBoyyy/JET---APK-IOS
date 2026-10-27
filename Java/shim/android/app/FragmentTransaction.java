package android.app;
import java.util.ArrayList;
public abstract class FragmentTransaction {
    public abstract FragmentTransaction add(Fragment f, String tag);
    public abstract FragmentTransaction add(int containerId, Fragment f);
    public abstract FragmentTransaction add(int containerId, Fragment f, String tag);
    public abstract FragmentTransaction replace(int containerId, Fragment f);
    public abstract FragmentTransaction replace(int containerId, Fragment f, String tag);
    public abstract FragmentTransaction remove(Fragment f);
    public abstract int commit();
    public abstract int commitAllowingStateLoss();
    public abstract void commitNow();
    public abstract void commitNowAllowingStateLoss();
}
