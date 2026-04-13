package net.rtxyd.fallen.lib.runtime.forgemod.network;

import net.minecraft.resources.ResourceLocation;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import org.jetbrains.annotations.NotNull;

public class ClientBoundSyncExtraGemBonusesPacket extends AbstractRegistryBoundPacketPayload<ExtraGemBonusRegistry.ExtraGemBonus> {

    public static final String version = "1.0";
    public static final Type<ClientBoundSyncExtraGemBonusesPacket> TYPE = IVanillaLikeCustomPacketPayload.createType(FallenLib.MODID, "egb_cl");
    public static final FriendlyByteBufCodec<ClientBoundSyncExtraGemBonusesPacket> BUF_CODEC =
            createByteBufCodec(FallenLib.LOGGER, ExtraGemBonusRegistry.ExtraGemBonus.CODEC, ClientBoundSyncExtraGemBonusesPacket::new);

    @Override
    public @NotNull Type<ClientBoundSyncExtraGemBonusesPacket> type() {
        return TYPE;
    }

    public ClientBoundSyncExtraGemBonusesPacket(ResourceLocation path, ExtraGemBonusRegistry.ExtraGemBonus extraGemBonus) {
        super(path, extraGemBonus);
    }

    public static class Begin implements AbstractRegistryBoundPacketPayload.IBegin<ClientBoundSyncExtraGemBonusesPacket> {
        public static final Type<Begin> TYPE = IVanillaLikeCustomPacketPayload.createType(FallenLib.MODID, "egb_cl_begin");

        @Override
        public Class<ClientBoundSyncExtraGemBonusesPacket> getProcessClass() {
            return ClientBoundSyncExtraGemBonusesPacket.class;
        }

        @Override
        public @NotNull Type<Begin> type() {
            return TYPE;
        }
    }

    public static class End implements AbstractRegistryBoundPacketPayload.IEnd<ClientBoundSyncExtraGemBonusesPacket> {
        public static final Type<End> TYPE = IVanillaLikeCustomPacketPayload.createType(FallenLib.MODID, "egb_cl_end");

        @Override
        public Class<ClientBoundSyncExtraGemBonusesPacket> getProcessClass() {
            return ClientBoundSyncExtraGemBonusesPacket.class;
        }

        @Override
        public @NotNull Type<End> type() {
            return TYPE;
        }
    }
}
