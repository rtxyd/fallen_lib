package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AsmAnnotationDataFactory implements AnnotationDataFactory {
    private final List<AnnotationNode> annotationNodes;

    public AsmAnnotationDataFactory(List<AnnotationNode> annotationNodes) {
        this.annotationNodes = annotationNodes == null ? List.of() : annotationNodes;
    }

    @Override
    public AnnotationData create(AnnotationNode node) {
        return new AsmAnnotationData(node);
    }

    public Optional<AnnotationData> getAnnotation(String desc) {
        for (AnnotationNode node : annotationNodes) {
            if (desc.equals(node.desc)) {
                return Optional.of(new AsmAnnotationData(node));
            }
        }
        return Optional.empty();
    }

    public void parseByDesc(String desc, Consumer<AnnotationData> consumer) {
        for (AnnotationNode node1 : annotationNodes) {
            if (desc.equals(node1.desc)) {
                consumer.accept(new AsmAnnotationData(node1));
            }
        }
    }

    public void parseData(AnnotationData data, Consumer<AnnotationData> consumer) {
        consumer.accept(data);
    }
}