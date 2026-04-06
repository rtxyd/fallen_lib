package net.rtxyd.fallen.lib.runtime.forgemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.compat.fga.FGAVersionStage;
import net.rtxyd.fallen.lib.runtime.forgemod.event.Common;
import net.rtxyd.fallen.lib.runtime.forgemod.network.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(FallenLib.MODID)
public class FallenLib {
    public static final String MODID = "fallen_lib";
    public static final Logger LOGGER = LogManager.getLogger("fallen_lib");
    public FallenLib(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        if (ModList.get().isLoaded("apotheosis")) {
            bus.addListener(this::init);
            if (SimpleMixinConnector.FGACheck == null
                    || !SimpleMixinConnector.FGACheck.getStage().equals(FGAVersionStage.FL_ONE_TWO)) {
                MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, Common::onAddReloadListeners);
            }
        }
    }

    public static ResourceLocation id(@NotNull String path) {
        return new ResourceLocation(MODID, path);
    }

    public void init(FMLCommonSetupEvent e) {
        Connection.register();
        e.enqueueWork(() -> {
        });
    }
}
