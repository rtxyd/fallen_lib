package net.rtxyd.fallen.lib.engine;


import net.rtxyd.fallen.lib.type.engine.Resource;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class FileResource implements Resource {

    private final ResourceContainer container;
    private final File file;
    private final String path;

    public FileResource(File root, File file, String path) {
        this.container = new DirectoryContainer(root);
        this.file = file;
        this.path = path;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public ResourceContainer container() {
        return container;
    }

    @Override
    public boolean isHandledByOuterStream() {
        return false;
    }
}