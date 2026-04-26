package net.rtxyd.fallen.lib.runtime.forgemod.util.eventkey;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.rtxyd.fallen.lib.util.call.IHandler3;
import net.rtxyd.fallen.lib.util.call.TunnelCallbackBox3;

public class SlotOnTakeKey implements EventKey<Container, IHandler3<Slot, Player, ItemStack>> {

    private final TunnelCallbackBox3<Container, Slot, Player, ItemStack> box = new TunnelCallbackBox3<>();

    SlotOnTakeKey() {}

    public void submit(Container key, IHandler3<Slot, Player, ItemStack> handler) {
        box.submit(key, handler);
    }

    public void fire(Slot s, Player p, ItemStack i) {
        box.executeAll(s, p, i);
    }

    public void cleanup(Container key) {
        box.take(key);
    }

    public void clear() {
        box.clear();
    }
}
