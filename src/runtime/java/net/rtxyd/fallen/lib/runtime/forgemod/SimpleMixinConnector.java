package net.rtxyd.fallen.lib.runtime.forgemod;

import net.rtxyd.fallen.lib.extra.mixin.FallenMixinConnectorRegistry;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.util.ArrayList;
import java.util.List;

public class SimpleMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        boolean isApothExist = getClass().getClassLoader().getResource("dev/shadowsoffire/apotheosis/Apotheosis.class") != null;
        if (isApothExist) {
            Mixins.addConfiguration(FallenLib.MODID + ".mixins.json");
        }
        try {
            connectRegistry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connectRegistry() {
        List<IMixinConnector> connectors = new ArrayList<>();

        FallenMixinConnectorRegistry.forEach(cl -> {
            try {
                Class<?> clz = getClass().getClassLoader().loadClass(cl);
                connectors.add((IMixinConnector) clz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                FallenLib.LOGGER.error("Can't load class {}", cl);
            }
        });
        for (IMixinConnector connector : connectors) {
            connector.connect();
        }
    }
}
