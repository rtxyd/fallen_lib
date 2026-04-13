package net.rtxyd.fallen.lib.runtime.forgemod.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;
import net.rtxyd.fallen.lib.runtime.forgemod.util.IPacketBoundRegistry;
import net.rtxyd.fallen.lib.runtime.forgemod.util.Serialization;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractPacketBoundRegistry<REGISTRY_ITEM extends ICodecProvider<REGISTRY_ITEM>,
        BEGIN extends AbstractRegistryBoundPacketPayload.IBegin<PROCESS>,
        PROCESS extends AbstractRegistryBoundPacketPayload<REGISTRY_ITEM>,
        END extends AbstractRegistryBoundPacketPayload.IEnd<PROCESS>>
        extends SimpleJsonResourceReloadListener implements IPacketBoundRegistry<REGISTRY_ITEM>, ICodecProvider<REGISTRY_ITEM> {

    protected final String path;
    private final Logger logger;
    protected ICondition.IContext context;
    protected Predicate<ResourceLocation> locFilter;
    protected final String type;
    protected final boolean doSync;
    protected final boolean useTypeIdAsKey;

    protected BiMap<ResourceLocation, REGISTRY_ITEM> registry = ImmutableBiMap.of();
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends AbstractPacketBoundRegistry>, AbstractPacketBoundRegistry> SINGLETONS = new HashMap<>();

    private final BiMap<ResourceLocation, Codec<? extends REGISTRY_ITEM>> CODEC_MAP = HashBiMap.create();
    private Codec<REGISTRY_ITEM> fallbackCodec = null;

    protected final Map<ResourceLocation, REGISTRY_ITEM> temp = new HashMap<>();
    Constructors3<REGISTRY_ITEM, BEGIN, PROCESS, END> packetConstructors;
    private Codec<REGISTRY_ITEM> singletonBoundCodec;

    public AbstractPacketBoundRegistry(Logger logger, String path, String type, Predicate<ResourceLocation> locFilter, boolean doSync, boolean useTypeIdAsKey) {
        super(new Gson(), path);
        this.path = path;
        this.logger = logger;
        this.type = type;
        this.doSync = doSync;
        this.locFilter = locFilter;
        this.useTypeIdAsKey = useTypeIdAsKey;
        initSingletonBoundCodec();
    }

    final void initPacketsConstructors(Constructors3<REGISTRY_ITEM, BEGIN, PROCESS, END> constructors) {
        if (this.packetConstructors == null) {
            this.packetConstructors = constructors;
        }
    }

    public final void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(this);
        if (this.context != null) return;
        this.context = event.getConditionContext();
    }

    final void registerCommon() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, this::onAddReloadListeners);
    }

    final void registerSync() {
        MinecraftForge.EVENT_BUS.addListener(this::syncClient);
    }

    @Override
    @SuppressWarnings("unchecked")
    // placebo way to parse
    protected final void apply(Map<ResourceLocation, JsonElement> map, ResourceManager provider, ProfilerFiller p_10795_) {
        if (!this.validate()) {
            return;
        }
        this.beginReload();
        Codec<REGISTRY_ITEM> codec = (Codec<REGISTRY_ITEM>) SINGLETONS.get(this.getClass()).getCodec();
        if (codec == null) {
            logger.error("Codec is null or not registered in {}", path);
            return;
        }
        map.forEach((key, ele) -> {
            if (!locFilter.test(key)) return;
            try {
                JsonObject obj;
                if (Serialization.isNotNullOrIsJsonObj(ele, key, this.path, this.logger)) {
                    obj = ele.getAsJsonObject();
                    if (Serialization.checkEmptyJsonObjAndLog(obj, key, this.path, this.logger) && Serialization.checkConditions(obj, key, this.path, this.logger, this.context)) {
                        REGISTRY_ITEM deserialized = codec.decode(JsonOps.INSTANCE, obj).getOrThrow(false, s -> {}).getFirst();
                        if (useTypeIdAsKey) {
                            Optional<JsonElement> id = JsonOps.INSTANCE.get(obj, type).resultOrPartial(str -> {});
                            Optional<ResourceLocation> keyTypeId = id.map(t -> ResourceLocation.CODEC.decode(JsonOps.INSTANCE, t).resultOrPartial(logger::error).get().getFirst());
                            this.registry.put(keyTypeId.get(), deserialized);
                        } else {
                            this.registry.put(key, deserialized);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed parsing {} file {}.", this.path, key);
                logger.error("Underlying Exception: ", e);
            }
        });
        this.onReload();
    }

    public boolean validate() {
        if (this.packetConstructors == null && this.doSync) {
            logger.error("Registry [{}] is intended to do sync, but bound constructors are not initialized", path);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <A extends ICodecProvider<A>, B extends AbstractRegistryBoundPacketPayload.IBegin<C>, C extends AbstractRegistryBoundPacketPayload<A>, D extends AbstractRegistryBoundPacketPayload.IEnd<C>>
    AbstractPacketBoundRegistry<A, B, C, D> getSingleton(Class<? extends AbstractPacketBoundRegistry<A, B, C, D>> registryClass) {
        return (AbstractPacketBoundRegistry<A, B, C, D>) SINGLETONS.get(registryClass);
    }

    @SuppressWarnings("rawtypes")
    static void registerSingleton(AbstractPacketBoundRegistry instance) {
        SINGLETONS.computeIfAbsent(instance.getClass(), k -> instance);
    }

    public final void registerCodec(ResourceLocation loc, Codec<? extends REGISTRY_ITEM> codec) {
        CODEC_MAP.put(loc, codec);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    // placebo way codec
    final Codec<REGISTRY_ITEM> initSingletonBoundCodec() {
        if (this.singletonBoundCodec == null) {
            this.singletonBoundCodec = new Codec<>() {
                @Override
                public <T> DataResult<Pair<REGISTRY_ITEM, T>> decode(DynamicOps<T> ops, T input) {
                    Optional<T> id = ops.get(input, type).resultOrPartial(str -> {});
                    Optional<ResourceLocation> key = id.map(t -> ResourceLocation.CODEC.decode(ops, t).resultOrPartial(logger::error).get().getFirst());

                    Codec codec = key.<Codec>map(CODEC_MAP::get).orElse(fallbackCodec);

                    if (codec == null) {
                        return DataResult.error(() -> "Failed parsing [" + id.get() + "] Obj: " + input);
                    }
                    return codec.decode(ops, input);
                }

                @Override
                public <T> DataResult<T> encode(REGISTRY_ITEM input, DynamicOps<T> ops, T prefix) {
                    Codec<REGISTRY_ITEM> codec = (Codec<REGISTRY_ITEM>) input.getCodec();
                    ResourceLocation key = CODEC_MAP.inverse().get(codec);
                    if (key == null) {
                        return DataResult.error(() -> "Codec is not registered! Obj:" + input);
                    }
                    T resultKey = ResourceLocation.CODEC.encodeStart(ops, key).getOrThrow(false, logger::error);
                    T resultObj = codec.encode(input, ops, prefix).getOrThrow(false, logger::error);
                    return ops.mergeToMap(resultObj, ops.createString(type), resultKey);
                }
            };
        }
        return this.singletonBoundCodec;
    }

    protected abstract void registerBuiltinCodecs();
    protected final void registerFallbackCodec(Codec<REGISTRY_ITEM> fallback) {
        this.fallbackCodec = fallback;
    }

    // serverside
    @Override
    public final void syncClient(OnDatapackSyncEvent e) {
        ServerPlayer player = e.getPlayer();
        PacketDistributor.PacketTarget target = player == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(() -> player);
        Connection.sendToTarget(target, packetConstructors.beginConstructor().get());
        registry.forEach((path, item) -> {
            Connection.sendToTarget(target, packetConstructors.processConstructor().apply(path, item));
        });
        Connection.sendToTarget(target, packetConstructors.endConstructor().get());
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
    public final void beginSync() {
        this.temp.clear();
    }

    @Override
    public final void registerTempEntry(ResourceLocation loc, REGISTRY_ITEM item) {
        if (this.registry.containsKey(loc)) throw new UnsupportedOperationException("Duplicated id: [" + loc + "]");
        this.validateItem(loc, item);
        this.temp.put(loc, item);
    }

    @Override
    public void validateItem(ResourceLocation loc, REGISTRY_ITEM item) {}

    @Override
    public final void handleBegin(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(this::beginSync);
    }

    @Override
    public final void handleProcess(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation path, REGISTRY_ITEM item) {
        contextSupplier.get().enqueueWork(() -> {
            this.registerTempEntry(path, item);
        });
    }

    @Override
    public final void handleEnd(Supplier<NetworkEvent.Context> contextSupplier) {
        if (ServerLifecycleHooks.getCurrentServer() != null) return;
        contextSupplier.get().enqueueWork(() -> {
            this.beginReload();
            this.applyTemp();
            this.onReload();
        });
    }

    @Override
    public final void applyTemp() {
        temp.forEach((k,v)->{
            registry.put(k,v);
        });
    }

    @Override
    public final Codec<REGISTRY_ITEM> getCodec() {
        return this.singletonBoundCodec;
    }

    public final Codec<REGISTRY_ITEM> getFallbackCodec() {
        return this.fallbackCodec;
    }
}
