package net.rtxyd.fallen.lib.util.patch;

import net.rtxyd.fallen.lib.util.MiscUtil;
import net.rtxyd.fallen.lib.util.PatchUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Set;

import static net.rtxyd.fallen.lib.util.PatchUtil.*;

public final class StandardInserterHelper extends AbstractInserterHelper {

    private final boolean strictReturn;
    private boolean replaceReturn;
    private boolean debugMode;

    private Type[] callArgs;
    private Type callReturnType;
    private Type hookReturnType;
    private int callReturnTypeSort;
    private int hookReturnTypeSort;

    private boolean hookReturnVoid;
    private boolean callReturnVoid;
    private boolean callReturnPrimitive;
    private boolean ignoreReceiver;
    private boolean needCheckCast;

    private LabelNode startLabel;
    private LabelNode endLabel;

    private int localsMax;
    private int[] paramSlots;
    private int receiverSlot;
    private int returnSlot;
    private int contextFlags;

    private Type[] outerArgs;
    private boolean outerStatic;
    private int[] loadOuterArgsOrdinal;
    private Type outerReturnType;

    public StandardInserterHelper(MethodNode methodNode, MethodInsnNode exactTarget, MethodInsnNode hookMethod,
                                  int[] loadOuterArgsOrdinal, Set<PatchOption> options) {
        super(methodNode, exactTarget, hookMethod);
        this.replaceReturn = options.contains(PatchOption.REPLACE_RETURN);
        this.strictReturn = options.contains(PatchOption.STRICT_RETURN);
        this.debugMode = options.contains(PatchOption.DEBUG_MODE);
        this.loadOuterArgsOrdinal = loadOuterArgsOrdinal;
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
        InsnList after = computeAfterTargetMethod();
        // get here to avoid insert clearing list.
        AbstractInsnNode last = after.getLast();
        methodNode.instructions.insert(exactTarget, after);
        end();
        return last == null ? exactTarget : last;
    }

    @Override
    AbstractInsnNode runAndNavigateWithCache(InserterMeta meta, boolean insertBottom) {
        localsMax = meta.localsMaxCache;
        paramSlots = meta.paramsSlot;
        receiverSlot = meta.receiverSlot;
        returnSlot = meta.returnSlot;
        debugMode = meta.couldDebug && debugMode;

        InsnList after = new InsnList();
        if (meta.stackHasReturn) {
            after.add(new VarInsnNode(getStoreOpcode(callReturnType), returnSlot));
        }

        after.add(loadHookParamsAndInvoke());
        computeAfterHookMethod(after);
        // get here to avoid insert clearing list.
        AbstractInsnNode last = after.getLast();
        if (insertBottom) {
            // update newest node
            methodNode.instructions.insert(meta.last, after);
            meta.last = last;
            return last;
        }
        methodNode.instructions.insert(exactTarget, after);
        return meta.last;
    }

    private void initOuterInfo() {
        outerArgs = Type.getArgumentTypes(methodNode.desc);
        outerStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
        outerReturnType = Type.getReturnType(methodNode.desc);
        int maxOrdinal = outerStatic ? outerArgs.length : outerArgs.length + 1;
        for (int i : loadOuterArgsOrdinal) {
            if (i > maxOrdinal || i < 0)
                throw new UnsupportedOperationException("Outer arg ordinal ["+ i + "] out of bound! [" + outerArgs.length + "]" + MiscUtil.culpritString(hookMethod));
        }
    }

    private void init() {
        callArgs = Type.getArgumentTypes(exactTarget.desc);
        callReturnType = Type.getReturnType(exactTarget.desc);
        hookReturnType = Type.getReturnType(hookMethod.desc);
        contextFlags = 0;

        initOuterInfo();
        if (!callReturnType.getDescriptor().equals(hookReturnType.getDescriptor())) {
            if (strictReturn) {
                throw new UnsupportedOperationException(String.format("Strict return but return type mismatches! Expect: [ %s ]. %s", callReturnType.getDescriptor(), PatchUtil.culpritHookMethod(hookMethod)));
            }
            this.needCheckCast = true;
        }

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
            replaceReturn = false;
            contextFlags |= NInserterContext.F_RET_VOID;
        }

        if (ignoreReceiver) {
            contextFlags |= NInserterContext.F_INSTANCE;
        }

        // these two are related to hook
        if (hookReturnVoid) {
            if (replaceReturn) {
                throw new UnsupportedOperationException(String.format("Replace return value but inserter method returns void. %s", PatchUtil.culpritHookMethod(hookMethod)));
            }
        }
        if (replaceReturn) {
            contextFlags |= NInserterContext.F_REPLACE_RET;
        }
        if (strictReturn) {
            contextFlags |= NInserterContext.F_STRICT_RET;
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
        InsnList argInsns = new InsnList();
        if (debugMode) {
            argInsns.add(startLabel);
        }
        for (int i = callArgs.length - 1; i >= 0; i--) {
            argInsns.add(new VarInsnNode(getStoreOpcode(callArgs[i]), paramSlots[i]));
        }
        receiverSlot = localsMax;
        if (!ignoreReceiver) {
            // store object ref (this)
            argInsns.add(new VarInsnNode(Opcodes.ASTORE, receiverSlot));
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
            argInsns.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
            localsMax += 1;
        }
        for (int i = 0; i < callArgs.length; i++) {
            argInsns.add(new VarInsnNode(getLoadOpcode(callArgs[i]), paramSlots[i]));
        }
        return argInsns;
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
        if (!callReturnVoid) {
            if (callReturnPrimitive) {
                newInsns.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
                newInsns.add(boxType(callReturnType));
            } else {
                // object
                newInsns.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
            }
        } else {
            newInsns.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        newInsns.add(pushInt(contextFlags));
        newInsns.add(pushInt(loadOuterArgsOrdinal.length - 1));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/rtxyd/fallen/lib/util/patch/InserterContext", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;II)V"));
        // here is 1 stack remain
        return newInsns;
    }

    private void loadOuterArgs(InsnList invokeInsns, int offset) {
        for (int i = 0; i < loadOuterArgsOrdinal.length; i++) {
            int paramSlot = loadOuterArgsOrdinal[i];
            if (outerArgs.length == 0) break;
            int index = i + offset;
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
    }

    private InsnList loadHookParamsAndInvoke() {
        InsnList invokeInsns = new InsnList();
        invokeInsns.add(newCtx());
        invokeInsns.add(pushInt(callArgs.length + loadOuterArgsOrdinal.length));
        invokeInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        // load params to stack and invoke hook
        for (int i = 0; i < callArgs.length; i++) {
            Type callT = callArgs[i];
            int paramSlot = paramSlots[i];
            if (callT.getSort() != Type.OBJECT && callT.getSort() != Type.ARRAY) {
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(i));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(boxType(callT));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            } else {
                // object
                invokeInsns.add(new InsnNode(Opcodes.DUP));
                invokeInsns.add(pushInt(i));
                invokeInsns.add(new VarInsnNode(getLoadOpcode(callT), paramSlot));
                invokeInsns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        loadOuterArgs(invokeInsns, callArgs.length);
        /*
         *  invoke hook method
         *  ctx, arg1, arg2, arg3,..., argN
         */
        invokeInsns.add(hookMethod.clone(null));
        computeAfterHookMethod(invokeInsns);
        return invokeInsns;
    }

    private InsnList computeAfterTargetMethod() {
        // createInserterContext
        InsnList hookInsns = new InsnList();
        returnSlot = localsMax;
        if (!callReturnVoid) {
            hookInsns.add(new VarInsnNode(getStoreOpcode(callReturnType), returnSlot));
            if (debugMode) {
                methodNode.localVariables.add(new LocalVariableNode(
                        "fallen$hookRet",
                        callReturnType.getDescriptor(),
                        null,
                        startLabel,
                        endLabel,
                        localsMax
                ));
            }
            localsMax += callReturnType.getSize();
        }

        hookInsns.add(loadHookParamsAndInvoke());
        computeAfterHookMethod(hookInsns);
        return hookInsns;
    }

    private void computeAfterHookMethod(InsnList retList) {
        // test here with hook returns x,
        // and target returns x,
        // and relaceReturn is false
        if (!replaceReturn) {
            if (!callReturnVoid && !hookReturnVoid) {
                PatchUtil.popValue(retList, hookReturnType.getDescriptor());
                retList.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
            }
            if (callReturnVoid && !hookReturnVoid) {
                PatchUtil.popValue(retList, hookReturnType.getDescriptor());
            }
            if (!callReturnVoid && hookReturnVoid) {
                retList.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
            }
        } else {
            // when callReturnVoid replaceReturn is false, but in case, I check these.
            // only when strict return is false,
            // when it's true, we don't need to check these.
            if (!strictReturn) {
                if (!hookReturnVoid) {
                    // only call this method when hook is not void.
                    PatchUtil.replaceWithTypeAdaptation(retList, hookReturnType.getDescriptor(), callReturnType.getDescriptor());
                }
                if (callReturnVoid && !hookReturnVoid) {
                    // callReturnVoid is checked in init, and if it's void, replace return is set to false,
                    // this is unreachable
                    PatchUtil.popValue(retList, hookReturnType.getDescriptor());
                }
                if (!callReturnVoid && hookReturnVoid) {
                    // this is unreachable,
                    // has thrown exception in init when hook return void but want to replace return.
                    retList.add(new VarInsnNode(getLoadOpcode(callReturnType), returnSlot));
                }
            }
        }
        if (debugMode) {
            retList.add(endLabel);
        }
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
