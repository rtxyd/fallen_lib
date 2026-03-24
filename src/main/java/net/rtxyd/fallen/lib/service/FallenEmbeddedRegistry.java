package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.extra.cmerge.SimpleClassMergeEngine;

class FallenEmbeddedRegistry {
    SimpleClassMergeEngine engine = new SimpleClassMergeEngine();
    void register(String internalName) {
        engine.register(internalName);
    }
}
