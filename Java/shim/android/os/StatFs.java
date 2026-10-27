package android.os;
/** Real filesystem numbers from the host volume that holds the path (4 KiB block size). */
public class StatFs {
    private java.io.File mF; private static final long BS = 4096;
    public StatFs(String path) { restat(path); }
    public void restat(String path) { java.io.File f = new java.io.File(path); if (!f.exists()) throw new IllegalArgumentException("Invalid path: " + path); mF = f; }
    public long getBlockSizeLong() { return BS; } @Deprecated public int getBlockSize() { return (int) BS; }
    public long getBlockCountLong() { return mF.getTotalSpace() / BS; } @Deprecated public int getBlockCount() { return (int) Math.min(Integer.MAX_VALUE, getBlockCountLong()); }
    public long getFreeBlocksLong() { return mF.getFreeSpace() / BS; } @Deprecated public int getFreeBlocks() { return (int) Math.min(Integer.MAX_VALUE, getFreeBlocksLong()); }
    public long getAvailableBlocksLong() { return mF.getUsableSpace() / BS; } @Deprecated public int getAvailableBlocks() { return (int) Math.min(Integer.MAX_VALUE, getAvailableBlocksLong()); }
    public long getFreeBytes() { return getFreeBlocksLong() * BS; } public long getAvailableBytes() { return getAvailableBlocksLong() * BS; } public long getTotalBytes() { return getBlockCountLong() * BS; }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    // ---- END generated SDK surface ----
}
