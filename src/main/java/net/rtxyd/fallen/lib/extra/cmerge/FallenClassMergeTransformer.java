package net.rtxyd.fallen.lib.extra.cmerge;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public class FallenClassMergeTransformer implements ITransformer<ClassNode> {

    private SimpleClassMergeEngine engine;

    public FallenClassMergeTransformer(SimpleClassMergeEngine engine) {
        this.engine = engine;
    }

    @Override
    public @NotNull ClassNode transform(ClassNode cn, ITransformerVotingContext context) {
        return engine.transform(cn);
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target> targets() {
        return engine.getTargets();
    }
}
