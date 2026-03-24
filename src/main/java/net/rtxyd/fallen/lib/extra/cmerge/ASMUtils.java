package net.rtxyd.fallen.lib.extra.cmerge;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ASMUtils {
    public static ClassNode read(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    public static byte[] readClassBytes(String internalName) {
        try (InputStream in = ASMUtils.class.getClassLoader().getResourceAsStream(internalName + ".class")) {

            if (in == null) throw new RuntimeException("Class not found: " + internalName);

            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodNode cloneMethodWithOwnerRemap(MethodNode original, String fromOwner, String toOwner) {

        MethodNode copy = new MethodNode(
                original.access,
                original.name,
                original.desc,
                original.signature,
                original.exceptions.toArray(new String[0])
        );

        Map<String, String> map = Map.of(fromOwner, toOwner);

        SimpleRemapper remapper = new SimpleRemapper(map);

        MethodRemapper methodRemapper = new MethodRemapper(copy, remapper);

        original.accept(methodRemapper);

        return copy;
    }

    // from mixin
    private static final int[] OPCODE_BLACKLIST = {
            Opcodes.RETURN, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
            Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE,
            Opcodes.ASTORE, Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE,
            Opcodes.SASTORE
    };

    private boolean isThisField(ClassNode template, FieldInsnNode fieldInsn) {
        return template.fields.stream().anyMatch(fn -> fn.name.equals(fieldInsn.name) && fn.desc.equals(fieldInsn.desc));
    }

    boolean isSuperInit(AbstractInsnNode insn, ClassNode currentClass) {

        if (!(insn instanceof MethodInsnNode methodInsn)) {
            return false;
        }

        if (methodInsn.getOpcode() != Opcodes.INVOKESPECIAL) {
            return false;
        }

        if (!methodInsn.name.equals("<init>")) {
            return false;
        }

        if (methodInsn.owner.equals(currentClass.name)) {
            return true;
        }

        if (methodInsn.owner.equals(currentClass.superName) || methodInsn.owner.equals("java/lang/Object")) {
            return true;
        }

        return false;
    }
}
