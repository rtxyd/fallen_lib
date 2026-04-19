package net.rtxyd.fallen.lib.engine;


import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.File;
import java.util.Optional;

class DirectoryContainer implements ResourceContainer {
    private final File root;

    public DirectoryContainer(File root) {
        this.root = root;
    }

    @Override
    public String getName() {
        return root.getName();
    }

    @Override
    public Optional<File> asFile() {
        return Optional.of(root);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String path() {
        return root.getPath();
    }
}
