package dalvik.system;
/** API 27+: child-first PathClassLoader. On the JET host, app code is already defined by the host loader, so lookups delegate as PathClassLoader does. */
public final class DelegateLastClassLoader extends PathClassLoader {
    public DelegateLastClassLoader(String dexPath, ClassLoader parent) { super(dexPath, parent); }
    public DelegateLastClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) { super(dexPath, librarySearchPath, parent); }
    public DelegateLastClassLoader(String dexPath, String librarySearchPath, ClassLoader parent, boolean delegateResourceLoading) { super(dexPath, librarySearchPath, parent); }
}
