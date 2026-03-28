package net.rtxyd.fallen.lib.service;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import net.rtxyd.fallen.lib.extra.cmerge.FallenClassMergeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class FallenBootstrap implements ITransformationService {
    static boolean initialized;
    static boolean isDevEnvironment;
    private final FallenRegistryBox box = new FallenRegistryBox();
    static final Logger LOGGER = LoggerFactory.getLogger("fallen.bootstrap");
    @Override
    public String name() {
        return "fallen";
    }

    @Override
    public void initialize(IEnvironment environment) {
        if (initialized) {
            throw new IllegalStateException("Already initialized");
        }
        String target = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        boolean isDevTarget =
                target.endsWith("dev") ||
                target.equals("mcp") ||
                target.equals("srg");

        boolean isProduction =
                System.getProperties().containsKey("production");
        isDevEnvironment = isDevTarget && !isProduction;

        InitializeHelper helper = new InitializeHelper(environment);
        helper.collectScanners();

        try {
            helper.scanResources();
        } catch (IOException e) {
            LOGGER.debug("Unexpected: Error on ResourceScanEngine!");
            return;
        }

        helper.registerPatches(box.patchRegistry);
        helper.registerMixinConnectors();
        helper.registerEmbedded(box.embeddedRegistry);
        initialized = true;
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {
    }

    @Override
    public List<ITransformer> transformers() {
        LOGGER.info("Creating delegating transformer.");
        return List.of(new FallenDelegatingTransformer(box.patchRegistry, new DefaultPatchCtorContext(box.patchRegistry.classBytes.keySet())),
                new FallenClassMergeTransformer(box.embeddedRegistry.classMergeEngine));
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isDevEnvironment() {
        return isDevEnvironment;
    }
}
