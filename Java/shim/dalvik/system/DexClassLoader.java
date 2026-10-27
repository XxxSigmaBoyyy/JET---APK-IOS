package dalvik.system;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
/**
 * Load app jars (dex2jar output) via URLClassLoader.
 * findLibrary returns arm64 .so paths; actual System.load is rewritten to
 * {@code dev.jet.nativeapi.Natives.load*} by JetAppClassLoader / stage tooling.
 */
public class DexClassLoader extends URLClassLoader {
    private final String librarySearchPath;
    public DexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(toURLs(dexPath), parent != null ? parent : ClassLoader.getSystemClassLoader());
        this.librarySearchPath = librarySearchPath;
        if (librarySearchPath != null && !librarySearchPath.isEmpty()) {
            String cur = System.getProperty("jet.native.lib.dir", "");
            System.setProperty("jet.native.lib.dir",
                cur.isEmpty() ? librarySearchPath : cur + ":" + librarySearchPath);
        }
    }
    private static URL[] toURLs(String dexPath) {
        try {
            if (dexPath == null || dexPath.isEmpty()) return new URL[0];
            String[] parts = dexPath.split(File.pathSeparator);
            URL[] urls = new URL[parts.length];
            for (int i = 0; i < parts.length; i++) {
                File f = new File(parts[i]);
                urls[i] = f.toURI().toURL();
            }
            return urls;
        } catch (Exception e) {
            return new URL[0];
        }
    }
    @Override
    public String findLibrary(String name) {
        if (librarySearchPath == null) return null;
        for (String dir : librarySearchPath.split(File.pathSeparator)) {
            File a = new File(dir, "lib" + name + ".so");
            if (a.isFile()) return a.getAbsolutePath();
            File b = new File(new File(dir, "arm64-v8a"), "lib" + name + ".so");
            if (b.isFile()) return b.getAbsolutePath();
        }
        return null;
    }
}
