package net.rtxyd.fallen.lib.type.service;

import net.rtxyd.fallen.lib.service.AnnotationData;
import net.rtxyd.fallen.lib.service.FallenPatchEntry;
import net.rtxyd.fallen.lib.type.engine.Resource;

@FunctionalInterface
public interface IPatchEntryFunc {
    FallenPatchEntry with(String patchName, AnnotationData anData, Resource rc);
}
