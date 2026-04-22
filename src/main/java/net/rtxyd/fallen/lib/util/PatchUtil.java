package net.rtxyd.fallen.lib.util;

import net.rtxyd.fallen.lib.FallenCoreLib;
import net.rtxyd.fallen.lib.util.patch.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.*;

public class PatchUtil {
    public static final String STANDARD_METHOD = "public static Object hook(IInserterContext<Object, Object> ctx, Object... args)";

    public static ClassNode cloneClassNode(ClassNode original) {
        ClassWriter cw = new ClassWriter(0);
        original.accept(cw);
        byte[] bytes = cw.toByteArray();

        ClassReader cr = new ClassReader(bytes);
        ClassNode copy = new ClassNode();
        cr.accept(copy, 0);
        return copy;
    }

    public static boolean isCtor(MethodNode mn) {
        return mn.name.equals("<init>");
    }

    public static boolean isCleanRecordCtor(MethodNode mn, ClassNode cn) {
        if (!isRecord(cn) || !mn.name.equals("<init>")) {
            return false;
        }
        InsnList insns = mn.instructions;

        int fieldCount = cn.recordComponents.size();
        int counter = 0;
        boolean failed = false;

        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            int op = insn.getOpcode();
            // skip non-code opcode
            if (op < 0) continue;

            if (op == Opcodes.PUTFIELD) {
                if (counter >= fieldCount) {
                    failed = true;
                    break;
                }
                FieldInsnNode fi = (FieldInsnNode) insn;
                RecordComponentNode rc = cn.recordComponents.get(counter);
                if (!fi.name.equals(rc.name)) {
                    failed = true;
                    break;
                }
                counter++;
                continue;
            }
            if (op == Opcodes.RETURN) {
                break;
            }
        }

        return counter == fieldCount && !failed;
    }

    public static boolean isRecord(ClassNode cn) {
        return cn.recordComponents != null && !cn.recordComponents.isEmpty();
    }

    public static boolean isNotInnerClass(ClassNode cn) {
        return cn.nestHostClass == null;
    }

    public static MethodInsnNode methodToInsn(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String name = method.getName();
        String desc = Type.getMethodDescriptor(method);
        boolean isInterface = method.getDeclaringClass().isInterface();

        int opcode;
        if (Modifier.isStatic(method.getModifiers())) {
            opcode = Opcodes.INVOKESTATIC;
        } else if (isInterface) {
            opcode = Opcodes.INVOKEINTERFACE;
        } else if (method.getName().equals("<init>") || Modifier.isPrivate(method.getModifiers())) {
            opcode = Opcodes.INVOKESPECIAL;
        } else {
            opcode = Opcodes.INVOKEVIRTUAL;
        }

        return new MethodInsnNode(opcode, owner, name, desc, isInterface);
    }

    public static int getLoadOpcode(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return Opcodes.ILOAD;
            case Type.LONG: return Opcodes.LLOAD;
            case Type.FLOAT: return Opcodes.FLOAD;
            case Type.DOUBLE: return Opcodes.DLOAD;
            case Type.ARRAY:
            case Type.OBJECT: return Opcodes.ALOAD;
            default: throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static int getStoreOpcode(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return Opcodes.ISTORE;
            case Type.LONG: return Opcodes.LSTORE;
            case Type.FLOAT: return Opcodes.FSTORE;
            case Type.DOUBLE: return Opcodes.DSTORE;
            case Type.ARRAY:
            case Type.OBJECT: return Opcodes.ASTORE;
            default:
                throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static AbstractInsnNode boxType(Type t) {
        String owner, desc;
        switch (t.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                desc = "(Z)Ljava/lang/Boolean;";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                desc = "(B)Ljava/lang/Byte;";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                desc = "(C)Ljava/lang/Character;";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                desc = "(S)Ljava/lang/Short;";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                desc = "(I)Ljava/lang/Integer;";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                desc = "(J)Ljava/lang/Long;";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                desc = "(F)Ljava/lang/Float;";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                desc = "(D)Ljava/lang/Double;";
                break;
            default:
                throw new IllegalArgumentException("Cannot box type: " + t);
        }
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", desc, false);
    }

    public static AbstractInsnNode unboxType(Type t) {
        String owner, name, desc;
        switch (t.getSort()) {
            case Type.BOOLEAN:
                owner = "java/lang/Boolean";
                name = "booleanValue";
                desc = "()Z";
                break;
            case Type.BYTE:
                owner = "java/lang/Byte";
                name = "byteValue";
                desc = "()B";
                break;
            case Type.CHAR:
                owner = "java/lang/Character";
                name = "charValue";
                desc = "()C";
                break;
            case Type.SHORT:
                owner = "java/lang/Short";
                name = "shortValue";
                desc = "()S";
                break;
            case Type.INT:
                owner = "java/lang/Integer";
                name = "intValue";
                desc = "()I";
                break;
            case Type.LONG:
                owner = "java/lang/Long";
                name = "longValue";
                desc = "()J";
                break;
            case Type.FLOAT:
                owner = "java/lang/Float";
                name = "floatValue";
                desc = "()F";
                break;
            case Type.DOUBLE:
                owner = "java/lang/Double";
                name = "doubleValue";
                desc = "()D";
                break;
            default:
                throw new IllegalArgumentException("Cannot unbox type: " + t);
        }
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, false);
    }

    public static AbstractInsnNode getDefaultValueInsn(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: return new InsnNode(Opcodes.ICONST_0);
            case Type.LONG: return new InsnNode(Opcodes.LCONST_0);
            case Type.FLOAT: return new InsnNode(Opcodes.FCONST_0);
            case Type.DOUBLE: return new InsnNode(Opcodes.DCONST_0);
            case Type.ARRAY:
            case Type.OBJECT: return new InsnNode(Opcodes.ACONST_NULL);
            default: throw new IllegalArgumentException("Unknown type: " + t);
        }
    }

    public static void insertMethodHook(ClassNode classNode,
                                        MethodNode methodNode,
                                        Predicate<? super AbstractInsnNode> filter,
                                        InserterMethodData methodData) {
        var type = methodData.getType();
        switch (type) {
            case STANDARD -> insertStandardMethodHook(classNode, methodNode, filter::test, methodData);
//            case STANDARD_VOID -> insertStandardMethodHook(classNode, methodNode, filter::test, methodData, true);
//            case BEFORE_CANCELLABLE -> insertBeforeCancellableMethodHook(classNode, methodNode, filter::test, hookMethod, replaceReturn, debugMode);
            case BEFORE_MODIFY_ARG -> insertBeforeModifyArgMethodHook(classNode, methodNode, filter::test, methodData);
        }
    }

    private static boolean checkDesc(MethodInsnNode hookMethod, InserterType type) {
        if (!hookMethod.desc.startsWith(InserterType.standardStarter())) {
            FallenCoreLib.LOGGER.debug("Inserter {} is not standard form! Expect: (descriptor)\n {}", hookMethod.owner + "." + hookMethod.name + hookMethod.desc, type.getExpected());
            return false;
        }
        return true;
    }
    private static boolean checkOpcode(MethodInsnNode hookMethod) {
        if (hookMethod.getOpcode() != Opcodes.INVOKESTATIC) {
            FallenCoreLib.LOGGER.debug("Inserter {} is not standard form! Expect: (static)\n", hookMethod.owner + "." + hookMethod.name + hookMethod.desc);
            return false;
        }
        return true;
    }

    private static void insertBeforeModifyArgMethodHook(ClassNode classNode, MethodNode methodNode, Predicate<MethodInsnNode> filter, InserterMethodData methodData) {
        InsnList insns = methodNode.instructions;
        // to check if init, or if is after super ctor
        boolean shouldCheck = methodNode.name.equals("<init>");
        MethodInsnNode hookMethod = methodData.getInserterMethod();
        if (!checkDesc(hookMethod, InserterType.BEFORE_MODIFY_ARG)) return;
        if (!checkOpcode(hookMethod)) return;
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) continue;
            if (!filter.test(call)) continue;
            if (shouldCheck && call.owner.equals(classNode.superName)) {
                shouldCheck = false;
                continue;
            }
            insn = StablePatchUtilV2.insertMethodHookBeforeModifyArg(methodNode, call, methodData);
        }
    }

    private static void insertStandardMethodHook(ClassNode classNode,
                                        MethodNode methodNode,
                                        Predicate<MethodInsnNode> filter,
                                        InserterMethodData methodData) {
        InsnList insns = methodNode.instructions;
        // to check if init, or if is after super ctor
        MethodInsnNode hookMethod = methodData.getInserterMethod();
        boolean shouldCheck = methodNode.name.equals("<init>");
        if (!hookMethod.desc.startsWith(InserterType.standardStarter())) {
            FallenCoreLib.LOGGER.debug("Inserter {} is not standard form! Expect: \n {}", hookMethod.owner + "." + hookMethod.name, InserterType.Expected.EXPECTED_STANDARD);
            return;
        }
        if (!checkOpcode(hookMethod)) return;
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) continue;
            if (!filter.test(call)) continue;
            if (shouldCheck && call.owner.equals(classNode.superName)) {
                shouldCheck = false;
                continue;
            }
            insn = StablePatchUtilV2.insertMethodHookStandard(methodNode, call, methodData);
        }
    }

    public static AbstractInsnNode pushInt(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, v);
        } else {
            return new LdcInsnNode(v);
        }
    }

    public static AbstractInsnNode pushBoolean(boolean bool) {
        if (bool) {
            return new InsnNode(Opcodes.ICONST_1);
        } else {
            return new InsnNode(Opcodes.ICONST_0);
        }
    }

    public static boolean isStatic(MethodNode mn) {
        return (mn.access & Opcodes.ACC_STATIC) != 0;
    }

    public static List<AbstractInsnNode> findInsnInMethod(MethodNode mn, Predicate<AbstractInsnNode> ainP) {
        InsnList insns = mn.instructions;
        List<AbstractInsnNode> insnNodeList = new ArrayList<>();
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (ainP.test(insn)) {
                insnNodeList.add(insn);
                return insnNodeList;
            }
        }
        return insnNodeList;
    }

    public static List<MethodInsnNode> findMethodInsnInMethod(MethodNode mn, Predicate<MethodInsnNode> ainP) {
        InsnList insns = mn.instructions;
        List<MethodInsnNode> insnNodeList = new ArrayList<>();
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode min && ainP.test(min)) {
                insnNodeList.add(min);
                return insnNodeList;
            }
        }
        return insnNodeList;
    }

    /**
     * Call this method only if sourceDesc is not void
     * @param instructions list to insert
     * @param sourceDesc from
     * @param targetDesc to
     */
    public static void replaceWithTypeAdaptation(InsnList instructions,
                                                 String sourceDesc,
                                                 String targetDesc) {
        if (sourceDesc.equals(targetDesc)) {
            return;
        }

        InsnList converters = new InsnList();

        adaptType(converters, sourceDesc, targetDesc);

        instructions.add(converters);
    }

    private static void adaptType(InsnList list, String sourceDesc, String targetDesc) {
        if ("V".equals(targetDesc)) {
            popValue(list, sourceDesc);
            return;
        }

        boolean srcPrim = isPrimitive(sourceDesc);
        boolean tgtPrim = isPrimitive(targetDesc);

        if (!srcPrim && !tgtPrim) {
            if (!sourceDesc.equals(targetDesc)) {
                String internalName = targetDesc.substring(1, targetDesc.length() - 1);
                list.add(new TypeInsnNode(Opcodes.CHECKCAST, internalName));
            }
            return;
        }

        if (!srcPrim && tgtPrim) {
            unboxToPrimitive(list, targetDesc);
            return;
        }

        if (srcPrim && !tgtPrim) {
            boxPrimitive(list, sourceDesc, targetDesc);
            return;
        }

        convertPrimitive(list, sourceDesc, targetDesc);
    }

    public static boolean isPrimitive(String desc) {
        return desc.length() == 1;
    }

    public static boolean isPrimitiveA(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    public static void popValue(InsnList list, String desc) {
        if (isDoubleSlot(desc)) {
            list.add(new InsnNode(Opcodes.POP2));
        } else {
            list.add(new InsnNode(Opcodes.POP));
        }
    }

    public static boolean isDoubleSlot(String desc) {
        return "J".equals(desc) || "D".equals(desc);
    }

    public static void unboxToPrimitive(InsnList list, String targetDesc) {
        if (isNumericPrimitive(targetDesc)) {
            unboxNumberToPrimitive(list, targetDesc);
        } else {
            unboxExactWrapper(list, targetDesc);
        }
    }

    private static boolean isNumericPrimitive(String desc) {
        return "I".equals(desc) || "J".equals(desc) || "F".equals(desc) || "D".equals(desc)
                || "B".equals(desc) || "S".equals(desc);
    }

    private static void unboxNumberToPrimitive(InsnList list, String targetDesc) {
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));

        String methodName;
        String methodDesc;
        switch (targetDesc) {
            case "I":
                methodName = "intValue";
                methodDesc = "()I";
                break;
            case "J":
                methodName = "longValue";
                methodDesc = "()J";
                break;
            case "F":
                methodName = "floatValue";
                methodDesc = "()F";
                break;
            case "D":
                methodName = "doubleValue";
                methodDesc = "()D";
                break;
            case "B":
                // byte: intValue -> I2B
                methodName = "intValue";
                methodDesc = "()I";
                break;
            case "S":
                // short: intValue -> I2S
                methodName = "intValue";
                methodDesc = "()I";
                break;
            default:
                throw new IllegalArgumentException("Unsupported numeric primitive: " + targetDesc);
        }

        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", methodName, methodDesc, false));

        if ("B".equals(targetDesc)) {
            list.add(new InsnNode(Opcodes.I2B));
        } else if ("S".equals(targetDesc)) {
            list.add(new InsnNode(Opcodes.I2S));
        }
    }

    private static void unboxExactWrapper(InsnList list, String targetDesc) {
        String wrapper = getWrapperInternalName(targetDesc);
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, wrapper));

        String unboxMethod = getUnboxMethodName(targetDesc);
        String unboxDesc = "()" + targetDesc;
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, wrapper, unboxMethod, unboxDesc, false));
    }

    public static String getWrapperInternalName(String primitiveDesc) {
        switch (primitiveDesc) {
            case "Z": return "java/lang/Boolean";
            case "B": return "java/lang/Byte";
            case "C": return "java/lang/Character";
            case "S": return "java/lang/Short";
            case "I": return "java/lang/Integer";
            case "J": return "java/lang/Long";
            case "F": return "java/lang/Float";
            case "D": return "java/lang/Double";
            default: throw new IllegalArgumentException("Not primitive: " + primitiveDesc);
        }
    }

    public static String getUnboxMethodName(String primitiveDesc) {
        switch (primitiveDesc) {
            case "Z": return "booleanValue";
            case "B": return "byteValue";
            case "C": return "charValue";
            case "S": return "shortValue";
            case "I": return "intValue";
            case "J": return "longValue";
            case "F": return "floatValue";
            case "D": return "doubleValue";
            default: throw new IllegalArgumentException("Not primitive: " + primitiveDesc);
        }
    }

    public static void boxPrimitive(InsnList list, String sourceDesc, String targetDesc) {
        String wrapper = getWrapperInternalName(sourceDesc);
        String boxMethod = "valueOf";
        String boxDesc = "(" + sourceDesc + ")L" + wrapper + ";";

        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, wrapper, boxMethod, boxDesc, false));

        if (!targetDesc.equals("L" + wrapper + ";")) {
            if (targetDesc.equals("Ljava/lang/Number;") || targetDesc.equals("Ljava/lang/Object;")) {
            } else {
                String internal = targetDesc.substring(1, targetDesc.length() - 1);
                list.add(new TypeInsnNode(Opcodes.CHECKCAST, internal));
            }
        }
    }

    public static String culpritHookMethod(MethodInsnNode method) {
        return String.format("Culprit: [ %s ]", method.owner.replace("/", ".") + "." + method.name + method.desc);
    }

    public static void convertPrimitive(InsnList list, String from, String to) {
        if (from.equals(to)) return;

        int opcode = getConversionOpcode(from, to);
        if (opcode != -1) {
            list.add(new InsnNode(opcode));
        } else {
            throw new IllegalArgumentException("Cannot convert " + from + " to " + to);
        }
    }

    public static int getConversionOpcode(String from, String to) {
        switch (from + "->" + to) {
            // int
            case "I->F":
                return Opcodes.I2F;
            case "I->D":
                return Opcodes.I2D;
            case "I->J":
                return Opcodes.I2L;
            case "I->B":
                return Opcodes.I2B;
            case "I->C":
                return Opcodes.I2C;
            case "I->S":
                return Opcodes.I2S;
            // long
            case "J->F":
                return Opcodes.L2F;
            case "J->D":
                return Opcodes.L2D;
            case "J->I":
                return Opcodes.L2I;
            // float
            case "F->D":
                return Opcodes.F2D;
            case "F->I":
                return Opcodes.F2I;
            case "F->J":
                return Opcodes.F2L;
            // double
            case "D->F":
                return Opcodes.D2F;
            case "D->I":
                return Opcodes.D2I;
            case "D->J":
                return Opcodes.D2L;
            default:
                return -1;
        }
    }
}
