package net.rtxyd.fallen.lib.runtime.forgemod.util;

import org.jetbrains.annotations.NotNull;

public interface BiCodec<B, C> {

    void encode(@NotNull C value, @NotNull B buf);

    @NotNull C decode(@NotNull B buf);
}
