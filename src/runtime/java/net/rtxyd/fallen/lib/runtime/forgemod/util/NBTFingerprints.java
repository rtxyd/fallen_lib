package net.rtxyd.fallen.lib.runtime.forgemod.util;

import net.minecraft.world.item.ItemStack;
import net.rtxyd.fallen.lib.util.ObjectCaky;

public final class NBTFingerprints {

    public static ObjectCaky.CakyReviewer<ItemStack> fullNBT() {
        return stack -> {
            var tag = stack.getTag();
            return tag != null ? tag.hashCode() : Integer.MIN_VALUE;
        };
    }

    public static ObjectCaky.CakyReviewer<ItemStack> subTag(String key) {
        return stack -> {
            var tag = stack.getTagElement(key);
            return tag != null ? tag.hashCode() : Integer.MIN_VALUE;
        };
    }
}