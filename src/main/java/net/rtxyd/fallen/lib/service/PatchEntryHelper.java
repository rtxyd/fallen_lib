package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.api.IFallenPatch;
import net.rtxyd.fallen.lib.config.FallenConfig;
import net.rtxyd.fallen.lib.engine.JarInJarContainer;
import net.rtxyd.fallen.lib.engine.JarInJarResource;
import net.rtxyd.fallen.lib.type.engine.Resource;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;
import net.rtxyd.fallen.lib.type.service.IClassBytesProvider;
import net.rtxyd.fallen.lib.type.service.IPatchEntryFunc;
import net.rtxyd.fallen.lib.util.patch.InserterMethodData;
import net.rtxyd.fallen.lib.util.patch.InserterType;
import net.rtxyd.fallen.lib.util.patch.PatchOption;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PatchEntryHelper {

    private final IPatchSecurityHelper securityHelper = new PatchSecurityHelperV2();

    // out parameters must not be null
    public void buildPatchEntries(FallenConfig cfg, Resource rc, List<FallenPatchEntry> outEntries, Map<String, byte[]> outPatchBytes) {
        Optional<File> contOpt = rc.container().asFile();
        if (contOpt.isEmpty()) {
            if (rc instanceof JarInJarResource jr) {
                try {
                    buildEntriesInnerJr(cfg, jr, outEntries, outPatchBytes, this::parseAndBuild);
                } catch (IOException e) {
                    FallenBootstrap.LOGGER.error("Failed build entries for [{}], file [{}]", cfg.getPackage(), jr.calculateLocation(jr.getMaxDepth()));
                    e.printStackTrace();
                }
            }
            return;
        }
        File cont = contOpt.get();
        // in development environment.
        if (cont.isDirectory()) {
            buildEntriesInner(outEntries, outPatchBytes, cfg, rc, (file, zn) -> {
                File f = file.container().asFile().orElseThrow();
                try (InputStream isA = getClass().getClassLoader().getResourceAsStream(zn)) {
                    if (isA != null) {
                        return isA.readAllBytes();
                    }
                    try (InputStream isB = ClassLoader.getSystemClassLoader().getResourceAsStream(zn)) {
                        if (isB != null) {
                            return isB.readAllBytes();
                        }
                    }
                    return null;
                } catch (IOException e) {
                    FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, f.getName(), e);
                    return null;
                }
            }, this::parseAndBuild);
            return;
        }

        buildEntriesInner(outEntries, outPatchBytes, cfg, rc, (jarResource, zn) -> {
            File jar = jarResource.container().asFile().orElseThrow();
            try (JarFile jarFile = new JarFile(jar)) {
                JarEntry jarEntry = jarFile.getJarEntry(zn);
                if (jarEntry == null) {
                    FallenBootstrap.LOGGER.warn("Warning: class file [{}] not found in [{}]", zn, jar.getName());
                    return null;
                }
                try (InputStream is = jarFile.getInputStream(jarEntry)) {
                    return is.readAllBytes();
                }
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing [{}] in jarFile [{}]",zn, jar.getName(), e);
                return null;
            }
        }, this::parseAndBuild);
    }

    private void buildEntriesInnerJr(FallenConfig cfg, JarInJarResource jr, List<FallenPatchEntry> outEntries, Map<String, byte[]> outStoredBytes, IPatchEntryFunc entrySupplier) throws IOException {
        List<String> cls = cfg.buildClassNames();
        jr.openParent(jis -> {
            JarEntry entry;
            int counter = 0;
            int size = cls.size();
            String name = cls.get(counter);
            String zn = name.replace(".", "/") + ".class";

            try {
                while ((entry = jis.getNextJarEntry()) != null && counter < size) {
                    if (entry.getName().endsWith(zn)) {
                        zn = cls.get(counter).replace(".", "/") + ".class";
                        counter += 1;
                        byte[] bytesA = jis.readAllBytes();
                        ClassNode cn = new ClassNode();
                        new ClassReader(bytesA).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        if (cn.invisibleAnnotations != null) {
                            AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(cn.invisibleAnnotations);
                            Optional<AnnotationData> opt = factory.getAnnotation("Lnet/rtxyd/fallen/lib/api/annotation/FallenPatch;");
                            if (opt.isEmpty()
                                    || !securityHelper.isPatchClassSafe(cn)) {
                                continue;
                            }
                            FallenPatchEntry pEntry = entrySupplier.with(name, opt.get(), jr);
                            outEntries.add(pEntry);
                            outStoredBytes.put(name, bytesA);
                        }
                    }
                }
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing inner jar {}",
                        jr.calculateLocation(((JarInJarContainer) jr.container()).getInnerJarFiles().size()), e);
            }
        });
    }

    private void buildEntriesInner(List<FallenPatchEntry> entries, Map<String, byte[]> patchBytes, FallenConfig cfg, Resource rc, IClassBytesProvider bytesFunction, IPatchEntryFunc entrySupplier) {
        int counter = 0;
        List<String> cfgCls = cfg.buildClassNames();
        for (String patchName : cfgCls) {
            String zn = patchName.replace(".", "/") + ".class";
            try {
                byte[] inputBytes = bytesFunction.getClassBytes(rc, zn);
                if (inputBytes == null) {
                    continue;
                }
                ClassNode cn = new ClassNode();
                new ClassReader(inputBytes).accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                if (cn.invisibleAnnotations != null) {
                    AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(cn.invisibleAnnotations);
                    Optional<AnnotationData> opt = factory.getAnnotation("Lnet/rtxyd/fallen/lib/api/annotation/FallenPatch;");
                    if (opt.isEmpty()
                            || !securityHelper.isPatchClassSafe(cn)) {
                        continue;
                    }
                    FallenPatchEntry pEntry = entrySupplier.with(patchName, opt.get(), rc);
                    entries.add(pEntry);
                    patchBytes.put(patchName, inputBytes);
                    counter += 1;
                }
            } catch (Exception e) {
                FallenBootstrap.LOGGER.debug("Failed parsing fallen patch class {}", zn, e);
            }
        }
        if (counter == 0) {
            FallenBootstrap.LOGGER.warn("Empty entries: package: {}", cfg.getPackage());
        }
    }

//    @SuppressWarnings("unchecked")
    public FallenPatchEntry parseAndBuild(String className, AnnotationData data, Resource rc) {
        Integer pr = (Integer) data.get("priority");
        List<String> ic = data.getWithDefaut("inserters", List.of());
        Map<String, InserterMethodData> inserterMap = new HashMap<>();
        buildInserter(inserterMap, ic, rc);
        if (inserterMap.isEmpty()) {
            FallenBootstrap.LOGGER.debug("Warning: inserters of {} is empty. May be unstandard patch.", className);
        }
        if (pr == null) {
            pr = 1000;
        }
        FallenPatchEntry.Targets targets;
        AnnotationData tarData = (AnnotationData) data.get("targets");
        if (tarData == null) {
            targets = new FallenPatchEntry.Targets();
        } else {
            targets = new FallenPatchEntry.Targets();
            List<String> exact = tarData.getWithDefaut("exact", List.of());
            List<String> subclass = tarData.getWithDefaut("subclass", List.of());
            if (containsForbidden(exact) || containsForbidden(subclass)) {
                FallenBootstrap.LOGGER.warn("Warning: {} targets mc or forge class, " +
                        "it is not supported for now. May support in the future.", className);
            } else {
                targets = new FallenPatchEntry.Targets().from(exact, subclass);
            }
        }
        return buildEntry(className, pr, targets, inserterMap);
    }

    private void buildInserter(Map<String, InserterMethodData> inserterMap, List<String> classNames, Resource rc) {
        if (rc instanceof JarInJarResource jr) {
            try {
                parseAndBuildInserterJr(inserterMap, classNames, jr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if (rc.container().isDirectory()) {
            for (String className : classNames) {
                parseAndBuildInserter(inserterMap, rc, className);
            }
        }
    }

    private void parseAndBuildInserterJr(Map<String, InserterMethodData> inserterMap, List<String> cls, JarInJarResource jr) throws IOException {
        jr.openParent(jis -> {
            JarEntry entry;
            int counter = 0;
            int size = cls.size();
            String name = cls.get(counter);
            String internal = name.replace(".", "/");
            String zn = internal + ".class";
            try {
                while ((entry = jis.getNextJarEntry()) != null && counter < size) {
                    if (entry.getName().endsWith(zn)) {
                        zn = cls.get(counter).replace(".", "/") + ".class";
                        counter += 1;
                        byte[] bytesA = jis.readAllBytes();
                        ClassNode cn = new ClassNode();
                        new ClassReader(bytesA).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        buildInserterInner(inserterMap, cn, internal, name);
                    }
                }
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing inner jar {}",
                        jr.calculateLocation(((JarInJarContainer) jr.container()).getInnerJarFiles().size()), e);
            }
        });
    }

    private void parseAndBuildInserter(Map<String, InserterMethodData> inserterMap, Resource rc, String className) {
        String internal = className.replace(".", "/");
        String zn = internal + ".class";
        // in development environment.
        ResourceContainer cont = rc.container();
        if (cont.isDirectory()) {
            ClassNode cn = null;
            try (InputStream isA = getClass().getClassLoader().getResourceAsStream(zn)) {
                if (isA != null) {
                    cn = new ClassNode();
                    new ClassReader(isA).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                try (InputStream isB = ClassLoader.getSystemClassLoader().getResourceAsStream(zn)) {
                    if (isB != null) {
                        cn = new ClassNode();
                        new ClassReader(isB).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    }
                }
                if (cn == null) {
                    FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, cont.getName());
                    return;
                }
                buildInserterInner(inserterMap, cn, internal, className);
            } catch (IOException e) {
                FallenBootstrap.LOGGER.debug("Failed parsing [{}] in folder [{}]", zn, cont.getName(), e);
            }
            return;
        }
        try (JarFile jarFile = new JarFile(cont.asFile().orElseThrow())) {
            JarEntry jarEntry = jarFile.getJarEntry(zn);
            if (jarEntry == null) {
                FallenBootstrap.LOGGER.warn("Warning: inserter class file [{}] not found in [{}]", zn, cont.getName());
                return;
            }
            try (InputStream is = jarFile.getInputStream(jarEntry)) {
                ClassNode cn = new ClassNode();
                new ClassReader(is).accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
                buildInserterInner(inserterMap, cn, internal, className);
            }
        } catch (IOException e) {
            FallenBootstrap.LOGGER.debug("Failed parsing [{}] in jarFile [{}]",zn, cont.getName(), e);
        }
    }

    private void buildInserterInner(Map<String, InserterMethodData> inserterMap, ClassNode cn, String internalName, String qualifiedName) {
        String sStandard = InserterType.standardStarter();
        String fInserter = IFallenPatch.fallenInserterDescriptor();
        for (MethodNode mn : cn.methods) {
            if (mn.invisibleAnnotations != null) {
                AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(mn.invisibleAnnotations);
                Optional<AnnotationData> opt = factory.getAnnotation(fInserter);
                if (opt.isEmpty()) {
                    continue;
                }
                AnnotationData data = opt.get();
                String type = data.getWithDefaut("type", InserterType.STANDARD.name());
                InserterType type1 = InserterType.valueOf(type);
                if (type == null) {
                    FallenBootstrap.LOGGER.warn("Inserter type [{}] does not exist", type);
                }
                if ((mn.access & Opcodes.ACC_PUBLIC) != 0
                        && (mn.access & Opcodes.ACC_STATIC) != 0) {
                    if (!mn.desc.startsWith(sStandard)) {
                        throw new UnsupportedOperationException(String.format("Inserter method descriptor mismatches! Expect: %s", sStandard));
                    }
                    InserterMethodData.Params detail;
                    AnnotationData params = (AnnotationData) data.get("params");
                    if (params == null) {
                        detail = InserterMethodData.Params.createDefault();
                    } else {
                        List<Integer> catchOuterArgs = params.getWithDefaut("catchOuterArgs", List.of());
                        int[] args = new int[catchOuterArgs.size()];
                        for (int i = 0; i < catchOuterArgs.size(); i++) {
                            args[i] = catchOuterArgs.get(i);
                        }
                        int modifyArg = params.getWithDefaut("modifyArg", -1);
                        detail = new InserterMethodData.Params(args, modifyArg);
                    }
                    List<String> options = data.getWithDefaut("options", List.of());
                    Set<PatchOption> patchOptions = new HashSet<>();
                    for (String option : options) {
                        patchOptions.add(PatchOption.valueOf(option));
                    }
                    switch (type1) {
                        case STANDARD, BEFORE_MODIFY_ARG -> {
                            InserterMethodData value = new InserterMethodData(new MethodInsnNode(Opcodes.INVOKESTATIC, internalName, mn.name, mn.desc), type1, detail, patchOptions);
                            putAndLog(inserterMap, value, qualifiedName, mn, type1);
                        }
                        case STANDARD_VOID -> {
                            if (mn.desc.endsWith("V")) {
                                InserterMethodData value = new InserterMethodData(new MethodInsnNode(Opcodes.INVOKESTATIC, internalName, mn.name, mn.desc), type1, detail, patchOptions);
                                putAndLog(inserterMap, value, qualifiedName, mn, type1);
                            } else {
                                throw new UnsupportedOperationException(String.format("Method is annotated with VOID, but return type is not void! Culprit: [ %s ]", qualifiedName + "." + mn.name + mn.desc));
                            }
                        }
                    }
                }
            }
        }
    }

    private void putAndLog(Map<String, InserterMethodData> inserterMap, InserterMethodData data, String qualifiedName, MethodNode mn, InserterType type1) {
        String key = qualifiedName + "." + mn.name + "." + type1.ordinal();
        if (inserterMap.containsKey(key)) {
            throw new UnsupportedOperationException(String.format("Duplicated inserter method name is not supported, please rename it. Culprit: [ %s ]", qualifiedName + "." + mn.name + mn.desc));
        }
        inserterMap.put(key, data);
        FallenBootstrap.LOGGER.info("Registered inserter: [ {} ] with method: [ {} ]", key, qualifiedName + "." + mn.name + mn.desc);
    }

    private FallenPatchEntry buildEntry(String className, int priority, FallenPatchEntry.Targets targets, Map<String, InserterMethodData> inserters) {
        return new FallenPatchEntry(
                className,
                targets.computeTargeter(),
                priority,
                targets,
                inserters);
    }

    public boolean containsForbidden(List<String> targets) {
        for (String s : targets) {
            if (s.startsWith("net.minecraft.")
                    || s.startsWith("net.minecraftforge.")) {
                return true;
            }
        }
        return false;
    }
}
