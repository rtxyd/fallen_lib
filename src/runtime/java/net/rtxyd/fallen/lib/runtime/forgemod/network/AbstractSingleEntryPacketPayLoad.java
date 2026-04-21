package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.util.FriendlyByteBufCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class AbstractSingleEntryPacketPayLoad<E> implements IVanillaLikeCustomPacketPayload{
    private final E entry;

    protected AbstractSingleEntryPacketPayLoad(E entry) {
        this.entry = entry;
    }

    public final E getEntry() {
        return entry;
    }

    protected abstract Codec<E> getBoundEntryCodec();

    public static <P extends AbstractSingleEntryPacketPayLoad<E>, E> FriendlyByteBufCodec<P> createByteBufCodec(
            Codec<E> entryCodec,
            Function<E, P> constructor
    ) {
        return new FriendlyByteBufCodec<>() {
            @Override
            public void encode(@NotNull P value, @NotNull FriendlyByteBuf buf) {
                buf.writeNbt((CompoundTag) entryCodec.encodeStart(NbtOps.INSTANCE, value.getEntry())
                        .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing item for {}", value.getEntry())));
            }

            @Override
            public @NotNull P decode(@NotNull FriendlyByteBuf buf) {
                CompoundTag tag = buf.readNbt();
                var result = entryCodec.decode(NbtOps.INSTANCE, tag)
                        .getOrThrow(false,s -> FallenLib.LOGGER.error("Failed parsing received payload for {}", this.getClass().getName())).getFirst();
                return constructor.apply(result);
            }
        };
    }
}
