package net.rtxyd.fallen.lib.runtime.forgemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
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
        registerRegistryBoundPacketPayLoads(ExtraGemBonusRegistry.INSTANCE, ClientBoundSyncExtraGemBonusesPacket.BUF_CODEC,
                ClientBoundSyncExtraGemBonusesPacket.Begin.class,   ClientBoundSyncExtraGemBonusesPacket.Begin::new,    ClientBoundSyncExtraGemBonusesPacket.Begin::handle,
                ClientBoundSyncExtraGemBonusesPacket.class,         ClientBoundSyncExtraGemBonusesPacket::new,          ClientBoundSyncExtraGemBonusesPacket::handle,
                ClientBoundSyncExtraGemBonusesPacket.End.class,     ClientBoundSyncExtraGemBonusesPacket.End::new,      ClientBoundSyncExtraGemBonusesPacket.End::handle);
    }

    public static <REGISTRY_ITEM,
            BEGIN extends AbstractRegistryBoundPacketPayLoad.IBegin<PROCESS>,
            PROCESS extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>,
            END extends AbstractRegistryBoundPacketPayLoad.IEnd<PROCESS>,
            REGISTRY extends AbstractPacketBoundRegistry<REGISTRY_ITEM, BEGIN, PROCESS, END>>
    void registerRegistryBoundPacketPayLoads(REGISTRY singleton, FriendlyByteBufCodec<PROCESS> codec,
                                             Class<BEGIN> begin, Supplier<BEGIN> beginConstructor, BiConsumer<BEGIN, Supplier<NetworkEvent.Context>> beginHandler,
                                             Class<PROCESS> process, BiFunction<ResourceLocation, REGISTRY_ITEM, PROCESS> processConstructor, BiConsumer<PROCESS, Supplier<NetworkEvent.Context>> processHandler,
                                             Class<END> end, Supplier<END> endConstructor, BiConsumer<END, Supplier<NetworkEvent.Context>> endHandler) {
        if (INSTANCE == null) throw new RuntimeException("Fallen Lib Connection is not initialized!");

        INSTANCE.messageBuilder(begin, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(nullEncoderAuto())
                .decoder(nullDecoderAuto())
                .consumerMainThread(beginHandler).add();
        INSTANCE.messageBuilder(process, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(codec::encode)
                .decoder(codec::decode)
                .consumerMainThread(processHandler).add();
        INSTANCE.messageBuilder(end, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(nullEncoderAuto())
                .decoder(nullDecoderAuto())
                .consumerMainThread(endHandler).add();
        singleton.initConstructors(new IPacketBoundRegistry.Constructors3<>(beginConstructor, processConstructor, endConstructor));
        AbstractPacketBoundRegistry.registerSingleton(singleton);
        AbstractRegistryBoundPacketPayLoad.boundRegistrySingleton(process, singleton);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, singleton::onAddReloadListeners);
        singleton.registerSync();
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
