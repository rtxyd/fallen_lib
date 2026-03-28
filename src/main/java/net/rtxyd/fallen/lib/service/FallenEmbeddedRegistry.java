package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.extra.cmerge.SimpleClassMergeEngine;

class FallenEmbeddedRegistry {
    SimpleClassMergeEngine classMergeEngine = new SimpleClassMergeEngine();
    void registerClassMerge(String internalName) {
        classMergeEngine.register(internalName);
    }
}
