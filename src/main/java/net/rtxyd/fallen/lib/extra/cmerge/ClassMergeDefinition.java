package net.rtxyd.fallen.lib.extra.cmerge;

import org.objectweb.asm.tree.ClassNode;

public record ClassMergeDefinition (ClassNode templateNode, int priority) {}