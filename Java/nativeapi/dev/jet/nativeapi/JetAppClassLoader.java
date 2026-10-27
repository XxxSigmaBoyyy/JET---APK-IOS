package dev.jet.nativeapi;

import org.objectweb.asm.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * URLClassLoader for APK (dex2jar) classes that rewrites
 * {@code System.loadLibrary}/{@code System.load} to {@link Natives}.
 * Shared address space: pointers pass as-is into the partner interpreter.
 */
public final class JetAppClassLoader extends URLClassLoader {
    private static final String NATIVES = "dev/jet/nativeapi/Natives";

    public JetAppClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public static JetAppClassLoader fromPaths(String dexPath, String librarySearchPath, ClassLoader parent)
            throws MalformedURLException {
        if (librarySearchPath != null && !librarySearchPath.isEmpty()) {
            String cur = System.getProperty("jet.native.lib.dir", "");
            System.setProperty("jet.native.lib.dir",
                    cur.isEmpty() ? librarySearchPath : cur + ":" + librarySearchPath);
        }
        String[] parts = dexPath == null || dexPath.isEmpty() ? new String[0] : dexPath.split(File.pathSeparator);
        URL[] urls = new URL[parts.length];
        for (int i = 0; i < parts.length; i++) urls[i] = new File(parts[i]).toURI().toURL();
        return new JetAppClassLoader(urls, parent != null ? parent : ClassLoader.getSystemClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        try (InputStream in = super.getResourceAsStream(path)) {
            if (in == null) throw new ClassNotFoundException(name);
            byte[] raw = in.readAllBytes();
            byte[] out = rewriteLoads(raw);
            return defineClass(name, out, 0, out.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    public String findLibrary(String name) {
        String dirs = System.getProperty("jet.native.lib.dir", "");
        for (String dir : dirs.split("[:;]")) {
            if (dir.isEmpty()) continue;
            File a = new File(dir, "lib" + name + ".so");
            if (a.isFile()) return a.getAbsolutePath();
            File b = new File(new File(dir, "arm64-v8a"), "lib" + name + ".so");
            if (b.isFile()) return b.getAbsolutePath();
        }
        return null;
    }

    static byte[] rewriteLoads(byte[] raw) {
        // Cheap UTF8 scan: skip ASM if class never mentions System.load*
        if (!contains(raw, "java/lang/System") ||
                !(contains(raw, "loadLibrary") || contains(raw, "load"))) {
            return raw;
        }
        ClassReader cr = new ClassReader(raw);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        final int[] n = {0};
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int acc, String mn, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(acc, mn, desc, sig, ex)) {
                    @Override public void visitMethodInsn(int op, String owner, String m, String d, boolean itf) {
                        if (op == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner)) {
                            if ("loadLibrary".equals(m) && "(Ljava/lang/String;)V".equals(d)) {
                                // name -> Natives.loadLibrary(name); pop boolean
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, NATIVES, "loadLibrary",
                                        "(Ljava/lang/String;)Z", false);
                                super.visitInsn(Opcodes.POP);
                                n[0]++;
                                return;
                            }
                            if ("load".equals(m) && "(Ljava/lang/String;)V".equals(d)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, NATIVES, "load",
                                        "(Ljava/lang/String;)Z", false);
                                super.visitInsn(Opcodes.POP);
                                n[0]++;
                                return;
                            }
                        }
                        // Also Runtime.loadLibrary0 is package-private; apps use System.*.
                        super.visitMethodInsn(op, owner, m, d, itf);
                    }
                };
            }
        }, 0);
        return n[0] > 0 ? cw.toByteArray() : raw;
    }

    private static boolean contains(byte[] hay, String needle) {
        byte[] n = needle.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i + n.length <= hay.length; i++) {
            for (int j = 0; j < n.length; j++) if (hay[i + j] != n[j]) continue outer;
            return true;
        }
        return false;
    }
}
