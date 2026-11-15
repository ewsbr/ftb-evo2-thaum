package evo2restore.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Rewrites methods of classes in mod jars. */
public final class Asm {
    private Asm() {}

    /** Wraps the visitor of every method named {@code method} in {@code bytes}. */
    public static byte[] transform(byte[] bytes, String method, UnaryOperator<MethodVisitor> wrap) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            // Minecraft classes are not on the classpath, so frame computation cannot load them.
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                return name.equals(method) ? wrap.apply(mv) : mv;
            }
        }, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    /** The jar entry {@code name}, or null. */
    public static byte[] entry(byte[] jar, String name) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                if (e.getName().equals(name)) return in.readAllBytes();
            }
        }
        return null;
    }

    /**
     * Copies {@code jar}, replacing each entry in {@code patches} by the result of its function and appending
     * {@code added} entries that the jar lacks.
     */
    public static byte[] rewriteJar(byte[] jar, Map<String, UnaryOperator<byte[]>> patches, Map<String, byte[]> added)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(jar.length + 4096);
        java.util.Set<String> seen = new java.util.HashSet<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                byte[] data = in.readAllBytes();
                UnaryOperator<byte[]> patch = patches.get(entry.getName());
                if (patch != null) data = patch.apply(data);
                if (added.containsKey(entry.getName())) data = added.get(entry.getName());
                seen.add(entry.getName());
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                zip.putNextEntry(copy);
                zip.write(data);
                zip.closeEntry();
            }
            for (Map.Entry<String, byte[]> a : added.entrySet()) {
                if (seen.contains(a.getKey())) continue;
                ZipEntry e = new ZipEntry(a.getKey());
                e.setTime(0);
                zip.putNextEntry(e);
                zip.write(a.getValue());
                zip.closeEntry();
            }
        }
        if (!seen.containsAll(patches.keySet())) throw new IOException("jar lacks " + patches.keySet());
        return out.toByteArray();
    }
}
