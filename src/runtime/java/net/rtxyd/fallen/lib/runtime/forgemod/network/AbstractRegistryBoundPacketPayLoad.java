package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM> implements IVanillaLikeCustomPacketPayload {
    private final ResourceLocation path;
    private final REGISTRY_ITEM registryItem;
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends AbstractRegistryBoundPacketPayLoad>, AbstractPacketBoundRegistry> REGISTRY_SINGLETONS = new HashMap<>();

    protected AbstractRegistryBoundPacketPayLoad(ResourceLocation path, REGISTRY_ITEM registryItem) {
        this.path = path;
        this.registryItem = registryItem;
    }

    @SuppressWarnings("unchecked")
    protected Codec<REGISTRY_ITEM> getBoundItemCodec() {
        var registry = getBoundRegistry(this.getClassAuto());
        if (registry == null) {
            FallenLib.LOGGER.error("{} is not bound!", this.getClass());
            throw new RuntimeException("Packet is not bound!");
        }
        return (Codec<REGISTRY_ITEM>) registry.getCodec();
    };

    public final ResourceLocation getPath() {
        return path;
    }
    public final REGISTRY_ITEM getItem() {
        return registryItem;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        getBoundRegistry(this.getClassAuto()).handleProcess(contextSupplier, this.getPath(), this.getItem());
    }

    @SuppressWarnings("unchecked")
    public final Class<? extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>> getClassAuto() {
        return (Class<? extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>>) this.getClass();
    }

    static <A, B extends AbstractRegistryBoundPacketPayLoad.IBegin<C>, C extends AbstractRegistryBoundPacketPayLoad<A>, D extends AbstractRegistryBoundPacketPayLoad.IEnd<C>>
    void boundRegistrySingleton(Class<? extends AbstractRegistryBoundPacketPayLoad<A>> packetClass, AbstractPacketBoundRegistry<A, B, C ,D> instance) {
        REGISTRY_SINGLETONS.computeIfAbsent(packetClass, k -> instance);
    }

    @SuppressWarnings("unchecked")
    public static <A, B extends AbstractRegistryBoundPacketPayLoad.IBegin<C>, C extends AbstractRegistryBoundPacketPayLoad<A>, D extends AbstractRegistryBoundPacketPayLoad.IEnd<C>>
    AbstractPacketBoundRegistry<A, B, C, D> getBoundRegistry(Class<? extends AbstractRegistryBoundPacketPayLoad<A>> registryClass) {
        return (AbstractPacketBoundRegistry<A, B, C, D>) REGISTRY_SINGLETONS.get(registryClass);
    }

    public static <ORIGIN extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>, REGISTRY_ITEM> FriendlyByteBufCodec<ORIGIN> createByteBufCodec(
            Codec<REGISTRY_ITEM> itemCodec,
            BiFunction<ResourceLocation, REGISTRY_ITEM , ORIGIN> constructor
    ) {
        return new FriendlyByteBufCodec<>() {
            @Override
            public void encode(@NotNull ORIGIN value, @NotNull FriendlyByteBuf buf) {
                buf.writeResourceLocation(value.getPath());
                buf.writeNbt((CompoundTag) itemCodec.encodeStart(NbtOps.INSTANCE, value.getItem())
                        .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing item for {}", value.getItem())));
            }

            @Override
            public @NotNull ORIGIN decode(@NotNull FriendlyByteBuf buf) {
                ResourceLocation path = buf.readResourceLocation();
                CompoundTag tag = buf.readNbt();
                var result = itemCodec.decode(NbtOps.INSTANCE, tag)
                        .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing received payload for {}", path)).getFirst();
                return constructor.apply(path, result);
            }
        };
    }
    @SuppressWarnings("rawtypes")
    public static interface IBegin<T extends AbstractRegistryBoundPacketPayLoad> {
        Class<T> getProcessClass();
        default void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            getBoundRegistry(ClientBoundSyncExtraGemBonusesPacket.class).handleBegin(contextSupplier);
        }
    }
    @SuppressWarnings("rawtypes")
    public static interface IEnd<T extends AbstractRegistryBoundPacketPayLoad> {
        Class<T> getProcessClass();
        default void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            getBoundRegistry(ClientBoundSyncExtraGemBonusesPacket.class).handleEnd(contextSupplier);
        }
    }
}
