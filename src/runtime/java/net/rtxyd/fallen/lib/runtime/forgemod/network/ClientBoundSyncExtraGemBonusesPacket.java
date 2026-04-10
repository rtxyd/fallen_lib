package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClientBoundSyncExtraGemBonusesPacket extends AbstractRegistryBoundPacketPayLoad<ExtraGemBonusRegistry.ExtraGemBonus> {

    public static final String version = "1.0";
    public static final IVanillaLikeCustomPacketPayload.Type<ClientBoundSyncExtraGemBonusesPacket> TYPE = new IVanillaLikeCustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FallenLib.MODID, "egb_cl"));

    @Override
    public @NotNull IVanillaLikeCustomPacketPayload.Type<? extends IVanillaLikeCustomPacketPayload> type() {
        return TYPE;
    }

    public static final FriendlyByteBufCodec<ClientBoundSyncExtraGemBonusesPacket> BUF_CODEC =
            createByteBufCodec(ExtraGemBonusRegistry.ExtraGemBonus.CODEC, ClientBoundSyncExtraGemBonusesPacket::new);

    public ClientBoundSyncExtraGemBonusesPacket(ResourceLocation path, ExtraGemBonusRegistry.ExtraGemBonus extraGemBonus) {
        super(path, extraGemBonus);
    }

    public static class Begin implements AbstractRegistryBoundPacketPayLoad.IBegin<ClientBoundSyncExtraGemBonusesPacket> {
        @Override
        public Class<ClientBoundSyncExtraGemBonusesPacket> getProcessClass() {
            return ClientBoundSyncExtraGemBonusesPacket.class;
        }
    }

    public static class End implements AbstractRegistryBoundPacketPayLoad.IEnd<ClientBoundSyncExtraGemBonusesPacket> {
        @Override
        public Class<ClientBoundSyncExtraGemBonusesPacket> getProcessClass() {
            return ClientBoundSyncExtraGemBonusesPacket.class;
        }
    }
}
