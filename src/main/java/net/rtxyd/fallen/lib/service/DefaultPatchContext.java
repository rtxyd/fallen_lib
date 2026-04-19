package net.rtxyd.fallen.lib.service;

import net.rtxyd.fallen.lib.type.service.IFallenPatchContext;
import net.rtxyd.fallen.lib.util.patch.InserterKey;
import net.rtxyd.fallen.lib.util.patch.InserterMethodData;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

public class DefaultPatchContext implements IFallenPatchContext {
    private final Set<String> appliedByClass = new LinkedHashSet<>();
    private final Set<String> appliedByClassView = Collections.unmodifiableSet(appliedByClass);
    private boolean isClassEnd;
    private ClassNode classNode;
    private Map<String, InserterMethodData> currentEntryInserters;

    public void beginClass() {
        isClassEnd = false;
        appliedByClass.clear();
    }

    void recordPatchEffect(String patchId) {
        appliedByClass.add(patchId);
    }

    void setClassNode(ClassNode node) {
        this.classNode = node;
    }

    /**
     * Please use this carefully. It can only be manually patch.
     * Better with PatchUtil
     * @return ClassNode, current patching class
     */
    @Override
    public ClassNode getClassNode() {
        return this.classNode;
    }

    @Override
    public Set<String> currentClassPatchesApplied() {
        return appliedByClassView;
    }

    public void endClass() {
        isClassEnd = true;
        appliedByClass.clear();
    }

    @Override
    public InserterMethodData getFallenInserter(InserterKey inserterKey) {
        return currentEntryInserters.get(inserterKey.combine());
    }

    void setEntryInserters(Map<String, InserterMethodData> inserter) {
        currentEntryInserters = Collections.unmodifiableMap(inserter);
    }

    public boolean isClassEnd() {
        return isClassEnd;
    }
}
