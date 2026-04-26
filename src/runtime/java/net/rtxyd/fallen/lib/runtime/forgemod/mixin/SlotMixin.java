package net.rtxyd.fallen.lib.runtime.forgemod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.minecraft.SlotOnTakeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Slot.class, targets = {"net.minecraft.world.inventory.ItemCombinerMenu$2"})
public class SlotMixin {
    @Inject(method = "onTake", at = @At(value = "HEAD"))
    public void hookOnTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        MinecraftForge.EVENT_BUS.post(new SlotOnTakeEvent((Slot)(Object)this, player, stack));
    }
}
