package net.rtxyd.fallen.lib.runtime.forgemod.mixin;

import net.minecraft.world.item.ItemStack;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ItemStackCakyGetter;
import net.rtxyd.fallen.lib.util.ObjectCaky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackCakyGetter {
    @Unique
    private volatile ObjectCaky fallen_lib$caky = null;

    @Unique
    @Override
    public <T> T fallen_lib$getObjectCaky(String id, ObjectCaky.CakyLoader<ItemStack, T> loader, ObjectCaky.CakyReviewer<ItemStack> reviewer) {
        if (fallen_lib$caky == null) {
            fallen_lib$caky = new ObjectCaky();
        }
        return fallen_lib$caky.resolve((ItemStack) (Object) this, id, loader, reviewer);
    }
}
