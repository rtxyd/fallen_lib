package net.rtxyd.fallen.lib.api;

import net.rtxyd.fallen.lib.api.annotation.FallenPatch;
import net.rtxyd.fallen.lib.type.service.IFallenPatchContext;

/**
 * Implementation must be annotated with {@link FallenPatch}.
 * Do only ASM operations in your implementations.
 * Do not define any static fields and use static block.
 * Do not reference in constructor with any {@link net.minecraft}, {@link net.minecraftforge}
 * Do not reference any target class.
 * Do not trigger any active class loading.
 * Do not create extra thread in your implementations.
 */
@FallenPatch
public interface IFallenPatch {
    static String fallenPatchDescriptor() {return "Lnet/rtxyd/fallen/lib/api/annotation/FallenPatch;";}
    static String fallenInserterDescriptor() {return "Lnet/rtxyd/fallen/lib/api/annotation/FallenInserter;";}
    void apply(IFallenPatchContext ctx);
}
