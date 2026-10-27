package android.content.res;
import java.io.*;
public final class AssetManager {
    private final File root;
    public AssetManager(File root) { this.root = root; }
    public InputStream open(String fileName) throws IOException {
        if (root == null) { System.err.println("[ASSET-MISS] <no root> :: " + fileName); throw new FileNotFoundException(fileName); }
        while (fileName.startsWith("/")) fileName = fileName.substring(1);
        File f = new File(root, fileName);
        if (!f.isFile()) { System.err.println("[ASSET-MISS] " + root + " :: " + fileName); throw new FileNotFoundException(fileName); }
        return new FileInputStream(f);
    }
    public static final int ACCESS_UNKNOWN = 0, ACCESS_RANDOM = 1, ACCESS_STREAMING = 2, ACCESS_BUFFER = 3;
    public InputStream open(String fileName, int mode) throws IOException { return open(fileName); }
    public void close() {}
    public AssetFileDescriptor openFd(String fileName) throws IOException {
        if (root == null) throw new FileNotFoundException(fileName);
        while (fileName.startsWith("/")) fileName = fileName.substring(1);
        File f = new File(root, fileName);
        if (!f.isFile()) throw new FileNotFoundException(fileName);
        return new AssetFileDescriptor(f, 0, f.length());
    }
    public AssetFileDescriptor openNonAssetFd(String fileName) throws IOException { return openFd(fileName); }
    public File jetRoot() { return root; }
    public String[] list(String path) throws IOException {
        if (root == null) return new String[0];
        File f = new File(root, path == null ? "" : path);
        String[] n = f.list(); return n == null ? new String[0] : n;
    }
}
