package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.File;
import java.util.Optional;

class JarContainer implements ResourceContainer {
    private final File jar;

    public JarContainer(File jar) {
        this.jar = jar;
    }

    @Override
    public String getName() {
        return jar.getName();
    }

    @Override
    public Optional<File> asFile() {
        return Optional.of(jar);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String path() {
        return jar.getAbsolutePath();
    }
}