package net.rtxyd.fallen.lib.extra.cmerge;

import cpw.mods.modlauncher.api.ITransformer;
import net.rtxyd.fallen.lib.service.AnnotationData;
import net.rtxyd.fallen.lib.service.AsmAnnotationDataFactory;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleClassMergeEngine implements IClassMergeEngine {

    private final Map<String, ClassMergeDefinition> merges = new HashMap<>();

    @Override
    public void register(String templateInternalName) {
        byte[] bytes = ASMUtils.readClassBytes(templateInternalName);
        ClassNode node = ASMUtils.read(bytes);

        AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(node.invisibleAnnotations);
        Optional<AnnotationData> opt = factory.getAnnotation(Type.getDescriptor(ClassMerge.class));
        if (opt.isEmpty()) {
            throw new RuntimeException("Unexpected: " +
                    templateInternalName +
                    " Missing @ClassMerge");
        }
        String target = opt.get().getWithDefaut("target", null);
        if (target == null) {
            throw new RuntimeException("Unexpected: " +
                    templateInternalName +
                    " has null target!");
        }

        merges.computeIfAbsent(target.replace(".", "/"), s -> new ClassMergeDefinition(node, 1000));
    }

    @Override
    public ClassNode transform(ClassNode target) {
        ClassMergeDefinition def = merges.get(target.name);
        if (def != null) {
            applyMerge(target, def.templateNode());

            return target;
        }


        return target;
    }

    @Override
    public void applyMerge(ClassNode target, ClassNode template) {

        // interfaces
        for (String itf : template.interfaces) {
            if (!target.interfaces.contains(itf))
                target.interfaces.add(itf);
        }

        // fields
        for (FieldNode field : template.fields) {
            if (field.name.startsWith("this$")) {
                continue;
            }

            AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(field.invisibleAnnotations);
            Optional<AnnotationData> opt = factory.getAnnotation(Type.getDescriptor(MergeShadow.class));
            if (opt.isPresent()) {
                continue;
            }

            target.fields.add(new FieldNode(
                    field.access,
                    field.name,
                    field.desc,
                    field.signature,
                    null
            ));
        }

        // methods
        for (MethodNode method : template.methods) {
            if ("<init>".equals(method.name)) {
                injectConstructorInit(target, template, method);
                continue;
            }
            AsmAnnotationDataFactory factory = new AsmAnnotationDataFactory(method.invisibleAnnotations);
            Optional<AnnotationData> opt = factory.getAnnotation(Type.getDescriptor(MergeInject.class));
            if (opt.isPresent()) {
                String type = opt.get().getWithDefaut("type", null);
                if (type == null) {
                    throw new RuntimeException("Unexpected: " + "annotation MergeInject in " + method.name + " in " + template.name + " has null method name!");
                }
                switch (type) {
                    case "TAIL" -> {
                        injectTail(target, template, method, opt.get());
                        continue;
                    }
                }
            }

            MethodNode copy = ASMUtils.cloneMethodWithOwnerRemap(method, template.name, target.name);

            target.methods.add(copy);
        }
    }

    @Override
    public void injectConstructorInit(ClassNode target, ClassNode template, MethodNode method) {
        MethodNode copiedInit = ASMUtils.cloneMethodWithOwnerRemap(method, template.name, target.name);
        for (MethodNode mn : target.methods) {
            if ("<init>".equals(mn.name)) {
                InsnList collect = new InsnList();
                Map<LabelNode, LabelNode> labels = Bytecode.cloneLabels(copiedInit.instructions);
                for (AbstractInsnNode insn = copiedInit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (isSuperInit(insn, template)) {
                        AbstractInsnNode insnWalker = insn;
                        int extraStack = copiedInit.maxStack - mn.maxStack;
                        if (extraStack > 0) {
                            mn.maxStack += extraStack;
                        }
                        for (insnWalker = insnWalker.getNext(); insnWalker != null; insnWalker = insnWalker.getNext()) {
                            if (insnWalker.getOpcode() == Opcodes.RETURN) {
                                break;
                            }
                            collect.add(insnWalker.clone(labels));
                        }
                        break;
                    }
                }
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (isSuperInit(insn, target)) {
                        AbstractInsnNode afterSuper = insn.getNext();
                        mn.instructions.insertBefore(afterSuper, collect);
                        break;
                    }
                }
            }
        }
    }

    private static boolean isSuperInit(AbstractInsnNode insn, ClassNode template) {
        if (!(insn instanceof MethodInsnNode m)) return false;

        if (m.getOpcode() != Opcodes.INVOKESPECIAL) return false;
        if (!m.name.equals("<init>")) return false;

        return m.owner.equals(template.name) || m.owner.equals(template.superName);
    }

    @Override
    public void injectTail(ClassNode target, ClassNode template, MethodNode injectMethod, AnnotationData data) {
        String targetMethodName = data.getWithDefaut("method", null);
        if (targetMethodName == null) {
            throw new RuntimeException("Unexpected: " + "annotation MergeInject in " + injectMethod.name + " in " + template.name + " has null method name!");
        }

        int opcode = Opcodes.INVOKEVIRTUAL;
        MethodNode copiedInjectMethod =
                ASMUtils.cloneMethodWithOwnerRemap(
                        injectMethod,
                        template.name,
                        target.name
                );
        if ((injectMethod.access & Opcodes.ACC_STATIC) != 0) {
            opcode = Opcodes.INVOKESTATIC;
        }

        target.methods.add(copiedInjectMethod);

        for (MethodNode method : target.methods) {

            if (!method.name.equals(targetMethodName)) {
                continue;
            } else if ((method.access & Opcodes.ACC_STATIC) != 0 && opcode !=Opcodes.INVOKESTATIC) {
                throw new RuntimeException(template.name + "/" + injectMethod.name  + " | Method access mismatch!");
            }

            InsnList call = new InsnList();

            if (opcode == Opcodes.INVOKEVIRTUAL) {
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));

                Type[] args = Type.getArgumentTypes(method.desc);

                int index = 1;
                for (Type ignored : args) {
                    call.add(new VarInsnNode(Opcodes.ALOAD, index));
                    index++;
                }
            } else {
                Type[] args = Type.getArgumentTypes(method.desc);

                int index = 0;
                for (Type ignored : args) {
                    call.add(new VarInsnNode(Opcodes.ALOAD, index));
                    index++;
                }
            }


            call.add(new MethodInsnNode(
                    opcode,
                    target.name,
                    injectMethod.name,
                    injectMethod.desc,
                    false
            ));

            for (AbstractInsnNode insn : method.instructions.toArray()) {

                if (insn.getOpcode() == Opcodes.RETURN) {
                    method.instructions.insertBefore(insn, call);
                }
            }
        }
    }

    public @NotNull Set<ITransformer.Target> getTargets() {
        return merges.keySet().stream().map(ITransformer.Target::targetClass).collect(Collectors.toSet());
    }
}
