package android.content.res;
import java.io.*;
/** Uncompressed asset view: the bundle stores assets as plain files, so offset is 0 and length is the file length. */
public class AssetFileDescriptor implements Closeable {
    public static final long UNKNOWN_LENGTH = -1;
    private final File file; private final long start, length; private FileInputStream fis;
    public AssetFileDescriptor(File f, long start, long length) { file = f; this.start = start; this.length = length; }
    public File jetFile() { return file; }
    public FileDescriptor getFileDescriptor() { try { if (fis == null) fis = new FileInputStream(file); return fis.getFD(); } catch (IOException e) { throw new UncheckedIOException(e); } }
    public long getStartOffset() { return start; }
    public long getLength() { return length; }
    public long getDeclaredLength() { return length; }
    public FileInputStream createInputStream() throws IOException { FileInputStream in = new FileInputStream(file); if (start > 0) in.skip(start); return in; }
    @Override public void close() throws IOException { if (fis != null) { fis.close(); fis = null; } }
    @Override public String toString() { return "{AssetFileDescriptor: " + file + " start=" + start + " len=" + length + "}"; }
}
