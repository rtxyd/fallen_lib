package net.rtxyd.fallen.lib.type.engine;

import java.io.File;
import java.util.Optional;

public interface ResourceContainer {
    String getName();

    Optional<File> asFile();

    boolean isDirectory();

    String path();
}
