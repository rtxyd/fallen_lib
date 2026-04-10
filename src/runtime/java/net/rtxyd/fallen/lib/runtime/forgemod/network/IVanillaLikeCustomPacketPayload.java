package net.rtxyd.fallen.lib.runtime.forgemod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IVanillaLikeCustomPacketPayload {
    @NotNull IVanillaLikeCustomPacketPayload.Type<? extends IVanillaLikeCustomPacketPayload> type();

    void handle(Supplier<NetworkEvent.Context> contextSupplier);

    public class Type<T> {
        private final ResourceLocation ID;
        public Type(ResourceLocation id) {
            this.ID = id;
        }
    }
}
