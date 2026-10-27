package dalvik.system;
public class BaseDexClassLoader extends DexClassLoader {
    public BaseDexClassLoader(String dexPath, java.io.File optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory != null ? optimizedDirectory.getPath() : null, librarySearchPath, parent);
    }
}
