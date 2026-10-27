package libcore.io;
import java.nio.ByteOrder;
/** Android libcore.io.Memory (hidden API): raw peek/poke on native addresses and byte-array endian helpers.
 *  Libraries probe Class.forName("libcore.io.Memory") to detect Android (protobuf-lite UnsafeUtil); they then use the Android
 *  memory accessor. Linux host: backed by sun.misc.Unsafe; iOS host: same semantics over raw pointers. */
public final class Memory {
    private Memory() {}
    /** sun.misc.Unsafe via reflection (no compile-time dependency on the internal API). */
    private static final class U {
        static final Object T; static final java.lang.invoke.MethodHandle GB, PB, GS, PS, GI, PI, GL, PL;
        static {
            try {
                Class<?> c = Class.forName("sun.misc.Unsafe"); java.lang.reflect.Field f = c.getDeclaredField("theUnsafe"); f.setAccessible(true); T = f.get(null);
                java.lang.invoke.MethodHandles.Lookup l = java.lang.invoke.MethodHandles.lookup();
                GB = l.unreflect(c.getMethod("getByte", long.class)).bindTo(T); PB = l.unreflect(c.getMethod("putByte", long.class, byte.class)).bindTo(T);
                GS = l.unreflect(c.getMethod("getShort", long.class)).bindTo(T); PS = l.unreflect(c.getMethod("putShort", long.class, short.class)).bindTo(T);
                GI = l.unreflect(c.getMethod("getInt", long.class)).bindTo(T); PI = l.unreflect(c.getMethod("putInt", long.class, int.class)).bindTo(T);
                GL = l.unreflect(c.getMethod("getLong", long.class)).bindTo(T); PL = l.unreflect(c.getMethod("putLong", long.class, long.class)).bindTo(T);
            } catch (Exception e) { throw new ExceptionInInitializerError(e); }
        }
        static byte getByte(long a) { try { return (byte) GB.invokeExact(a); } catch (Throwable t) { throw new RuntimeException(t); } }
        static void putByte(long a, byte v) { try { PB.invokeExact(a, v); } catch (Throwable t) { throw new RuntimeException(t); } }
        static short getShort(long a) { try { return (short) GS.invokeExact(a); } catch (Throwable t) { throw new RuntimeException(t); } }
        static void putShort(long a, short v) { try { PS.invokeExact(a, v); } catch (Throwable t) { throw new RuntimeException(t); } }
        static int getInt(long a) { try { return (int) GI.invokeExact(a); } catch (Throwable t) { throw new RuntimeException(t); } }
        static void putInt(long a, int v) { try { PI.invokeExact(a, v); } catch (Throwable t) { throw new RuntimeException(t); } }
        static long getLong(long a) { try { return (long) GL.invokeExact(a); } catch (Throwable t) { throw new RuntimeException(t); } }
        static void putLong(long a, long v) { try { PL.invokeExact(a, v); } catch (Throwable t) { throw new RuntimeException(t); } }
    }
    private static final boolean NATIVE_LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    public static byte peekByte(long a) { return U.getByte(a); }
    public static void pokeByte(long a, byte v) { U.putByte(a, v); }
    public static short peekShort(long a, boolean swap) { short v = U.getShort(a); return swap ? Short.reverseBytes(v) : v; }
    public static int peekInt(long a, boolean swap) { int v = U.getInt(a); return swap ? Integer.reverseBytes(v) : v; }
    public static long peekLong(long a, boolean swap) { long v = U.getLong(a); return swap ? Long.reverseBytes(v) : v; }
    public static void pokeShort(long a, short v, boolean swap) { U.putShort(a, swap ? Short.reverseBytes(v) : v); }
    public static void pokeInt(long a, int v, boolean swap) { U.putInt(a, swap ? Integer.reverseBytes(v) : v); }
    public static void pokeLong(long a, long v, boolean swap) { U.putLong(a, swap ? Long.reverseBytes(v) : v); }
    public static void peekByteArray(long a, byte[] dst, int off, int n) { for (int i = 0; i < n; i++) dst[off + i] = U.getByte(a + i); }
    public static void pokeByteArray(long a, byte[] src, int off, int n) { for (int i = 0; i < n; i++) U.putByte(a + i, src[off + i]); }
    public static void memmove(Object dst, int dstOff, Object src, int srcOff, long n) { System.arraycopy(src, srcOff, dst, dstOff, (int) n); }
    public static int peekInt(byte[] b, int o, ByteOrder order) {
        return order == ByteOrder.BIG_ENDIAN ? ((b[o] & 0xff) << 24) | ((b[o + 1] & 0xff) << 16) | ((b[o + 2] & 0xff) << 8) | (b[o + 3] & 0xff)
                : ((b[o + 3] & 0xff) << 24) | ((b[o + 2] & 0xff) << 16) | ((b[o + 1] & 0xff) << 8) | (b[o] & 0xff);
    }
    public static long peekLong(byte[] b, int o, ByteOrder order) {
        long hi = peekInt(b, order == ByteOrder.BIG_ENDIAN ? o : o + 4, order) & 0xffffffffL, lo = peekInt(b, order == ByteOrder.BIG_ENDIAN ? o + 4 : o, order) & 0xffffffffL;
        return (hi << 32) | lo;
    }
    public static short peekShort(byte[] b, int o, ByteOrder order) { return order == ByteOrder.BIG_ENDIAN ? (short) ((b[o] << 8) | (b[o + 1] & 0xff)) : (short) ((b[o + 1] << 8) | (b[o] & 0xff)); }
    public static void pokeInt(byte[] b, int o, int v, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) { b[o] = (byte) (v >> 24); b[o + 1] = (byte) (v >> 16); b[o + 2] = (byte) (v >> 8); b[o + 3] = (byte) v; }
        else { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24); }
    }
    public static void pokeLong(byte[] b, int o, long v, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) { pokeInt(b, o, (int) (v >> 32), order); pokeInt(b, o + 4, (int) v, order); }
        else { pokeInt(b, o, (int) v, order); pokeInt(b, o + 4, (int) (v >> 32), order); }
    }
    public static void pokeShort(byte[] b, int o, short v, ByteOrder order) { if (order == ByteOrder.BIG_ENDIAN) { b[o] = (byte) (v >> 8); b[o + 1] = (byte) v; } else { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); } }
}
