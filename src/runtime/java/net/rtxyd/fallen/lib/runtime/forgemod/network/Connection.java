package net.rtxyd.fallen.lib.runtime.forgemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;
import net.rtxyd.fallen.lib.runtime.forgemod.util.IPacketBoundRegistry;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Connection {
    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(FallenLib.MODID, "netwwk"))
            .networkProtocolVersion(() -> "1.0")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel(); ;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        registerRegistryBoundPacketPayloads(ExtraGemBonusRegistry.INSTANCE, ClientBoundSyncExtraGemBonusesPacket.BUF_CODEC,
                ClientBoundSyncExtraGemBonusesPacket.Begin.class,   ClientBoundSyncExtraGemBonusesPacket.Begin::new,    ClientBoundSyncExtraGemBonusesPacket.Begin::handle,
                ClientBoundSyncExtraGemBonusesPacket.class,         ClientBoundSyncExtraGemBonusesPacket::new,          ClientBoundSyncExtraGemBonusesPacket::handle,
                ClientBoundSyncExtraGemBonusesPacket.End.class,     ClientBoundSyncExtraGemBonusesPacket.End::new,      ClientBoundSyncExtraGemBonusesPacket.End::handle);
    }

    public static <I extends ICodecProvider<I>,
            PB extends AbstractRegistryBoundPacketPayload.IBegin<P>,
            P extends AbstractRegistryBoundPacketPayload<I>,
            PE extends AbstractRegistryBoundPacketPayload.IEnd<P>,
            R extends AbstractPacketBoundRegistry<I, PB, P, PE>>
    void registerRegistryBoundPacketPayloads(R singleton, FriendlyByteBufCodec<P> codec,
                                             Class<PB> begin, Supplier<PB> beginConstructor, BiConsumer<PB, Supplier<NetworkEvent.Context>> beginHandler,
                                             Class<P> process, BiFunction<ResourceLocation, I, P> processConstructor, BiConsumer<P, Supplier<NetworkEvent.Context>> processHandler,
                                             Class<PE> end, Supplier<PE> endConstructor, BiConsumer<PE, Supplier<NetworkEvent.Context>> endHandler) {
        if (INSTANCE == null) throw new RuntimeException("Fallen Lib Connection is not initialized!");

        singleton.registerCommon();

        INSTANCE.messageBuilder(begin, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(nullEncoderAuto())
                .decoder(t -> beginConstructor.get())
                .consumerMainThread(beginHandler).add();
        INSTANCE.messageBuilder(process, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(codec::encode)
                .decoder(codec::decode)
                .consumerMainThread(processHandler).add();
        INSTANCE.messageBuilder(end, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(nullEncoderAuto())
                .decoder(t -> endConstructor.get())
                .consumerMainThread(endHandler).add();
        singleton.initPacketsConstructors(new AbstractPacketBoundRegistry.Constructors3<>(beginConstructor, processConstructor, endConstructor));
        AbstractPacketBoundRegistry.registerSingleton(singleton);
        AbstractRegistryBoundPacketPayload.boundRegistrySingleton(process, singleton);
        singleton.registerSync();
    }

    public static <I, P extends AbstractSingleEntryPacketPayLoad<I>> void registerSingleEntryPacketPayload(
            Class<P> process, NetworkDirection direction,
            FriendlyByteBufCodec<P> codec, BiConsumer<P, Supplier<NetworkEvent.Context>> handler) {
        INSTANCE.messageBuilder(process, id(), direction)
                .encoder(codec::encode)
                .decoder(codec::decode)
                .consumerMainThread(handler).add();
    }

    public static final Function<FriendlyByteBuf, ?> nullDecoder = t -> null;
    public static final BiConsumer<?, FriendlyByteBuf> nullEncoder = (a, b) -> {};

    @SuppressWarnings("unchecked")
    public static <T> Function<FriendlyByteBuf, T> nullDecoderAuto() {
        return (Function<FriendlyByteBuf, T>) nullDecoder;
    }
    @SuppressWarnings("unchecked")
    public static <T> BiConsumer<T, FriendlyByteBuf> nullEncoderAuto() {
        return (BiConsumer<T, FriendlyByteBuf>) nullEncoder;
    }

    public static <MSG> void sendToPlayer(MSG data, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), data);
    }

    public static <MSG> void sendToAllPlayers(MSG data) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), data);
    }

    public static <MSG> void sendToTarget(PacketDistributor.PacketTarget target, MSG data) {
        INSTANCE.send(target, data);
    }
}
