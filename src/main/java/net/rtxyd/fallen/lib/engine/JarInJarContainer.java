package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.File;
import java.util.LinkedList;
import java.util.Optional;
import java.util.jar.JarEntry;

public class JarInJarContainer implements ResourceContainer {
    private final File outerJar;
    private final LinkedList<JarEntry> innerJarFiles;
    private final JarEntry selfEntry;

    public JarInJarContainer(File outerJar, LinkedList<JarEntry> innerJarFiles, JarEntry selfEntry) {
        this.outerJar = outerJar;
        this.innerJarFiles = innerJarFiles;
        this.selfEntry = selfEntry;
    }

    @Override
    public String getName() {
        return selfEntry.getName();
    }

    @Override
    public Optional<File> asFile() {
        return Optional.empty();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public File getOuterJar() {
        return outerJar;
    }

    @Override
    public String path() {
        return null;
    }

    public LinkedList<JarEntry> getInnerJarFiles() {
        return innerJarFiles;
    }
}
