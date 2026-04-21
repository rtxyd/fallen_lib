package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class AbstractRegistryBoundPacketPayload<E extends ICodecProvider<E>> implements IVanillaLikeCustomPacketPayload {
    private final ResourceLocation path;
    private final E registryItem;
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends AbstractRegistryBoundPacketPayload>, AbstractPacketBoundRegistry> REGISTRY_SINGLETONS = new HashMap<>();

    protected AbstractRegistryBoundPacketPayload(ResourceLocation path, E registryItem) {
        this.path = path;
        this.registryItem = registryItem;
    }

    protected Codec<E> getBoundItemCodec() {
        var registry = getBoundRegistry(this.getClassAuto());
        if (registry == null) {
            throw new RuntimeException(String.format("Packet [%s] registry is not bound!", this.getClass()));
        }
        return  registry.getCodec();
    };

    public final ResourceLocation getPath() {
        return path;
    }
    public final E getItem() {
        return registryItem;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        getBoundRegistry(this.getClassAuto()).handleProcess(contextSupplier, this.getPath(), this.getItem());
    }

    @SuppressWarnings("unchecked")
    public final Class<? extends AbstractRegistryBoundPacketPayload<E>> getClassAuto() {
        return (Class<? extends AbstractRegistryBoundPacketPayload<E>>) this.getClass();
    }

    static <A extends ICodecProvider<A>, B extends AbstractRegistryBoundPacketPayload.IBegin<C>, C extends AbstractRegistryBoundPacketPayload<A>, D extends AbstractRegistryBoundPacketPayload.IEnd<C>>
    void boundRegistrySingleton(Class<? extends AbstractRegistryBoundPacketPayload<A>> packetClass, AbstractPacketBoundRegistry<A, B, C ,D> instance) {
        if (REGISTRY_SINGLETONS.putIfAbsent(packetClass, instance) != null) {
            throw new UnsupportedOperationException("Payload " + packetClass.getName() + " is already bound to a registry singleton!");
        }
    }

    @SuppressWarnings("unchecked")
    public static <A extends ICodecProvider<A>, B extends AbstractRegistryBoundPacketPayload.IBegin<C>, C extends AbstractRegistryBoundPacketPayload<A>, D extends AbstractRegistryBoundPacketPayload.IEnd<C>>
    AbstractPacketBoundRegistry<A, B, C, D> getBoundRegistry(Class<C> registryClass) {
        return (AbstractPacketBoundRegistry<A, B, C, D>) REGISTRY_SINGLETONS.get(registryClass);
    }

    public static <ORIGIN extends AbstractRegistryBoundPacketPayload<E>, E extends ICodecProvider<E>> FriendlyByteBufCodec<ORIGIN> createByteBufCodec(
            Logger logger,
            Codec<E> itemCodec,
            BiFunction<ResourceLocation, E , ORIGIN> constructor
    ) {
        return new FriendlyByteBufCodec<>() {
            @Override
            public void encode(@NotNull ORIGIN value, @NotNull FriendlyByteBuf buf) {
                buf.writeResourceLocation(value.getPath());
                buf.writeNbt((CompoundTag) itemCodec.encodeStart(NbtOps.INSTANCE, value.getItem())
                        .getOrThrow(false,s -> logger.error("Failed parsing item for {}", value.getItem())));
            }

            @Override
            public @NotNull ORIGIN decode(@NotNull FriendlyByteBuf buf) {
                ResourceLocation path = buf.readResourceLocation();
                CompoundTag tag = buf.readNbt();
                var result = itemCodec.decode(NbtOps.INSTANCE, tag)
                        .getOrThrow(false,s -> logger.error("Failed parsing received payload for {}", path)).getFirst();
                return constructor.apply(path, result);
            }
        };
    }

    public static interface IBegin<T extends AbstractRegistryBoundPacketPayload<?>> extends IVanillaLikeCustomPacketPayload {
        Class<T> getProcessClass();

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <T extends AbstractRegistryBoundPacketPayload<?>> Class<T> getProcessClassAuto(IBegin inst) {
            return (Class<T>) inst.getProcessClass();
        }

        @Override
        default void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            getBoundRegistry(getProcessClassAuto(this)).handleBegin(contextSupplier);
        }
    }

    public static interface IEnd<T extends AbstractRegistryBoundPacketPayload<?>> extends IVanillaLikeCustomPacketPayload {
        Class<T> getProcessClass();

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <T extends AbstractRegistryBoundPacketPayload<?>> Class<T> getProcessClassAuto(IEnd inst) {
            return (Class<T>) inst.getProcessClass();
        }

        @Override
        default void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            getBoundRegistry(getProcessClassAuto(this)).handleEnd(contextSupplier);
        }
    }
}
