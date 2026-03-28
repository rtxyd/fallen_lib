package net.rtxyd.fallen.lib.extra.mixin;

import net.rtxyd.fallen.lib.service.FallenBootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FallenMixinConnectorRegistry {
    private static final List<String> REGISTRY = new ArrayList<>();

    public static void register(String classReference) {
        if (FallenBootstrap.isInitialized()) return;
        REGISTRY.add(classReference);
    }

    public static void forEach(Consumer<String> consumer) {
        REGISTRY.forEach(consumer);
    }
}
