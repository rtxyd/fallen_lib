package net.rtxyd.fallen.lib.runtime.forgemod.util.eventkey;

public interface EventKey<C, H> {
    void submit(C context, H handler);
}