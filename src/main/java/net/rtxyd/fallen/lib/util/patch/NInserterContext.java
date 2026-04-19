package net.rtxyd.fallen.lib.util.patch;

import net.rtxyd.fallen.lib.type.util.patch.IInserterContext;

public class NInserterContext<REC, RET> implements IInserterContext<REC, RET> {
    private final REC receiver;
    private final RET ret;
    private final int flags;
    private final int lastOuterArg;

    public NInserterContext(REC receiver, RET ret, int flags) {
        this.receiver = receiver;
        this.ret = ret;
        this.flags = flags;
        this.lastOuterArg = -1;
    }

    public NInserterContext(REC receiver, RET ret, int flags, int lastOuterArg) {
        this.receiver = receiver;
        this.ret = ret;
        this.flags = flags;
        this.lastOuterArg = lastOuterArg;
    }

    @Override
    public REC receiver() {
        return receiver;
    }

    @Override
    public RET ret() {
        return ret;
    }

    @Override
    public boolean isInstanceCall() {
        return (flags & F_INSTANCE) != 0;
    }
    @Override
    public boolean isStaticCall() {
        return (flags & F_INVOKESTATIC) != 0;
    }
    @Override
    public boolean isInvokeDynamic() {
        return (flags & F_INVOKEDYNAMIC) != 0;
    }
    @Override
    public boolean isVoidReturn() {
        return (flags & F_RET_VOID) != 0;
    }
    @Override
    public boolean isPrimitiveReturn() {
        return (flags & F_RET_PRIMITIVE) != 0;
    }
    @Override
    public boolean willReplaceReturn() {
        return (flags & F_REPLACE_RET) != 0;
    }
}
