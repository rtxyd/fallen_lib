package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.tree.AnnotationNode;

public interface AnnotationDataFactory {
    AnnotationData create(AnnotationNode node);

}
