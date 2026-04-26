package net.rtxyd.fallen.lib.runtime.forgemod.util.eventkey;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.rtxyd.fallen.lib.util.call.IHandler3;
import net.rtxyd.fallen.lib.util.call.TunnelCallbackBox3;

public final class EventKeys {
    public static final SlotOnTakeKey SLOT_ON_TAKE = new SlotOnTakeKey();

    public static void clearAll() {
        SLOT_ON_TAKE.clear();
    }
}
