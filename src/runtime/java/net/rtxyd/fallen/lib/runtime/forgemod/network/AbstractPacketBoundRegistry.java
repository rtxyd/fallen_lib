package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.rtxyd.fallen.lib.runtime.forgemod.FallenLib;
import net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.ExtraGemBonusRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;
import net.rtxyd.fallen.lib.runtime.forgemod.util.IPacketBoundRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.Serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractPacketBoundRegistry<REGISTRY_ITEM,
        BEGIN extends AbstractRegistryBoundPacketPayLoad.IBegin<PROCESS>,
        PROCESS extends AbstractRegistryBoundPacketPayLoad<REGISTRY_ITEM>,
        END extends AbstractRegistryBoundPacketPayLoad.IEnd<PROCESS>>
        extends SimpleJsonResourceReloadListener implements IPacketBoundRegistry<REGISTRY_ITEM>, ICodecProvider<REGISTRY_ITEM> {

    protected final String path;
    protected ICondition.IContext context;
    protected Predicate<ResourceLocation> locFilter = Predicates.alwaysTrue();

    protected BiMap<ResourceLocation, REGISTRY_ITEM> registry = HashBiMap.create();
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends AbstractPacketBoundRegistry>, AbstractPacketBoundRegistry> SINGLETONS = new HashMap<>();
    protected final Map<ResourceLocation, REGISTRY_ITEM> temp = new HashMap<>();
    Constructors3<REGISTRY_ITEM, BEGIN, PROCESS, END> constructors;

    public AbstractPacketBoundRegistry(Gson gson, String path) {
        super(gson, path);
        this.path = path;
    }

    public void setLocFilter(Predicate<ResourceLocation> locFilter) {
        this.locFilter = locFilter;
    }

    final void initConstructors(Constructors3<REGISTRY_ITEM, BEGIN, PROCESS, END> constructors) {
        if (this.constructors == null) {
            this.constructors = constructors;
        }
    }

    public final void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(this);
        if (this.context != null) return;
        this.context = event.getConditionContext();
    }

    final void registerSync() {
        MinecraftForge.EVENT_BUS.addListener(this::syncClient);
    }

    @Override
    @SuppressWarnings("unchecked")
    // placebo way to parse
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager provider, ProfilerFiller p_10795_) {
        if (!this.validate()) {
            return;
        }
        this.beginReload();
        Codec<REGISTRY_ITEM> codec = (Codec<REGISTRY_ITEM>) SINGLETONS.get(this.getClass()).getCodec();
        if (codec == null) {
            FallenLib.LOGGER.error("Codec is null in {}", path);
            return;
        }
        map.forEach((key, ele) -> {
            if (!locFilter.test(key)) return;
            try {
                JsonObject obj;
                if (Serialization.isNotNullOrIsJsonObj(ele, key, path, FallenLib.LOGGER)) {
                    obj = ele.getAsJsonObject();
                    if (Serialization.checkEmptyJsonObjAndLog(obj, key, this.path, FallenLib.LOGGER) && Serialization.checkConditions(obj, key, this.path, FallenLib.LOGGER, this.context)) {
                        REGISTRY_ITEM deserialized = codec.decode(JsonOps.INSTANCE, obj).getOrThrow(false, s -> {}).getFirst();
                        this.registry.put(key, deserialized);
                    }
                }
            } catch (Exception e) {
                FallenLib.LOGGER.error("Failed parsing {} file {}.", this.path, key);
                FallenLib.LOGGER.error("Underlying Exception: ", e);
            }
        });
        this.onReload();
    }

    private boolean validate() {
        if (this.constructors == null) {
            FallenLib.LOGGER.error("Bound constructors are not initialized for {}", path);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <A, B extends AbstractRegistryBoundPacketPayLoad.IBegin<C>, C extends AbstractRegistryBoundPacketPayLoad<A>, D extends AbstractRegistryBoundPacketPayLoad.IEnd<C>>
    AbstractPacketBoundRegistry<A, B, C, D> getSingleton(Class<? extends AbstractPacketBoundRegistry<A, B, C, D>> registryClass) {
        return (AbstractPacketBoundRegistry<A, B, C, D>) SINGLETONS.get(registryClass);
    }

    @SuppressWarnings("rawtypes")
    static void registerSingleton(AbstractPacketBoundRegistry instance) {
        SINGLETONS.computeIfAbsent(instance.getClass(), k -> instance);
    }

    // serverside
    @Override
    public void syncClient(OnDatapackSyncEvent e) {
        ServerPlayer player = e.getPlayer();
        PacketDistributor.PacketTarget target = player == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(() -> player);
        Connection.sendToTarget(target, constructors.beginConstructor().get());
        registry.forEach((path, item) -> {
            Connection.sendToTarget(target, constructors.processConstructor().apply(path, item));
        });
        Connection.sendToTarget(target, constructors.endConstructor().get());
    }

    @Override
    public void beginReload() {
        this.registry = HashBiMap.create();
    }

    @Override
    public void onReload() {
        this.registry = ImmutableBiMap.copyOf(this.registry);
    }

    @Override
    public void beginSync() {
        this.temp.clear();
    }

    @Override
    public void registerTempEntry(ResourceLocation loc, REGISTRY_ITEM item) {
        this.temp.put(loc, item);
    }

    @Override
    public void handleBegin(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(this::beginSync);
    }

    @Override
    public void handleProcess(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation path, REGISTRY_ITEM item) {
        contextSupplier.get().enqueueWork(() -> {
            this.registerTempEntry(path, item);
        });
    }

    @Override
    public void handleEnd(Supplier<NetworkEvent.Context> contextSupplier) {
        if (ServerLifecycleHooks.getCurrentServer() != null) return;
        contextSupplier.get().enqueueWork(() -> {
            this.beginReload();
            this.applyTemp();
            this.onReload();
        });
    }

    @Override
    public void applyTemp() {
        temp.forEach((k,v)->{
            registry.put(k,v);
        });
    }
}
