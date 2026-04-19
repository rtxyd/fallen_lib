package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.FallenLib;
import net.rtxyd.fallen.lib.type.engine.ResourceConsumer;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;
import net.rtxyd.fallen.lib.type.engine.ResourceScanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarScanner implements ResourceScanner {
    private final File jarFile;
    private boolean scanInnerJar;

    public JarScanner(File jarFile, boolean scanInnerJar) {
        this.jarFile = jarFile;
        this.scanInnerJar = scanInnerJar;
    }

    @Override
    public void scan(ResourceConsumer consumer) {
        try (JarFile jar = new JarFile(jarFile)) {
            checkManifest(jar.getManifest());
            ResourceContainer container = new JarContainer(jarFile);
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                // to prevent spamming, here is no try-catch
                tryScanInnerJar(entry, jar, consumer);
                consumer.accept(new JarResource(container, jar, entry));
            }
        } catch (IOException e) {
            ResourceScanEngine.LOGGER.warn("Warning: can't open: [{}]", jarFile.getAbsolutePath());
        }
    }

    private void checkManifest(Manifest manifest) {
        if (manifest != null) {
            Attributes attrs = manifest.getMainAttributes();
            String title = attrs.getValue("Specification-Title");
            // check if it's runtime lib
            if (title.equals(FallenLib.SPEC_TITLE_ALL)) {
                scanInnerJar = true;
            }
        }
    }

    private void tryScanInnerJar(JarEntry entry, JarFile jar, ResourceConsumer consumer) {
        if (scanInnerJar && entry.getName().endsWith(".jar")) {
            try (InputStream jis = jar.getInputStream(entry)) {
                JarInJarScanner.create(jarFile, jis, entry).scan(consumer);
            } catch (IOException e) {
                ResourceScanEngine.LOGGER.warn("Warning: jar in jar reading is failed, outer jar: [{}], entry: [{}]", jarFile.getAbsolutePath(), entry.getName());
            }
        }
    }
}