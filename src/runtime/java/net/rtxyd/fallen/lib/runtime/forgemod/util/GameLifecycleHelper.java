package net.rtxyd.fallen.lib.runtime.forgemod.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.minecraft.SlotOnTakeEvent;
import net.rtxyd.fallen.lib.runtime.forgemod.util.eventkey.EventKey;
import net.rtxyd.fallen.lib.runtime.forgemod.util.eventkey.EventKeys;
import net.rtxyd.fallen.lib.util.call.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class GameLifecycleHelper {
    public static IClientPlayerSupplier pSupplier = () -> null;
    private static int maxContainerMapSize = 0;

    private static final ContextKeyRegistry CTX_KEY_REG = new ContextKeyRegistry();
    private static final ThreadLocalCallBox CALL_BOX = new ThreadLocalCallBox();
    private static final Map<Container, Set<Player>> CONTAINER_PLAYER_MAP = new HashMap<>();
    private static final Map<Player, AbstractContainerMenu> PLAYER_MENU_SNAPSHOT = new HashMap<>();

    public static final ContextKey<AbstractContainerMenu> LAST_MENU = CTX_KEY_REG.register("fallen_lib.player.menu");
    public static final Consumer<Exception> EMPTY_EX_CONSUMER = o -> {};

    public static <T> ContextKey<T> registerContextKey(String id) {
        if (FallenLib.getStage() == FallenLib.Stage.COMPLETE) {
            throw new UnsupportedOperationException("ContextKey can only be registered on mod loading phase");
        }
        return CTX_KEY_REG.register(id);
    }

    public static <T> ContextKey<T> getContextKey(String id) {
        return CTX_KEY_REG.get(id);
    }

    public static <C, H> void submitCallback(EventKey<C, H> eventKey, C anchor, H handler) {
        eventKey.submit(anchor, handler);
    }

    public static Set<Player> getContainerActivePlayers(Container menu) {
        return CONTAINER_PLAYER_MAP.get(menu);
    }

    public static <T> void submitContextCall(ContextKey<T> key, Callable<T> call) {
        CALL_BOX.submit(key, call);
    }

    public static <T> T callIfPresent(ContextKey<T> key, Consumer<Exception> handleEx) {
        return CALL_BOX.getAndCallIfPresent(key, handleEx);
    }

    public static <T> T callAndRemoveIfPresent(ContextKey<T> key, Consumer<Exception> handleEx) {
        return CALL_BOX.takeAndCallIfPresent(key, handleEx);
    }

    public static Player getClientPlayer() {
        return pSupplier.player();
    }

    public static Optional<Level> safeGetClientLevel() {
        final AtomicReference<Player> localPlayer = new AtomicReference<>();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> {
                    localPlayer.setPlain(pSupplier.player());
                });

        if (localPlayer.getPlain() != null) {
            return Optional.of(localPlayer.getPlain().level());
        }
        return Optional.empty();
    }

    public static Level getClientLevel() {
        Player localPlayer = pSupplier.player();
        if (localPlayer == null) return null;
        return localPlayer.level();
    }

    @SubscribeEvent
    static void onServerTickStart(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            if (!CALL_BOX.isEmpty()) {
                CALL_BOX.clear();
            }
        } else {
            MinecraftServer server = e.getServer();
            if (server == null) return;
            if (server.getTickCount() % 300 != 0) return;
            int size = CONTAINER_PLAYER_MAP.size();
//            if (size > 400) {
//                FallenLib.LOGGER.warn("Container map unusually large: {}", size);
//            }
            if (size > maxContainerMapSize) {
                maxContainerMapSize = size;
                FallenLib.LOGGER.info("New max container map size: {}", size);
            }
        }
    }
    @SubscribeEvent
    static void onClientTickStart(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            if (!CALL_BOX.isEmpty()) {
                CALL_BOX.clear();
            }
        }
    }
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent e) {
        CALL_BOX.clear();
        EventKeys.clearAll();
        CONTAINER_PLAYER_MAP.clear();
        PLAYER_MENU_SNAPSHOT.clear();
    }
    @SubscribeEvent
    static void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent e) {
        removePlayerFromAllContainers(e.getEntity());
        PLAYER_MENU_SNAPSHOT.remove(e.getEntity());
    }
    static void removePlayerFromAllContainers(Player p) {
        for (Set<Player> set : CONTAINER_PLAYER_MAP.values()) {
            set.remove(p);
        }
        CONTAINER_PLAYER_MAP.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
    @SubscribeEvent
    static void onSlotTake(SlotOnTakeEvent e) {
        EventKeys.SLOT_ON_TAKE.fire(e.getSlot(), e.getPlayer(), e.getStack());
    }
    @SubscribeEvent
    static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        Player p = e.player;
        if (p.level().isClientSide) return;
//        if (p.tickCount % 3 != 0) return;
        if (e.phase == TickEvent.Phase.END) {
            AbstractContainerMenu menu = PLAYER_MENU_SNAPSHOT.get(p);
            if (menu == null) return;
            if (p.containerMenu == menu) {
                return;
            } else {
                if (p.containerMenu == p.inventoryMenu) {
                    updateContainerMap(menu, p);
                } else {
                    if (p.containerMenu != null && p.containerMenu.stillValid(p)) {
                        PLAYER_MENU_SNAPSHOT.put(p, p.containerMenu);
                        storeContainerMap(p);
                        updateContainerMap(menu, p);
                    }
                }
            }
        }
    }
    @SubscribeEvent
    static void onContainerOpen(PlayerContainerEvent.Open e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        AbstractContainerMenu menu = e.getContainer();
        PLAYER_MENU_SNAPSHOT.put(p, menu);
        if (menu != p.inventoryMenu) {
            storeContainerMap(p);
        }
    }
    @SubscribeEvent
    static void onContainerClose(PlayerContainerEvent.Close e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        AbstractContainerMenu menu = e.getContainer();
        PLAYER_MENU_SNAPSHOT.remove(p);
        if (menu != p.inventoryMenu) {
            updateContainerMap(e.getContainer(), e.getEntity());
        }
    }
    private static void storeContainerMap(Player p) {
        if (p.containerMenu == null) return;
        Container last = null;
        for (Slot slot : p.containerMenu.slots) {
            if (last != slot.container) {
                last = slot.container;
                CONTAINER_PLAYER_MAP.computeIfAbsent(last, c -> new HashSet<>()).add(p);
            }
        }
    }
    private static void updateContainerMap(AbstractContainerMenu menu, Player p) {
        if (p.containerMenu == null || menu == null) return;
        Container last = null;
        for (Slot slot : menu.slots) {
            if (last != slot.container) {
                last = slot.container;
                Set<Player> pSet = CONTAINER_PLAYER_MAP.get(last);
                if (pSet == null) continue;
                pSet.remove(p);
                if (pSet.isEmpty()) {
                    CONTAINER_PLAYER_MAP.remove(last);
                    EventKeys.SLOT_ON_TAKE.cleanup(last);
                }
            }
        }
    }
}
