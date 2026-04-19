package net.rtxyd.fallen.lib.util.patch;

import net.rtxyd.fallen.lib.util.MiscUtil;
import net.rtxyd.fallen.lib.util.PatchUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Set;

import static net.rtxyd.fallen.lib.util.PatchUtil.*;

public final class BeforeModifyArgInserterHelper extends AbstractInserterHelper {

    private final boolean strictReturn;
    private boolean replaceReturn;
    private boolean debugMode;

    private Type[] callArgs;
    private Type callReturnType;
    private Type hookReturnType;
    private int callReturnTypeSort;
    private int hookReturnTypeSort;

    private Type[] outerArgs;
    private boolean outerStatic;
    private Type outerReturnType;
    private Type targetType;
    private int[] loadOuterArgsOrdinal;
    private int modifyArgOrdinal;
    private boolean modifyReceiver;

    private boolean hookReturnVoid;
    private boolean callReturnVoid;
    private boolean callReturnPrimitive;
    private boolean ignoreReceiver;

    private LabelNode startLabel;
    private LabelNode endLabel;

    private int localsMax;
    private int[] paramSlots;
    private int receiverSlot;
    private int returnSlot;
    private int contextFlags;
    private boolean needCheckCast;
    private boolean ignoreModifyArg;


    public BeforeModifyArgInserterHelper(MethodNode methodNode, MethodInsnNode exactTarget, MethodInsnNode hookMethod,
                                         int[] loadOuterArgsOrdinal, int modifyArgOrdinal,
                                         Set<PatchOption> options) {
        super(methodNode, exactTarget, hookMethod);
        this.replaceReturn = options.contains(PatchOption.REPLACE_RETURN);
        this.strictReturn = options.contains(PatchOption.STRICT_RETURN);
        this.debugMode = options.contains(PatchOption.DEBUG_MODE);
        this.loadOuterArgsOrdinal = loadOuterArgsOrdinal;
        this.modifyArgOrdinal = modifyArgOrdinal;
        if (debugMode) {
            this.startLabel = new LabelNode();
            this.endLabel = new LabelNode();
        }
        init();
    }

    @Override
    AbstractInsnNode runAndNavigate() {
        start();
        computeParamSlots();
        InsnList before = computeBeforeInsns();
        methodNode.instructions.insertBefore(exactTarget, before);
        if (debugMode) {
            methodNode.instructions.insert(exactTarget, endLabel);
        }
        end();
        return exactTarget;
    }

    @Override
    AbstractInsnNode runAndNavigateWithCache(InserterMeta meta, boolean ignore) {
        localsMax = meta.localsMaxCache;
        paramSlots = meta.paramsSlot;
        receiverSlot = meta.receiverSlot;
        returnSlot = meta.returnSlot;
        debugMode = meta.couldDebug && debugMode;

        InsnList after = computeBeforeInsns();
        methodNode.instructions.insertBefore(exactTarget, after);
        if (debugMode) {
            methodNode.instructions.insert(exactTarget, endLabel);
        }
        return exactTarget;
    }

    private void init() {
        callArgs = Type.getArgumentTypes(exactTarget.desc);
        callReturnType = Type.getReturnType(exactTarget.desc);
        hookReturnType = Type.getReturnType(hookMethod.desc);

        outerArgs = Type.getArgumentTypes(methodNode.desc);
        outerStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
        outerReturnType = Type.getReturnType(methodNode.desc);
        int maxOuterOrdinal = outerStatic ? outerArgs.length : outerArgs.length + 1;

        if (modifyArgOrdinal == -1) {
            this.ignoreModifyArg = true;
        }
        if (modifyArgOrdinal < 0 && !ignoreModifyArg) {
            throw new UnsupportedOperationException("Modify arg ordinal can't be less than 0! " + MiscUtil.culpritString(hookMethod));
        }
        if (modifyArgOrdinal > callArgs.length) {
            throw new UnsupportedOperationException("Modify arg ordinal ["+ modifyArgOrdinal + "] out of bound [" + callArgs.length + "]! " + MiscUtil.culpritString(hookMethod));
        }
        for (int i : loadOuterArgsOrdinal) {
            if (i > maxOuterOrdinal || i < 0)
                throw new UnsupportedOperationException("Outer arg ordinal ["+ i + "] out of bound! [" + outerArgs.length + "]" + MiscUtil.culpritString(hookMethod));
        }
        if (!ignoreModifyArg) {
            if (modifyArgOrdinal == 0) {
                targetType = Type.getObjectType(exactTarget.owner);
                modifyReceiver = true;
            } else {
                modifyArgOrdinal--;
                targetType = callArgs[modifyArgOrdinal];
                modifyReceiver = false;
            }
            if (!targetType.getDescriptor().equals(hookReturnType.getDescriptor())) {
                if (strictReturn) {
                    throw new UnsupportedOperationException(String.format("Strict return but return type mismatches! Expect: [ %s ]. %s", callReturnType.getDescriptor(), PatchUtil.culpritHookMethod(hookMethod)));
                }
                this.needCheckCast = true;
            }
        }

        contextFlags = 0;

        callReturnTypeSort = callReturnType.getSort();
        hookReturnTypeSort = hookReturnType.getSort();

        hookReturnVoid = hookReturnTypeSort == Type.VOID;
        callReturnVoid = callReturnTypeSort == Type.VOID;
        callReturnPrimitive = callReturnTypeSort != Type.OBJECT && callReturnTypeSort != Type.ARRAY;
        int targetOp = exactTarget.getOpcode();

        ignoreReceiver = true;
        switch (targetOp) {
            case Opcodes.INVOKEINTERFACE -> {
                contextFlags |= NInserterContext.F_INVOKEINTERFACE;
                ignoreReceiver = false;
            }
            case Opcodes.INVOKEVIRTUAL -> {
                contextFlags |= NInserterContext.F_INVOKEVIRTUAL;
                ignoreReceiver = false;
            }
            case Opcodes.INVOKESTATIC -> {
                contextFlags |= NInserterContext.F_INVOKESTATIC;
            }
            case Opcodes.INVOKEDYNAMIC -> {
                contextFlags |= NInserterContext.F_INVOKEDYNAMIC;
            }
            case Opcodes.INVOKESPECIAL -> {
                contextFlags |= NInserterContext.F_INVOKESPECIAL;
            }
        }

        if (callReturnPrimitive) {
            contextFlags |= NInserterContext.F_RET_PRIMITIVE;
        }
        if (callReturnVoid) {
            contextFlags |= NInserterContext.F_RET_VOID;
        }

        if (ignoreReceiver) {
            contextFlags |= NInserterContext.F_INSTANCE;
        }

        // these two are related to hook
        if (hookReturnVoid || hookReturnTypeSort != callReturnTypeSort) {
            replaceReturn = false;
        }
        if (replaceReturn) {
            contextFlags |= NInserterContext.F_REPLACE_RET;
        }
    }

    private void start() {
        localsMax = methodNode.maxLocals;
        paramSlots = new int[callArgs.length];
    }

    private void computeParamSlots() {
        // local slots and name if debug
        for (int i = 0; i < callArgs.length; i++) {
            // calc the stack slot of params
            paramSlots[i] = localsMax;
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookP$" + i,
                        callArgs[i].getDescriptor(),
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            localsMax += callArgs[i].getSize();
        }
    }

    private InsnList computeBeforeInsns() {
        InsnList list = new InsnList();
        if (debugMode) {
            list.add(startLabel);
        }
        for (int i = callArgs.length - 1; i >= 0; i--) {
            list.add(new VarInsnNode(getStoreOpcode(callArgs[i]), paramSlots[i]));
        }
        receiverSlot = localsMax;

        if (!ignoreReceiver) {
            // store object ref (this)
            list.add(new VarInsnNode(Opcodes.ASTORE, receiverSlot));
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookThs",
                        "Ljava/lang/util/Object;",
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            list.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
            localsMax += 1;
            if (modifyReceiver) {
                computeModifyArg(list, Opcodes.ASTORE, receiverSlot);
            } else {
                computeModifyArg(list, getStoreOpcode(targetType), paramSlots[modifyArgOrdinal]);
            }
        } else {
            computeModifyArg(list, getStoreOpcode(targetType), paramSlots[modifyArgOrdinal]);
        }

        for (int i = 0; i < callArgs.length; i++) {
            int opcode = getLoadOpcode(callArgs[i]);
            list.add(new VarInsnNode(opcode, paramSlots[i]));
        }
        return list;
    }

    private void computeModifyArg(InsnList list, int opcode, int modifySlot) {
        list.add(loadHookParamsAndInvoke());
        if (!hookReturnVoid) {
            if (ignoreModifyArg) {
                PatchUtil.popValue(list, hookReturnType.getDescriptor());
            } else {
                if (!strictReturn) {
                    PatchUtil.replaceWithTypeAdaptation(list, hookReturnType.getDescriptor(), targetType.getDescriptor());
                }
                list.add(new VarInsnNode(opcode, modifySlot));
            }
        }
    }

    private InsnList newCtx() {
        InsnList newInsns = new InsnList();
        newInsns.add(new TypeInsnNode(Opcodes.NEW, "net/rtxyd/fallen/lib/util/patch/InserterContext"));
        newInsns.add(new InsnNode(Opcodes.DUP));
        if (ignoreReceiver) {
            newInsns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            newInsns.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
        }
        newInsns.add(new InsnNode(Opcodes.ACONST_NULL));
        newInsns.add(pushInt(contextFlags));
        // last outer args index
        newInsns.add(pushInt(loadOuterArgsOrdinal.length - 1));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/rtxyd/fallen/lib/util/patch/InserterContext", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;II)V"));
        // here is 1 stack remain
        return newInsns;
    }

    private void loadOuterArgsStatic(InsnList invokeInsns, int paramSlot, int index) {
        Type callT = outerArgs[paramSlot];
        if (callT.getSort() != Type.OBJECT && callT.getSort() != Type.ARRAY) {
            invokeInsns.add(new InsnNode(Opcodes.DUP));
            invokeInsns.add(pushInt(index));
            invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
            invokeInsns.add(boxType(callT));
            invokeInsns.add(new InsnNode(Opcodes.AASTORE));
        } else {
            // object
            invokeInsns.add(new InsnNode(Opcodes.DUP));
            invokeInsns.add(pushInt(index));
            invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
            invokeInsns.add(new InsnNode(Opcodes.AASTORE));
        }
    }

    private void loadOuterArgsInstance(InsnList invokeInsns, int paramSlot, int index) {
        if (paramSlot == 0) {
            invokeInsns.add(new InsnNode(Opcodes.DUP));
            invokeInsns.add(pushInt(index));
            invokeInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            return;
        } else {
            paramSlot -= 1;
        }
        Type callT = outerArgs[paramSlot];
        if (callT.getSort() != Type.OBJECT && callT.getSort() != Type.ARRAY) {
            invokeInsns.add(new InsnNode(Opcodes.DUP));
            invokeInsns.add(pushInt(index));
            invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot + 1));
            invokeInsns.add(boxType(callT));
            invokeInsns.add(new InsnNode(Opcodes.AASTORE));
        } else {
            // object
            invokeInsns.add(new InsnNode(Opcodes.DUP));
            invokeInsns.add(pushInt(index));
            invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot + 1));
            invokeInsns.add(new InsnNode(Opcodes.AASTORE));
        }
    }

    private InsnList loadHookParamsAndInvoke() {
        InsnList invokeInsns = new InsnList();
        // 1
        invokeInsns.add(newCtx());
        // 2
        invokeInsns.add(pushInt(callArgs.length + loadOuterArgsOrdinal.length));
        invokeInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        // load params to stack and invoke hook
        if (!(outerStatic && outerArgs.length == 0)) {
            for (int i = 0; i < loadOuterArgsOrdinal.length; i++) {
                int paramSlot = loadOuterArgsOrdinal[i];
                if (outerStatic) {
                    loadOuterArgsStatic(invokeInsns, paramSlot, i);
                } else {
                    loadOuterArgsInstance(invokeInsns, paramSlot, i);
                }
            }
        }

        int offset = loadOuterArgsOrdinal.length;
        for (int i = 0; i < callArgs.length; i++) {
            Type callT = callArgs[i];
            int paramSlot = paramSlots[i];
            int index = i + offset;
            if (callT.getSort() != Type.OBJECT && callT.getSort() != Type.ARRAY) {
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(index));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(boxType(callT));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            } else {
                // object
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(index));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        /*
         *  invoke hook method
         *  ctx, arg1, arg2, arg3,..., argN
         */
        // consume 2
        invokeInsns.add(hookMethod.clone(null));
        return invokeInsns;
    }

    private void end() {
        methodNode.maxLocals = localsMax;
    }

    int[] getParamSlots() {
        return paramSlots;
    }

    int getReceiverSlot() {
        return receiverSlot;
    }

    int getReturnSlot() {
        return returnSlot;
    }

    boolean stackHasReturn() {
        return !callReturnVoid;
    }
}

