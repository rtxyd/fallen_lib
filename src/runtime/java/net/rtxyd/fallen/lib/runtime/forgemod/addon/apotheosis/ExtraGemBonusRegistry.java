package net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis;

import com.google.common.base.Predicates;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.network.AbstractPacketBoundRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.network.ClientBoundSyncExtraGemBonusesPacket;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;

import java.util.List;

public class ExtraGemBonusRegistry extends AbstractPacketBoundRegistry<ExtraGemBonusRegistry.ExtraGemBonus, ClientBoundSyncExtraGemBonusesPacket.Begin, ClientBoundSyncExtraGemBonusesPacket, ClientBoundSyncExtraGemBonusesPacket.End> {

    public static final ExtraGemBonusRegistry INSTANCE = new ExtraGemBonusRegistry();

    protected Multimap<DynamicHolder<Gem>, ExtraGemBonus> extraBonuses = HashMultimap.create();

    public ExtraGemBonusRegistry() {
        super(FallenLib.LOGGER, "extra_gem_bonuses", "type", Predicates.alwaysTrue(), true, false);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerCodec(ResourceLocation.fromNamespaceAndPath(FallenLib.MODID, "extra_gem_bonuses"), ExtraGemBonus.CODEC);
    }

    @Override
    public void beginReload() {
        this.registry = HashBiMap.create();
        this.extraBonuses = HashMultimap.create();
        this.clearExtraGemBonuses();
    }

    @Override
    public void onReload() {
        for (ExtraGemBonus extraBonus : registry.values()) {
            this.extraBonuses.put(extraBonus.gem, extraBonus);
        }
        this.applyExtraGemBonuses();
    }

    private void applyExtraGemBonuses() {
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(gem);

            for (ExtraGemBonus extraBonus : this.extraBonuses.get(holder)) {
                for (GemBonus bonus : extraBonus.bonuses()) {
                    try {
                        ((GemBonusExtension) gem).fallen_lib$appendExtraBonus(bonus);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void clearExtraGemBonuses() {
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            if (gem instanceof GemBonusExtension extension) {
                extension.fallen_lib$clearExtraBonuses();
            }
        }
    }

    public record ExtraGemBonus(DynamicHolder<Gem> gem,
                                List<GemBonus> bonuses) implements CodecProvider<ExtraGemBonus>, ICodecProvider<ExtraGemBonus> {

        public static final Codec<ExtraGemBonus> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        GemRegistry.INSTANCE.holderCodec().fieldOf("gem").forGetter(ExtraGemBonus::gem),
                        GemBonus.CODEC.listOf().fieldOf("bonuses").forGetter(ExtraGemBonus::bonuses))
                .apply(inst, ExtraGemBonus::new));

        @Override
        public Codec<? extends ExtraGemBonus> getCodec() {
            return CODEC;
        }
    }
}