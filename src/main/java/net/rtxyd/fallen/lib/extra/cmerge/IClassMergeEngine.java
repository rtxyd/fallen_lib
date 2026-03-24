package net.rtxyd.fallen.lib.extra.cmerge;

import net.rtxyd.fallen.lib.service.AnnotationData;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface IClassMergeEngine {
    void register(String templateInternalName);

    ClassNode transform(ClassNode target);

    void applyMerge(ClassNode target, ClassNode template);

    void injectConstructorInit(ClassNode target, ClassNode template, MethodNode method);

    void injectTail(ClassNode target, ClassNode template, MethodNode injectMethod, AnnotationData data);
}
