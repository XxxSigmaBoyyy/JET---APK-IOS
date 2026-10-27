import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Build-time patch of dex2jar v2.4's dex-reader jar (Apache-2.0) for on-device per-class translation speed:
 * DexFileReader.getString(int) decodes MUTF-8 from the string_data section on every call (type names, method/field
 * names and descriptors are looked up thousands of times per class). Memoize it in a String[] indexed by string id.
 * The original method is kept as jet$getString0; the reader's ByteBuffer state is unchanged.
 *
 * usage: java -cp asm.jar:. D2jPatch in.jar out.jar
 */
public final class D2jPatch {
    static final String READER = "com/googlecode/d2j/reader/DexFileReader.class";

    public static void main(String[] a) throws IOException {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
        boolean patched = false;
        try (ZipInputStream zi = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zo = new ZipOutputStream(Files.newOutputStream(tmp))) {
            for (ZipEntry e; (e = zi.getNextEntry()) != null; ) {
                byte[] b = zi.readAllBytes();
                if (e.getName().equals(READER)) { b = patch(b); patched = true; }
                ZipEntry ne = new ZipEntry(e.getName()); ne.setTime(e.getTime());
                zo.putNextEntry(ne); zo.write(b); zo.closeEntry();
            }
        }
        if (!patched) { Files.deleteIfExists(tmp); throw new IOException("DexFileReader not found in " + in); }
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("d2jpatch: " + out + " (DexFileReader.getString memoized)");
    }

    static byte[] patch(byte[] b) {
        ClassReader cr = new ClassReader(b);
        // COMPUTE_MAXS only: untouched methods are copied verbatim (original stack map frames); the one new method
        // below declares its own frames.
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        final boolean[] found = {false};
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                if (name.equals("getString") && desc.equals("(I)Ljava/lang/String;")) {
                    found[0] = true;
                    return super.visitMethod(Opcodes.ACC_PRIVATE, "jet$getString0", desc, sig, ex);
                }
                return super.visitMethod(acc, name, desc, sig, ex);
            }
            @Override public void visitEnd() {
                if (!found[0]) throw new IllegalStateException("DexFileReader.getString(I) not found");
                super.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE, "jet$str", "[Ljava/lang/String;", null, null).visitEnd();
                String owner = "com/googlecode/d2j/reader/DexFileReader";
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE, "getString", "(I)Ljava/lang/String;", null, null);
                mv.visitCode();
                Label notNeg = new Label(), have = new Label(), hit = new Label();
                // if (i == -1) return null;   (same as the original's NO_INDEX check)
                mv.visitVarInsn(Opcodes.ILOAD, 1); mv.visitInsn(Opcodes.ICONST_M1); mv.visitJumpInsn(Opcodes.IF_ICMPNE, notNeg);
                mv.visitInsn(Opcodes.ACONST_NULL); mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(notNeg);
                Object[] l2 = {owner, Opcodes.INTEGER}, l3 = {owner, Opcodes.INTEGER, "[Ljava/lang/String;"},
                         l4 = {owner, Opcodes.INTEGER, "[Ljava/lang/String;", "java/lang/String"};
                mv.visitFrame(Opcodes.F_NEW, 2, l2, 0, null);
                // String[] c = jet$str; if (c == null) jet$str = c = new String[stringIdIn.capacity() / 4];
                mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitFieldInsn(Opcodes.GETFIELD, owner, "jet$str", "[Ljava/lang/String;");
                mv.visitVarInsn(Opcodes.ASTORE, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 2); mv.visitJumpInsn(Opcodes.IFNONNULL, have);
                mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitFieldInsn(Opcodes.GETFIELD, owner, "stringIdIn", "Ljava/nio/ByteBuffer;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "capacity", "()I", false);
                mv.visitInsn(Opcodes.ICONST_4); mv.visitInsn(Opcodes.IDIV);
                mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String"); mv.visitVarInsn(Opcodes.ASTORE, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitVarInsn(Opcodes.ALOAD, 2); mv.visitFieldInsn(Opcodes.PUTFIELD, owner, "jet$str", "[Ljava/lang/String;");
                mv.visitLabel(have);
                mv.visitFrame(Opcodes.F_NEW, 3, l3, 0, null);
                // if (i < 0 || i >= c.length) return jet$getString0(i);
                Label inRange = new Label(), slow = new Label();
                mv.visitVarInsn(Opcodes.ILOAD, 1); mv.visitJumpInsn(Opcodes.IFLT, slow);
                mv.visitVarInsn(Opcodes.ILOAD, 1); mv.visitVarInsn(Opcodes.ALOAD, 2); mv.visitInsn(Opcodes.ARRAYLENGTH);
                mv.visitJumpInsn(Opcodes.IF_ICMPLT, inRange);
                mv.visitLabel(slow);
                mv.visitFrame(Opcodes.F_NEW, 3, l3, 0, null);
                mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "jet$getString0", "(I)Ljava/lang/String;", false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(inRange);
                mv.visitFrame(Opcodes.F_NEW, 3, l3, 0, null);
                // String s = c[i]; if (s != null) return s; s = jet$getString0(i); c[i] = s; return s;
                mv.visitVarInsn(Opcodes.ALOAD, 2); mv.visitVarInsn(Opcodes.ILOAD, 1); mv.visitInsn(Opcodes.AALOAD);
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 3); mv.visitJumpInsn(Opcodes.IFNONNULL, hit);
                mv.visitVarInsn(Opcodes.ALOAD, 0); mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "jet$getString0", "(I)Ljava/lang/String;", false);
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 2); mv.visitVarInsn(Opcodes.ILOAD, 1); mv.visitVarInsn(Opcodes.ALOAD, 3); mv.visitInsn(Opcodes.AASTORE);
                mv.visitLabel(hit);
                mv.visitFrame(Opcodes.F_NEW, 4, l4, 0, null);
                mv.visitVarInsn(Opcodes.ALOAD, 3); mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0); mv.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }
}
