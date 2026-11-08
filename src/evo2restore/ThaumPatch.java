package evo2restore;

import evo2restore.util.Hashes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Adds two null guards to Thaumaturge 1.1.0.
 *
 * Thaumaturge calls {@code CraftingRecipe.assemble(CraftingInput.EMPTY)} while indexing aspects and dereferences
 * the result. Cyclops Core's {@code RecipeCraftingShapedCustomOutput} returns null there, so the server crashes on
 * world load. https://github.com/Leclowndu93150/Thaumaturge/issues/617
 */
final class ThaumPatch {
    private static final String PKG = "com/leclowndu93150/thaumaturge/content/aspect/";
    private static final String CRAFTING = PKG + "CraftingAspectContributor.class";
    private static final String SPLIT = PKG + "SplitConservation.class";
    private static final Map<String, String> PATCHED_SHA1 = Map.of(
            CRAFTING, "69c02619820ba3e196d2a0cee4e493f8d84693f3",
            SPLIT, "254e46fb055e81f82ca8af53ccf4ebc08a4d3c4e");
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";

    private ThaumPatch() {}

    static byte[] patchJar(byte[] original) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(original.length);
        int patched = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(original));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                byte[] data = in.readAllBytes();
                if (entry.getName().equals(CRAFTING)) {
                    data = verified(entry.getName(), patchOutputOf(data));
                    patched++;
                } else if (entry.getName().equals(SPLIT)) {
                    data = verified(entry.getName(), patchUniformOf(data));
                    patched++;
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                zip.putNextEntry(copy);
                zip.write(data);
                zip.closeEntry();
            }
        }
        if (patched != 2) throw new IOException("expected 2 classes to patch, found " + patched);
        return out.toByteArray();
    }

    static boolean isPatched(byte[] jar) throws IOException {
        int matches = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(jar))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                String expected = PATCHED_SHA1.get(entry.getName());
                if (expected != null && expected.equals(Hashes.sha1(in.readAllBytes()))) matches++;
            }
        }
        return matches == PATCHED_SHA1.size();
    }

    private static byte[] verified(String name, byte[] data) throws IOException {
        String actual = Hashes.sha1(data);
        if (!actual.equals(PATCHED_SHA1.get(name))) {
            throw new IOException("patched " + name + " has SHA-1 " + actual + ", expected " + PATCHED_SHA1.get(name));
        }
        return data;
    }

    private static ClassWriter writer() {
        return new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            // Minecraft classes are not on the classpath, so frame computation cannot load them.
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
    }

    /** {@code return value;} becomes {@code if (value == null) return ItemStack.EMPTY; return value;}. */
    private static byte[] patchOutputOf(byte[] bytes) {
        return transform(bytes, "outputOf", mv -> new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode != Opcodes.ARETURN) {
                    super.visitInsn(opcode);
                    return;
                }
                Label ok = new Label();
                super.visitInsn(Opcodes.DUP);
                super.visitJumpInsn(Opcodes.IFNONNULL, ok);
                super.visitInsn(Opcodes.POP);
                super.visitFieldInsn(Opcodes.GETSTATIC, ITEM_STACK, "EMPTY", "L" + ITEM_STACK + ";");
                super.visitInsn(Opcodes.ARETURN);
                super.visitLabel(ok);
                super.visitInsn(Opcodes.ARETURN);
            }
        });
    }

    /** {@code output.isEmpty()} is preceded by {@code if (output == null) return Optional.empty();}. */
    private static byte[] patchUniformOf(byte[] bytes) {
        return transform(bytes, "uniformOf", mv -> new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                if (owner.equals(ITEM_STACK) && name.equals("isEmpty") && desc.equals("()Z")) {
                    Label ok = new Label();
                    super.visitInsn(Opcodes.DUP);
                    super.visitJumpInsn(Opcodes.IFNONNULL, ok);
                    super.visitInsn(Opcodes.POP);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
                    super.visitInsn(Opcodes.ARETURN);
                    super.visitLabel(ok);
                }
                super.visitMethodInsn(op, owner, name, desc, itf);
            }
        });
    }

    private static byte[] transform(
            byte[] bytes, String method, java.util.function.UnaryOperator<MethodVisitor> wrap) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = writer();
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                return name.equals(method) ? wrap.apply(mv) : mv;
            }
        }, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
}
