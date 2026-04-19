package net.rtxyd.fallen.lib.type.service;

import net.rtxyd.fallen.lib.type.engine.Resource;

@FunctionalInterface
public interface IClassBytesProvider {
    byte[] getClassBytes(Resource resource, String path);
}