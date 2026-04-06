package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ByteBufCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClientBoundSyncExtraGemBonusesPacket implements IVanillaLikeCustomPacketPayload {

    public static final String version = "1.0";
    public static final IVanillaLikeCustomPacketPayload.Type<ClientBoundSyncExtraGemBonusesPacket> TYPE = new IVanillaLikeCustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FallenLib.MODID, "egb_cl"));
    private static ClientBoundSyncExtraGemBonusesPacket cachedLast;

    @Override
    public @NotNull IVanillaLikeCustomPacketPayload.Type<? extends IVanillaLikeCustomPacketPayload> type() {
        return TYPE;
    }

    private ResourceLocation path;
    private ExtraGemBonusRegistry.ExtraGemBonus extraGemBonus;

    public static final ByteBufCodec<FriendlyByteBuf, ClientBoundSyncExtraGemBonusesPacket> BUF_CODEC = new ByteBufCodec<>() {
        @Override
        public void encode(@NotNull ClientBoundSyncExtraGemBonusesPacket value, @NotNull FriendlyByteBuf buf) {
            buf.writeResourceLocation(value.path);
            Codec<ExtraGemBonusRegistry.ExtraGemBonus> codec = ExtraGemBonusRegistry.ExtraGemBonus.CODEC;
            if (codec.encodeStart(NbtOps.INSTANCE, value.extraGemBonus)
                    .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing extra gem bonuses for {}", value.extraGemBonus.gem().getId()))
            instanceof CompoundTag tag) {
                buf.writeNbt(tag);
            }
        }

        @Override
        public @NotNull ClientBoundSyncExtraGemBonusesPacket decode(@NotNull FriendlyByteBuf buf) {
            ResourceLocation path = buf.readResourceLocation();
            CompoundTag tag = buf.readNbt();
            ExtraGemBonusRegistry.ExtraGemBonus result = ExtraGemBonusRegistry.ExtraGemBonus.CODEC.decode(NbtOps.INSTANCE, tag)
                    .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing received extra gem bonuses for")).getFirst();
            return new ClientBoundSyncExtraGemBonusesPacket(path, result);
        }
    };

    public ClientBoundSyncExtraGemBonusesPacket(ResourceLocation path, ExtraGemBonusRegistry.ExtraGemBonus extraGemBonus) {
        this.path = path;
        this.extraGemBonus = extraGemBonus;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ExtraGemBonusRegistry.INSTANCE.handleEntry(contextSupplier, this.path, this.extraGemBonus);
    }

    public static class Begin {
        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ExtraGemBonusRegistry.INSTANCE.handleBegin(contextSupplier);
        }
    }

    public static class End {
        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ExtraGemBonusRegistry.INSTANCE.handleEnd(contextSupplier);
        }
    }
}
