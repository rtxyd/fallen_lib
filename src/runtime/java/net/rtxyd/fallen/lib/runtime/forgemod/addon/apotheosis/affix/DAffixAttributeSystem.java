package net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.rtxyd.fallen.lib.util.ins_attr.ADefaultInsAttributeSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Default affix attribute system
 */
public class DAffixAttributeSystem extends ADefaultInsAttributeSystem<DynamicHolder<? extends Affix>, AffixInstance, DAffixAttribute> {


    public DAffixAttributeSystem(Map<DynamicHolder<? extends Affix>, AffixInstance> instances) {
        super(instances);
    }

    @Override
    public final DAffixAttributeSystem updateWith(Map<DynamicHolder<? extends Affix>, AffixInstance> instances) {
        return (DAffixAttributeSystem) super.updateWith(instances);
    }

    @Override
    public DAffixAttribute createAttributeWith(AffixInstance instance) {
        return new DAffixAttribute(instance, new HashMap<>(5), instance.level(), instance.level());
    }

    @Override
    public AffixInstance createInsWith(AffixInstance old, float value) {
        return new AffixInstance(old.affix(), old.stack(), old.rarity(), value);
    }
}
