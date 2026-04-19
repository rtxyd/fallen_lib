package net.rtxyd.fallen.lib.engine;

import net.rtxyd.fallen.lib.type.engine.Resource;
import net.rtxyd.fallen.lib.type.engine.ResourceContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

record JarResource(
        ResourceContainer container,
        JarFile jar,
        JarEntry entry
) implements Resource {

    @Override
    public String path() {
        return entry.getName();
    }

    @Override
    public InputStream open() throws IOException {
        return jar.getInputStream(entry);
    }

    @Override
    public boolean isHandledByOuterStream() {
        return false;
    }
}