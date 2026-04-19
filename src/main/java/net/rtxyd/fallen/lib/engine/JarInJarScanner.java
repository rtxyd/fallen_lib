package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.FallenLib;
import net.rtxyd.fallen.lib.type.engine.ResourceConsumer;
import net.rtxyd.fallen.lib.type.engine.ResourceScanner;
import net.rtxyd.fallen.lib.util.OuterTrackedInputStream;
import net.rtxyd.fallen.lib.util.SafeUncloseableInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarInJarScanner implements ResourceScanner {
    private final File nestHostFile;
    private final InputStream iStream;
    private final AtomicBoolean streamState;
    private final JarEntry entry;

    private JarInJarScanner(File nestHostFile, InputStream iStream, AtomicBoolean streamState, JarEntry entry) {
        this.nestHostFile = nestHostFile;
        this.iStream = iStream;
        this.streamState = streamState;
        this.entry = entry;
    }

    public static JarInJarScanner create(File nestHostFile, InputStream iStream, JarEntry entry) {
        AtomicBoolean state = new AtomicBoolean();
        InputStream stream = new OuterTrackedInputStream(iStream, state);
        return new JarInJarScanner(nestHostFile, stream, state, entry);
    }

    @Override
    public void scan(ResourceConsumer consumer) throws IOException {
//        if (!nestHostFile.getName().equals(FallenLib.MOD_NAME)) return;
        scanA(consumer);
    }

    public void scanA(ResourceConsumer consumer) throws IOException {
        JarInputStream jar = new JarInputStream(iStream);
        LinkedList<JarEntry> entries = new LinkedList<>();
        entries.add(entry);
        JarInJarContainer container = new JarInJarContainer(nestHostFile, entries, entry);
        JarEntry jarEntry;
        while ((jarEntry = jar.getNextJarEntry()) != null) {
            if (jarEntry.isDirectory()) continue;
            if (jarEntry.getName().endsWith(".jar")) {
                new JarInJarScanner(nestHostFile, jar, streamState, jarEntry).scanA(consumer);
            }
            consumer.accept(new JarInJarResource(container, new SafeUncloseableInputStream(jar), streamState, jarEntry));
        }
    }
}
