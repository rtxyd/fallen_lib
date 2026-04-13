package net.rtxyd.fallen.lib.runtime.forgemod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.network.AbstractRegistryBoundPacketPayload;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface IPacketBoundRegistry<REGISTRY_ITEM extends ICodecProvider<REGISTRY_ITEM>> {

    void beginReload();

    void onReload();

    // serverside
    void syncClient(OnDatapackSyncEvent e);

    void beginSync();

    void registerTempEntry(ResourceLocation loc, REGISTRY_ITEM extraGemBonus);

    void validateItem(ResourceLocation loc, REGISTRY_ITEM item);

    void handleBegin(Supplier<NetworkEvent.Context> contextSupplier);

    void handleProcess(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation path, REGISTRY_ITEM item);

    void handleEnd(Supplier<NetworkEvent.Context> contextSupplier);

    void applyTemp();

    public static record Constructors3<REGISTRY_ITEM extends ICodecProvider<REGISTRY_ITEM>,
    BEGIN extends AbstractRegistryBoundPacketPayload.IBegin<PROCESS>,
    PROCESS extends AbstractRegistryBoundPacketPayload<REGISTRY_ITEM>,
    END extends AbstractRegistryBoundPacketPayload.IEnd<PROCESS>>(Supplier<BEGIN> beginConstructor, BiFunction<ResourceLocation, REGISTRY_ITEM, PROCESS> processConstructor, Supplier<END> endConstructor) {}
}
