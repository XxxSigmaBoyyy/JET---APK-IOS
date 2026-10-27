package android.database;
public class DataSetObservable extends Observable<DataSetObserver> {
    public DataSetObservable() {}
    public void notifyChanged() { synchronized (mObservers) { for (int i = mObservers.size() - 1; i >= 0; i--) mObservers.get(i).onChanged(); } }
    public void notifyInvalidated() { synchronized (mObservers) { for (int i = mObservers.size() - 1; i >= 0; i--) mObservers.get(i).onInvalidated(); } }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
