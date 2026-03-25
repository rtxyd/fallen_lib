package net.rtxyd.fallen.lib.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.UniqueModListBuilder;
import net.minecraftforge.fml.loading.moddiscovery.*;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileLoadingException;
import net.rtxyd.fallen.lib.config.BuildInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static net.minecraftforge.fml.loading.LogMarkers.LOADING;
import static net.rtxyd.fallen.lib.service.FallenBootstrap.LOGGER;

public class SimpleModLocator extends AbstractJarFileModProvider implements IModLocator {

    // @Override
    public Stream<Path> scanCandidates() {
        var paths = Stream.<Path>builder();
        Path path = getPath();
        if (path == null) return paths.build();
        return paths.add(path).build();
    }

    public Path getPath() {
        final String runtime_loc = "META-INF/runtime/";
        final String mod_name = "fallen_lib";
        final String version = BuildInfo.VERSION;
        final String classifier = "runtime";
        final String fullPath = runtime_loc
                + mod_name
                + "-"
                + version
                + "-"
                + classifier
                + ".jar";
        URL url = this.getClass().getClassLoader().getResource(fullPath);
        if (url == null) {
            if (FallenBootstrap.isDevEnvironment) {
                LOGGER.warn("Run fallen core lib in dev environment without runtime lib.");
                return null;
            } else {
                url = this.getClass().getClassLoader().getResource("net/rtxyd/fallen/lib/runtime/forgemod/FallenLib.class");
                if (url == null) {
                    throw new RuntimeException("Can't find fallen runtime lib!");
                }
            }
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("Failed transfer url %s to URI!", url));
        }
    }

    @Override
    public String name() {
        return "fallen lib mod locator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {}

    @Override
    @SuppressWarnings("resource")
    // forge way to adapt the union path.
    public List<ModFileOrException> scanMods() {
        Path pathInModFile = getPath();
        if (pathInModFile == null) return List.of();
        try {
            final URI filePathUri = new URI("jij:" + (pathInModFile.toAbsolutePath().toUri().getRawSchemeSpecificPart())).normalize();
            final Map<String, ?> outerFsArgs = ImmutableMap.of("packagePath", pathInModFile);
            // we want to hold the file for a long time, so don't need to handle close.
            final FileSystem zipFS = FileSystems.newFileSystem(filePathUri, outerFsArgs);
            final Path pathInFS = zipFS.getPath("/");
            final String modType = IModFile.Type.LIBRARY.name();
            return List.of(createMod(modType, pathInFS));
        } catch (Exception e) {
            LOGGER.error("Failed to load mod fallen lib runtime from fallen lib");
            final RuntimeException exception = new ModFileLoadingException("Failed to load fallen lib runtime");
            exception.initCause(e);

            throw exception;
        }
    }

    public UniqueModListBuilder.UniqueModListData preprocess(List<ModFile> modFiles) {
        List<ModFile> uniqueLibListWithVersion;
        final Map<String, List<ModFile>> libFilesWithVersionByModuleName = modFiles.stream()
                .filter(mf -> mf.getModFileInfo() == null)
                .collect(groupingBy(this::getModId));
        uniqueLibListWithVersion = libFilesWithVersionByModuleName.entrySet().stream()
                .map(this::selectNewestModInfo)
                .toList();
        final Map<String, List<ModFile>> versionedLibIds = uniqueLibListWithVersion.stream()
                .map(this::getModId)
                .collect(Collectors.toMap(
                        Function.identity(),
                        libFilesWithVersionByModuleName::get
                ));
        final List<String> dupedLibErrors = versionedLibIds.values().stream()
                .filter(files -> files.size() > 1)
                .map(mods -> String.format("\tLibrary: '%s' from files: %s",
                        getModId(mods.get(0)),
                        mods.stream()
                                .map(modFile -> modFile.getFileName()).collect(joining(", "))
                )).toList();
        if (!dupedLibErrors.isEmpty()) {
            LOGGER.error(LOADING, "Found duplicate plugins or libraries:\n{}", dupedLibErrors.stream().collect(joining("\n")));
            throw new EarlyLoadingException("Duplicate plugins or libraries found", null, dupedLibErrors.stream()
                    .map(s -> new EarlyLoadingException.ExceptionData(s))
                    .toList());
        }
        final List<ModFile> loadedList = new ArrayList<>();
        loadedList.addAll(uniqueLibListWithVersion);
        return new UniqueModListBuilder.UniqueModListData(loadedList, Map.of());
    }

    public void postprocess(UniqueModListBuilder.UniqueModListData data){
        Map<IModFile.Type, List<ModFile>> modFilesMap = Maps.newHashMap();
        modFilesMap = data.modFiles().stream()
                .collect(Collectors.groupingBy(IModFile::getType));
    }

    private ModFile selectNewestModInfo(Map.Entry<String, List<ModFile>> fullList) {
        List<ModFile> modInfoList = fullList.getValue();
        if (modInfoList.size() > 1) {
            LOGGER.debug("Found {} mods for first modid {}, selecting most recent based on version data", modInfoList.size(), fullList.getKey());
            modInfoList.sort(Comparator.comparing(this::getVersion).reversed());
            LOGGER.debug("Selected file {} for modid {} with version {}", modInfoList.get(0).getFileName(), fullList.getKey(), this.getVersion(modInfoList.get(0)));
        }
        return modInfoList.get(0);
    }

    private ArtifactVersion getVersion(final ModFile mf)
    {
        if (mf.getModFileInfo() == null || mf.getModInfos() == null || mf.getModInfos().isEmpty()) {
            return mf.getJarVersion();
        }

        return mf.getModInfos().get(0).getVersion();
    }

    private String getModId(ModFile modFile) {
        if (modFile.getModFileInfo() == null || modFile.getModFileInfo().getMods().isEmpty()) {
            return modFile.getSecureJar().name();
        }

        return modFile.getModFileInfo().moduleName();
    }
}
