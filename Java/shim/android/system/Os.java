package android.system;
/** Subset of libcore Os backed by the host JVM (process identity + an environment overlay for setenv). */
public final class Os {
    private Os() {}
    private static final java.util.Map<String, String> ENV = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> UNSET = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static int getuid() { return android.os.Process.myUid(); } public static int geteuid() { return getuid(); }
    public static int getgid() { return getuid(); } public static int getegid() { return getuid(); }
    public static int getpid() { return android.os.Process.myPid(); } public static int getppid() { return ProcessHandle.current().parent().map(p -> (int) p.pid()).orElse(1); }
    public static int gettid() { return (int) Thread.currentThread().getId(); }
    public static String getenv(String name) { if (UNSET.contains(name)) return null; String v = ENV.get(name); return v != null ? v : System.getenv(name); }
    public static String[] environ() { java.util.Map<String, String> m = new java.util.TreeMap<>(System.getenv()); m.keySet().removeAll(UNSET); m.putAll(ENV); java.util.List<String> r = new java.util.ArrayList<>(); for (java.util.Map.Entry<String, String> e : m.entrySet()) r.add(e.getKey() + "=" + e.getValue()); return r.toArray(new String[0]); }
    public static void setenv(String name, String value, boolean overwrite) throws ErrnoException { if (name == null || name.isEmpty() || name.indexOf('=') >= 0) throw new ErrnoException("setenv", 22 /* EINVAL */); if (!overwrite && getenv(name) != null) return; UNSET.remove(name); ENV.put(name, value); }
    public static void unsetenv(String name) throws ErrnoException { ENV.remove(name); UNSET.add(name); }
    public static long sysconf(int name) { /* bionic values: _SC_PAGESIZE 0x27, _SC_NPROCESSORS_CONF 0x60, _SC_NPROCESSORS_ONLN 0x61, _SC_CLK_TCK 0x06 */ switch (name) { case 0x27: return 4096; case 0x60: case 0x61: return Runtime.getRuntime().availableProcessors(); case 0x06: return 100; default: return -1; } }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public static java.io.FileDescriptor accept(java.io.FileDescriptor p0, java.net.InetSocketAddress p1) { jet.runtime.ShimStubs.hit("android.system.Os.accept(Ljava/io/FileDescriptor;Ljava/net/InetSocketAddress;)Ljava/io/FileDescriptor;"); return null; }
    public static boolean access(java.lang.String p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.access(Ljava/lang/String;I)Z"); return false; }
    public static void bind(java.io.FileDescriptor p0, java.net.InetAddress p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.bind(Ljava/io/FileDescriptor;Ljava/net/InetAddress;I)V"); }
    public static void bind(java.io.FileDescriptor p0, java.net.SocketAddress p1) { jet.runtime.ShimStubs.hit("android.system.Os.bind(Ljava/io/FileDescriptor;Ljava/net/SocketAddress;)V"); }
    public static void chmod(java.lang.String p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.chmod(Ljava/lang/String;I)V"); }
    public static void chown(java.lang.String p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.chown(Ljava/lang/String;II)V"); }
    public static void close(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.close(Ljava/io/FileDescriptor;)V"); }
    public static void connect(java.io.FileDescriptor p0, java.net.InetAddress p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.connect(Ljava/io/FileDescriptor;Ljava/net/InetAddress;I)V"); }
    public static void connect(java.io.FileDescriptor p0, java.net.SocketAddress p1) { jet.runtime.ShimStubs.hit("android.system.Os.connect(Ljava/io/FileDescriptor;Ljava/net/SocketAddress;)V"); }
    public static java.io.FileDescriptor dup(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.dup(Ljava/io/FileDescriptor;)Ljava/io/FileDescriptor;"); return null; }
    public static java.io.FileDescriptor dup2(java.io.FileDescriptor p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.dup2(Ljava/io/FileDescriptor;I)Ljava/io/FileDescriptor;"); return null; }
    public static void execv(java.lang.String p0, java.lang.String[] p1) { jet.runtime.ShimStubs.hit("android.system.Os.execv(Ljava/lang/String;[Ljava/lang/String;)V"); }
    public static void execve(java.lang.String p0, java.lang.String[] p1, java.lang.String[] p2) { jet.runtime.ShimStubs.hit("android.system.Os.execve(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V"); }
    public static void fchmod(java.io.FileDescriptor p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.fchmod(Ljava/io/FileDescriptor;I)V"); }
    public static void fchown(java.io.FileDescriptor p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.fchown(Ljava/io/FileDescriptor;II)V"); }
    public static int fcntlInt(java.io.FileDescriptor p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.fcntlInt(Ljava/io/FileDescriptor;II)I"); return 0; }
    public static void fdatasync(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.fdatasync(Ljava/io/FileDescriptor;)V"); }
    public static void fsync(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.fsync(Ljava/io/FileDescriptor;)V"); }
    public static void ftruncate(java.io.FileDescriptor p0, long p1) { jet.runtime.ShimStubs.hit("android.system.Os.ftruncate(Ljava/io/FileDescriptor;J)V"); }
    public static java.lang.String gai_strerror(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.gai_strerror(I)Ljava/lang/String;"); return null; }
    public static java.net.SocketAddress getpeername(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.getpeername(Ljava/io/FileDescriptor;)Ljava/net/SocketAddress;"); return null; }
    public static java.net.SocketAddress getsockname(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.getsockname(Ljava/io/FileDescriptor;)Ljava/net/SocketAddress;"); return null; }
    public static byte[] getxattr(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.getxattr(Ljava/lang/String;Ljava/lang/String;)[B"); return null; }
    public static java.lang.String if_indextoname(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.if_indextoname(I)Ljava/lang/String;"); return null; }
    public static int if_nametoindex(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.system.Os.if_nametoindex(Ljava/lang/String;)I"); return 0; }
    public static java.net.InetAddress inet_pton(int p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.inet_pton(ILjava/lang/String;)Ljava/net/InetAddress;"); return null; }
    public static boolean isatty(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.isatty(Ljava/io/FileDescriptor;)Z"); return false; }
    public static void kill(int p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.kill(II)V"); }
    public static void lchown(java.lang.String p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.lchown(Ljava/lang/String;II)V"); }
    public static void link(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.link(Ljava/lang/String;Ljava/lang/String;)V"); }
    public static void listen(java.io.FileDescriptor p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.listen(Ljava/io/FileDescriptor;I)V"); }
    public static java.lang.String[] listxattr(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.system.Os.listxattr(Ljava/lang/String;)[Ljava/lang/String;"); return null; }
    public static long lseek(java.io.FileDescriptor p0, long p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.lseek(Ljava/io/FileDescriptor;JI)J"); return 0; }
    public static java.io.FileDescriptor memfd_create(java.lang.String p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.memfd_create(Ljava/lang/String;I)Ljava/io/FileDescriptor;"); return null; }
    public static void mincore(long p0, long p1, byte[] p2) { jet.runtime.ShimStubs.hit("android.system.Os.mincore(JJ[B)V"); }
    public static void mkdir(java.lang.String p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.mkdir(Ljava/lang/String;I)V"); }
    public static void mkfifo(java.lang.String p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.mkfifo(Ljava/lang/String;I)V"); }
    public static void mlock(long p0, long p1) { jet.runtime.ShimStubs.hit("android.system.Os.mlock(JJ)V"); }
    public static long mmap(long p0, long p1, int p2, int p3, java.io.FileDescriptor p4, long p5) { jet.runtime.ShimStubs.hit("android.system.Os.mmap(JJIILjava/io/FileDescriptor;J)J"); return 0; }
    public static void msync(long p0, long p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.msync(JJI)V"); }
    public static void munlock(long p0, long p1) { jet.runtime.ShimStubs.hit("android.system.Os.munlock(JJ)V"); }
    public static void munmap(long p0, long p1) { jet.runtime.ShimStubs.hit("android.system.Os.munmap(JJ)V"); }
    public static java.io.FileDescriptor open(java.lang.String p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.open(Ljava/lang/String;II)Ljava/io/FileDescriptor;"); return null; }
    public static java.io.FileDescriptor[] pipe() { jet.runtime.ShimStubs.hit("android.system.Os.pipe()[Ljava/io/FileDescriptor;"); return null; }
    public static void posix_fallocate(java.io.FileDescriptor p0, long p1, long p2) { jet.runtime.ShimStubs.hit("android.system.Os.posix_fallocate(Ljava/io/FileDescriptor;JJ)V"); }
    public static int prctl(int p0, long p1, long p2, long p3, long p4) { jet.runtime.ShimStubs.hit("android.system.Os.prctl(IJJJJ)I"); return 0; }
    public static int pread(java.io.FileDescriptor p0, java.nio.ByteBuffer p1, long p2) { jet.runtime.ShimStubs.hit("android.system.Os.pread(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)I"); return 0; }
    public static int pread(java.io.FileDescriptor p0, byte[] p1, int p2, int p3, long p4) { jet.runtime.ShimStubs.hit("android.system.Os.pread(Ljava/io/FileDescriptor;[BIIJ)I"); return 0; }
    public static int pwrite(java.io.FileDescriptor p0, java.nio.ByteBuffer p1, long p2) { jet.runtime.ShimStubs.hit("android.system.Os.pwrite(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;J)I"); return 0; }
    public static int pwrite(java.io.FileDescriptor p0, byte[] p1, int p2, int p3, long p4) { jet.runtime.ShimStubs.hit("android.system.Os.pwrite(Ljava/io/FileDescriptor;[BIIJ)I"); return 0; }
    public static int read(java.io.FileDescriptor p0, java.nio.ByteBuffer p1) { jet.runtime.ShimStubs.hit("android.system.Os.read(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;)I"); return 0; }
    public static int read(java.io.FileDescriptor p0, byte[] p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.system.Os.read(Ljava/io/FileDescriptor;[BII)I"); return 0; }
    public static java.lang.String readlink(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.system.Os.readlink(Ljava/lang/String;)Ljava/lang/String;"); return null; }
    public static int readv(java.io.FileDescriptor p0, java.lang.Object[] p1, int[] p2, int[] p3) { jet.runtime.ShimStubs.hit("android.system.Os.readv(Ljava/io/FileDescriptor;[Ljava/lang/Object;[I[I)I"); return 0; }
    public static int recvfrom(java.io.FileDescriptor p0, java.nio.ByteBuffer p1, int p2, java.net.InetSocketAddress p3) { jet.runtime.ShimStubs.hit("android.system.Os.recvfrom(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;ILjava/net/InetSocketAddress;)I"); return 0; }
    public static int recvfrom(java.io.FileDescriptor p0, byte[] p1, int p2, int p3, int p4, java.net.InetSocketAddress p5) { jet.runtime.ShimStubs.hit("android.system.Os.recvfrom(Ljava/io/FileDescriptor;[BIIILjava/net/InetSocketAddress;)I"); return 0; }
    public static void remove(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.system.Os.remove(Ljava/lang/String;)V"); }
    public static void removexattr(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.removexattr(Ljava/lang/String;Ljava/lang/String;)V"); }
    public static void rename(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.rename(Ljava/lang/String;Ljava/lang/String;)V"); }
    public static int sendto(java.io.FileDescriptor p0, java.nio.ByteBuffer p1, int p2, java.net.InetAddress p3, int p4) { jet.runtime.ShimStubs.hit("android.system.Os.sendto(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;ILjava/net/InetAddress;I)I"); return 0; }
    public static int sendto(java.io.FileDescriptor p0, byte[] p1, int p2, int p3, int p4, java.net.InetAddress p5, int p6) { jet.runtime.ShimStubs.hit("android.system.Os.sendto(Ljava/io/FileDescriptor;[BIIILjava/net/InetAddress;I)I"); return 0; }
    public static int sendto(java.io.FileDescriptor p0, byte[] p1, int p2, int p3, int p4, java.net.SocketAddress p5) { jet.runtime.ShimStubs.hit("android.system.Os.sendto(Ljava/io/FileDescriptor;[BIIILjava/net/SocketAddress;)I"); return 0; }
    public static void setegid(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.setegid(I)V"); }
    public static void seteuid(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.seteuid(I)V"); }
    public static void setgid(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.setgid(I)V"); }
    public static int setsid() { jet.runtime.ShimStubs.hit("android.system.Os.setsid()I"); return 0; }
    public static void setsockoptInt(java.io.FileDescriptor p0, int p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.system.Os.setsockoptInt(Ljava/io/FileDescriptor;III)V"); }
    public static void setuid(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.setuid(I)V"); }
    public static void setxattr(java.lang.String p0, java.lang.String p1, byte[] p2, int p3) { jet.runtime.ShimStubs.hit("android.system.Os.setxattr(Ljava/lang/String;Ljava/lang/String;[BI)V"); }
    public static void shutdown(java.io.FileDescriptor p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.shutdown(Ljava/io/FileDescriptor;I)V"); }
    public static java.io.FileDescriptor socket(int p0, int p1, int p2) { jet.runtime.ShimStubs.hit("android.system.Os.socket(III)Ljava/io/FileDescriptor;"); return null; }
    public static void socketpair(int p0, int p1, int p2, java.io.FileDescriptor p3, java.io.FileDescriptor p4) { jet.runtime.ShimStubs.hit("android.system.Os.socketpair(IIILjava/io/FileDescriptor;Ljava/io/FileDescriptor;)V"); }
    public static java.lang.String strerror(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.strerror(I)Ljava/lang/String;"); return null; }
    public static java.lang.String strsignal(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.strsignal(I)Ljava/lang/String;"); return null; }
    public static void symlink(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.system.Os.symlink(Ljava/lang/String;Ljava/lang/String;)V"); }
    public static void tcdrain(java.io.FileDescriptor p0) { jet.runtime.ShimStubs.hit("android.system.Os.tcdrain(Ljava/io/FileDescriptor;)V"); }
    public static void tcsendbreak(java.io.FileDescriptor p0, int p1) { jet.runtime.ShimStubs.hit("android.system.Os.tcsendbreak(Ljava/io/FileDescriptor;I)V"); }
    public static int umask(int p0) { jet.runtime.ShimStubs.hit("android.system.Os.umask(I)I"); return 0; }
    public static int write(java.io.FileDescriptor p0, java.nio.ByteBuffer p1) { jet.runtime.ShimStubs.hit("android.system.Os.write(Ljava/io/FileDescriptor;Ljava/nio/ByteBuffer;)I"); return 0; }
    public static int write(java.io.FileDescriptor p0, byte[] p1, int p2, int p3) { jet.runtime.ShimStubs.hit("android.system.Os.write(Ljava/io/FileDescriptor;[BII)I"); return 0; }
    public static int writev(java.io.FileDescriptor p0, java.lang.Object[] p1, int[] p2, int[] p3) { jet.runtime.ShimStubs.hit("android.system.Os.writev(Ljava/io/FileDescriptor;[Ljava/lang/Object;[I[I)I"); return 0; }
    // ---- END generated SDK surface ----
}
