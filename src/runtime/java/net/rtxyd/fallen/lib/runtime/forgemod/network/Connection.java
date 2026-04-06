package net.rtxyd.fallen.lib.runtime.forgemod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;

public class Connection {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(FallenLib.MODID, "netwwk"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ClientBoundSyncExtraGemBonusesPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientBoundSyncExtraGemBonusesPacket.BUF_CODEC::decode)
                .encoder(ClientBoundSyncExtraGemBonusesPacket.BUF_CODEC::encode)
                .consumerMainThread(ClientBoundSyncExtraGemBonusesPacket::handle)
                .add();

        net.messageBuilder(ClientBoundSyncExtraGemBonusesPacket.Begin.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(t -> new ClientBoundSyncExtraGemBonusesPacket.Begin())
                .encoder((a ,b) -> {})
                .consumerNetworkThread(ClientBoundSyncExtraGemBonusesPacket.Begin::handle)
                .add();

        net.messageBuilder(ClientBoundSyncExtraGemBonusesPacket.End.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(t -> new ClientBoundSyncExtraGemBonusesPacket.End())
                .encoder((a ,b) -> {})
                .consumerNetworkThread(ClientBoundSyncExtraGemBonusesPacket.End::handle)
                .add();
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
