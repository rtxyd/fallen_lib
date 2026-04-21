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

public abstract class AbstractPacketBoundRegistry<E extends ICodecProvider<E>,
        PB extends AbstractRegistryBoundPacketPayload.IBegin<P>,
        P extends AbstractRegistryBoundPacketPayload<E>,
        PE extends AbstractRegistryBoundPacketPayload.IEnd<P>>
        extends SimpleJsonResourceReloadListener implements IPacketBoundRegistry<E>, ICodecProvider<E> {

    protected final String path;
    private final Logger logger;
    protected ICondition.IContext context;
    protected Predicate<ResourceLocation> locFilter;
    protected final String type;
    protected final boolean doSync;
    protected final boolean useTypeIdAsKey;

    protected BiMap<ResourceLocation, E> registry = ImmutableBiMap.of();
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends AbstractPacketBoundRegistry>, AbstractPacketBoundRegistry> SINGLETONS = new HashMap<>();

    private final BiMap<ResourceLocation, Codec<? extends E>> CODEC_MAP = HashBiMap.create();
    private Codec<E> fallbackCodec = null;

    protected final Map<ResourceLocation, E> temp = new HashMap<>();
    Constructors3<E, PB, P, PE> packetConstructors;
    private Codec<E> singletonBoundCodec;

    public AbstractPacketBoundRegistry(Logger logger, String path, String type, Predicate<ResourceLocation> locFilter, boolean doSync, boolean useTypeIdAsKey) {
        super(new Gson(), path);
        this.path = path;
        this.logger = logger;
        this.type = type;
        this.doSync = doSync;
        this.locFilter = locFilter;
        this.useTypeIdAsKey = useTypeIdAsKey;
        this.initSingletonBoundCodec();
        this.registerBuiltinCodecs();
    }

    final void initPacketsConstructors(Constructors3<E, PB, P, PE> constructors) {
        if (this.packetConstructors == null) {
            this.validateConstructors(constructors);
            this.packetConstructors = constructors;
        }
    }

    private final void validateConstructors(Constructors3<E, PB, P, PE> constructors) {
        if (!(constructors.beginConstructor() != null && constructors.processConstructor() != null && constructors.endConstructor() != null)) {
            throw new RuntimeException("Registry[" + this.getClass() + "]: Invalid packet constructors!");
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
        Codec<E> codec = (Codec<E>) SINGLETONS.get(this.getClass()).getCodec();
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
                        E deserialized = codec.decode(JsonOps.INSTANCE, obj).getOrThrow(false, s -> {}).getFirst();
                        if (useTypeIdAsKey) {
                            Optional<JsonElement> id = JsonOps.INSTANCE.get(obj, type).resultOrPartial(str -> {});
                            Optional<ResourceLocation> keyTypeId = id.map(t -> ResourceLocation.CODEC.decode(JsonOps.INSTANCE, t).resultOrPartial(logger::error).get().getFirst());
                            this.register(keyTypeId.get(), deserialized);
                        } else {
                            this.register(key, deserialized);
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

    private final void register(ResourceLocation loc, E item) {
        if (this.registry.containsKey(loc)) throw new UnsupportedOperationException("Duplicated id: [" + loc + "]");
        registerTempEntry(loc, item);
        this.registry.put(loc, item);
    }

    public boolean validate() {
        if (this.packetConstructors == null && this.doSync) {
            throw new UnsupportedOperationException("Registry [" + this.getClass() +  "] is intended to do sync, but bound constructors are not initialized!");
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

    public final void registerCodec(ResourceLocation loc, Codec<? extends E> codec) {
        CODEC_MAP.put(loc, codec);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    // placebo way codec
    final Codec<E> initSingletonBoundCodec() {
        if (this.singletonBoundCodec == null) {
            this.singletonBoundCodec = new Codec<>() {
                @Override
                public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                    Optional<T> id = ops.get(input, type).resultOrPartial(str -> {});
                    Optional<ResourceLocation> key = id.map(t -> ResourceLocation.CODEC.decode(ops, t).resultOrPartial(logger::error).get().getFirst());

                    Codec codec = key.<Codec>map(CODEC_MAP::get).orElse(fallbackCodec);

                    if (codec == null) {
                        return DataResult.error(() -> "Failed parsing [" + id.get() + "] Obj: " + input);
                    }
                    return codec.decode(ops, input);
                }

                @Override
                public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                    Codec<E> codec = (Codec<E>) input.getCodec();
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
    protected final void registerFallbackCodec(Codec<E> fallback) {
        this.fallbackCodec = fallback;
    }

    // serverside
    @Override
    public final void syncClient(OnDatapackSyncEvent e) {
        if (packetConstructors == null) {
            throw new UnsupportedOperationException("Registry[" + this.getClass() + "] packet constructors are not initialized!");
        }
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
    public final void registerTempEntry(ResourceLocation loc, E item) {
        this.validateItem(loc, item);
        this.temp.put(loc, item);
    }

    @Override
    public void validateItem(ResourceLocation loc, E item) {}

    @Override
    public final void handleBegin(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(this::beginSync);
    }

    @Override
    public final void handleProcess(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation path, E item) {
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
    public final Codec<E> getCodec() {
        return this.singletonBoundCodec;
    }

    public final Codec<E> getFallbackCodec() {
        return this.fallbackCodec;
    }
}
