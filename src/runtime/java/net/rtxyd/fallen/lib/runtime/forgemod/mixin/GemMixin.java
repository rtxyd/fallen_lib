package net.rtxyd.fallen.lib.runtime.forgemod.mixin;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.GemBonusExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = Gem.class, remap = false)
public class GemMixin implements GemBonusExtension {

    @Final
    @Shadow
    protected transient java.util.Map<LootCategory, GemBonus> bonusMap;

    @Unique
    private final List<GemBonus> fallen_lib$extraBonuses = new ArrayList<>();

    @Unique
    @Override
    public void fallen_lib$appendExtraBonus(GemBonus bonus) {
        fallen_lib$validateBonus(bonus);
        this.fallen_lib$extraBonuses.add(bonus);

        for (LootCategory cat : bonus.getGemClass().types()) {
            this.bonusMap.put(cat, bonus);
        }
    }

    @Override
    public void fallen_lib$clearExtraBonuses() {
        this.fallen_lib$extraBonuses.clear();
    }

    @Unique
    private void fallen_lib$validateBonus(GemBonus bonus) {
        for (LootCategory category : bonus.getGemClass().types()) {
            if (this.bonusMap.containsKey(category)) {
                GemBonus conflict = this.bonusMap.get(category);
                if (!conflict.equals(bonus)) {
                    throw new IllegalArgumentException("Gem Bonus for class %s conflicts with existing bonus for class %s (categories overlap)"
                            .formatted(bonus.getGemClass().key(), conflict.getGemClass().key()));
                }
            }
        }
    }

    @Inject(method = "addInformation", at = @At("TAIL"), remap = false)
    private void fallen_lib$addExtraBonusesTooltips(ItemStack gem, LootRarity rarity, Consumer<Component> list, CallbackInfo ci) {
        for (GemBonus bonus : this.fallen_lib$extraBonuses) {
            if (!bonus.supports(rarity)) continue;

            Component modifyComp = bonus.getSocketBonusTooltip(gem, rarity);
            Component sum = Component.translatable("text.apotheosis.dot_prefix",
                    Component.translatable("%s: %s",
                            Component.translatable("gem_class." + bonus.getGemClass().key()),
                            modifyComp)).withStyle(ChatFormatting.GOLD);
            list.accept(sum);
        }
    }
}