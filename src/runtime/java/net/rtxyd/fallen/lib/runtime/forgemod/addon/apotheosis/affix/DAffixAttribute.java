package net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import net.rtxyd.fallen.lib.util.ins_attr.AInsAttribute;
import net.rtxyd.fallen.lib.util.ins_attr.InsAttributeModifier;

import java.util.Map;

/**
 * Default affix attribute
 */
public class DAffixAttribute extends AInsAttribute<AffixInstance> {
    public DAffixAttribute(AffixInstance instance, Map<String, InsAttributeModifier> modifiers, float initBase, float initFinal) {
        super(instance, modifiers, initBase, initFinal);
    }
}
