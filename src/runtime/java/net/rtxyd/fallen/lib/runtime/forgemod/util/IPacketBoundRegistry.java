package net.rtxyd.fallen.lib.runtime.forgemod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.network.AbstractRegistryBoundPacketPayLoad;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface IPacketBoundRegistry<REGISTRY_ITEM> {

    void beginReload();

    void onReload();

    // serverside
    void syncClient(OnDatapackSyncEvent e);

    void beginSync();

    void registerTempEntry(ResourceLocation loc, REGISTRY_ITEM extraGemBonus);

    void handleBegin(Supplier<NetworkEvent.Context> contextSupplier);

    void handleProcess(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation path, REGISTRY_ITEM item);

    void handleEnd(Supplier<NetworkEvent.Context> contextSupplier);

    void applyTemp();

    public static record Constructors3<REGISTRY_ITEM,
    BEGIN extends AbstractRegistryBoundPacketPayLoad.IBegin<PROCESS>,
    PROCESS extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>,
    END extends AbstractRegistryBoundPacketPayLoad.IEnd<PROCESS>>(Supplier<BEGIN> beginConstructor, BiFunction<ResourceLocation, REGISTRY_ITEM, PROCESS> processConstructor, Supplier<END> endConstructor) {}
}
