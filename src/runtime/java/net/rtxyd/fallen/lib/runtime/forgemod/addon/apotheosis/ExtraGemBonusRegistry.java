package net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.json.JsonUtil;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
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
import net.rtxyd.fallen.lib.runtime.forgemod.network.ClientBoundSyncExtraGemBonusesPacket;
import net.rtxyd.fallen.lib.runtime.forgemod.network.Connection;
import net.rtxyd.fallen.lib.runtime.forgemod.util.ICodecProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ExtraGemBonusRegistry extends SimpleJsonResourceReloadListener {

    public static final ExtraGemBonusRegistry INSTANCE = new ExtraGemBonusRegistry("extra_gem_bonuses");

    protected final String path;

    protected ICondition.IContext context;

    protected BiMap<ResourceLocation, ExtraGemBonus> registry = HashBiMap.create();

    protected Map<ResourceLocation, ExtraGemBonus> temp = new HashMap<>();

    protected Multimap<DynamicHolder<Gem>, ExtraGemBonus> extraBonuses = HashMultimap.create();

    public ExtraGemBonusRegistry(String path) {
        super(new Gson(), path);
        this.path = path;
    }

    @Override
    // placebo way to parse
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager provider, ProfilerFiller p_10795_) {
        this.beginReload();
        map.forEach((key, ele) -> {
            try {
                if (JsonUtil.checkAndLogEmpty(ele, key, this.path, FallenLib.LOGGER) && JsonUtil.checkConditions(ele, key, this.path, FallenLib.LOGGER, this.context)) {
                    JsonObject obj = ele.getAsJsonObject();
                    ExtraGemBonus deserialized = ExtraGemBonus.CODEC.decode(JsonOps.INSTANCE, obj).getOrThrow(false, s -> {}).getFirst();
                    this.registry.put(key, deserialized);
                }
            }
            catch (Exception e) {
                FallenLib.LOGGER.error("Failed parsing {} file {}.", this.path, key);
                FallenLib.LOGGER.error("Underlying Exception: ", e);
            }
        });
        this.onReload();
    }

    public void beginReload() {
        this.registry = HashBiMap.create();
        this.extraBonuses = HashMultimap.create();
        this.clearExtraGemBonuses();
    }

    public void onReload() {
        for (ExtraGemBonus extraBonus : registry.values()) {
            this.extraBonuses.put(extraBonus.gem, extraBonus);
        }
        this.applyExtraGemBonuses();
    }

    public static void register(final AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
        if (INSTANCE.context != null) return;
        INSTANCE.context = event.getConditionContext();
        registerSync();
    }

    private static void registerSync() {
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::syncClient);
    }

    private void applyExtraGemBonuses() {
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(gem);

            for (ExtraGemBonus extraBonus : this.extraBonuses.get(holder)) {
                for (GemBonus bonus : extraBonus.bonuses()) {
                    try {
                        ((GemBonusExtension) gem).fallen_lib$appendExtraBonus(bonus);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void clearExtraGemBonuses() {
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            if (gem instanceof GemBonusExtension extension) {
                extension.fallen_lib$clearExtraBonuses();
            }
        }
    }

    // serverside
    private void syncClient(OnDatapackSyncEvent e) {
        ServerPlayer player = e.getPlayer();
        PacketDistributor.PacketTarget target = player == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(() -> player);
        Connection.sendToTarget(target, new ClientBoundSyncExtraGemBonusesPacket.Begin());
        registry.forEach((path, bonus) -> {
            Connection.sendToTarget(target, new ClientBoundSyncExtraGemBonusesPacket(path, bonus));
        });
        Connection.sendToTarget(target, new ClientBoundSyncExtraGemBonusesPacket.End());
    }

    private void beginSync() {
    }

    private void registerTempEntry(ResourceLocation loc, ExtraGemBonus extraGemBonus) {
        this.temp.put(loc, extraGemBonus);
    }

    public void handleBegin(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(this::beginSync);
    }

    public void handleEntry(Supplier<NetworkEvent.Context> contextSupplier, ResourceLocation loc, ExtraGemBonus extraGemBonus) {
        contextSupplier.get().enqueueWork(() -> {
            this.registerTempEntry(loc, extraGemBonus);
        });
    }

    public void handleEnd(Supplier<NetworkEvent.Context> contextSupplier) {
        if (ServerLifecycleHooks.getCurrentServer() != null) return;
        contextSupplier.get().enqueueWork(() -> {
            this.beginReload();
            this.applyTemp();
            this.onReload();
        });
    }

    private void applyTemp() {
        temp.forEach((k,v)->{
            registry.put(k,v);
        });
    }

    public record ExtraGemBonus(DynamicHolder<Gem> gem,
                                List<GemBonus> bonuses) implements CodecProvider<ExtraGemBonus>, ICodecProvider<ExtraGemBonus> {

        public static final Codec<ExtraGemBonus> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        GemRegistry.INSTANCE.holderCodec().fieldOf("gem").forGetter(ExtraGemBonus::gem),
                        GemBonus.CODEC.listOf().fieldOf("bonuses").forGetter(ExtraGemBonus::bonuses))
                .apply(inst, ExtraGemBonus::new));

        @Override
        public Codec<? extends ExtraGemBonus> getCodec() {
            return CODEC;
        }
    }
}