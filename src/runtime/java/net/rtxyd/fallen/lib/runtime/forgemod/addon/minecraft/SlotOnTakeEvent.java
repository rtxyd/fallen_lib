package net.rtxyd.fallen.lib.runtime.forgemod.addon.minecraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class SlotOnTakeEvent extends Event {
    private final Slot slot;
    private final Player player;
    private final ItemStack stack;

    public SlotOnTakeEvent(Slot slot, Player player, ItemStack stack) {
        this.slot = slot;
        this.player = player;
        this.stack = stack;
    }

    public Slot getSlot() {
        return slot;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getStack() {
        return stack;
    }
}
