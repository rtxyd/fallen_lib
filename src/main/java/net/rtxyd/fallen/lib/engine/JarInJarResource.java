package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.Resource;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class JarInJarResource implements Resource {
    private final JarInJarContainer container;
    // don't get stream, it can only be taken by processor;
    InputStream stream;
    AtomicBoolean streamClosed;
    private final JarEntry selfEntry;

    public JarInJarResource(JarInJarContainer container, InputStream stream, AtomicBoolean streamClosed, JarEntry selfEntry) {
        this.container = container;
        this.stream = stream;
        this.streamClosed = streamClosed;
        this.selfEntry = selfEntry;
    }

    @Override
    public String path() {
        return selfEntry.getName();
    }

    @Override
    public InputStream open() throws IOException {
        if (!streamClosed.get()) return stream;
        InputStream stream1 = Files.newInputStream(container.getOuterJar().toPath());
        JarInputStream stream2 = new JarInputStream(stream1);
        AtomicInteger depthCounter = new AtomicInteger(0);
        JarInputStream stream3 = recursiveRead(container.getInnerJarFiles(), stream2, depthCounter);
        return readSelfFromParent(stream3, depthCounter);
    }

    public void open(Predicate<JarEntry> checkEntry, BiConsumer<JarEntry, InputStream> consumer) throws IOException {
        AtomicInteger depthCounter = new AtomicInteger(1);
        openParentA(jir -> {
            try {
                readFromParentConsume(jir,depthCounter, checkEntry, consumer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, depthCounter);
    }

    public void openParent(Consumer<JarInputStream> consumer) throws IOException {
        AtomicInteger depthCounter = new AtomicInteger(1);
        openParentA(consumer, depthCounter);
    }

    private void openParentA(Consumer<JarInputStream> consumer, AtomicInteger depthCounter) throws IOException {
        try (JarFile jar = new JarFile(container.getOuterJar())) {
            LinkedList<JarEntry> jarFileList = new LinkedList<>(container.getInnerJarFiles());
            if (jarFileList.isEmpty()) throw new RuntimeException(String.format("Entry size should be at least 1, but it's jij resource. Entry: %s", container.getOuterJar().getPath() + "!/" + selfEntry.getName()));
            JarEntry firstEntry = jarFileList.getFirst();
            InputStream jis = jar.getInputStream(firstEntry);
            jarFileList.removeFirst();
            consumer.accept(recursiveRead(jarFileList, new JarInputStream(jis), depthCounter));
        } catch (Exception e) {
            ResourceScanEngine.LOGGER.error("Failed open jar in jar resource. Location: {}", location(container.getOuterJar().getPath(), selfEntry));
            e.printStackTrace();
        }
    }

    public InputStream readSelfFromParent(JarInputStream parentJar, AtomicInteger depth) throws IOException {
        JarEntry entry;
        while((entry = parentJar.getNextJarEntry()) != null) {
            if (entry.getName().equals(selfEntry.getName())) {
                return parentJar;
            }
        }
        throw new RuntimeException(String.format("Can't find JarEntry: [ %s ] in parent jar in jar [ %s ]. It should be there.", selfEntry.getName(), calculateLocation(depth.get())));
    }

    public void readFromParentConsume(JarInputStream parentJar, AtomicInteger depth, Predicate<JarEntry> checkEntry, BiConsumer<JarEntry, InputStream> consumer) throws IOException {
        JarEntry entry;
        while((entry = parentJar.getNextJarEntry()) != null) {
            if (checkEntry.test(entry)) {
                consumer.accept(entry, parentJar);
            }
        }
        throw new RuntimeException(String.format("Can't find JarEntry: [ %s ] in parent jar in jar [ %s ]. It should be there.", selfEntry.getName(), calculateLocation(depth.get())));
    }

    private JarInputStream recursiveRead(LinkedList<JarEntry> jarFileList, JarInputStream jis, AtomicInteger depth) throws IOException {
        if (jarFileList.isEmpty()) {
            return jis;
        }
        JarInputStream jisNext = null;
        JarEntry entryInJar;
        JarEntry jarFile = jarFileList.getFirst();
        while ((entryInJar = jis.getNextJarEntry()) != null) {
            if (entryInJar.getName().equals(jarFile.getName())) {
                jisNext = jis;
                break;
            }
        }
        if (entryInJar == null || jisNext == null) {
            throw new RuntimeException(String.format("Can't find JarEntry: [ %s ]. It should be there.", calculateLocation(depth.get())));
        }
        jarFileList.removeFirst();
        depth.incrementAndGet();
        return recursiveRead(jarFileList, new JarInputStream(jisNext), depth);
    }

    public String calculateLocation(int depth) {
        var list = container.getInnerJarFiles();
        if (depth > list.size()) throw new RuntimeException("Depth out of bound.");
        StringBuilder combinedEntry = new StringBuilder();
        combinedEntry.append(container.getOuterJar().getPath());
        for (int i = 0; i < depth; i++) {
            combinedEntry.append("/");
            combinedEntry.append(list.get(i).getName());
        }
        return combinedEntry.toString();
    }

    public int getMaxDepth() {
        return container.getInnerJarFiles().size();
    }

    private String location(String jarLoc, JarEntry entry) {
        return jarLoc + "!/" + entry.getName();
    }

    @Override
    public ResourceContainer container() {
        return container;
    }

    @Override
    public boolean isHandledByOuterStream() {
        return true;
    }
}
