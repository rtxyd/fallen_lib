package net.rtxyd.fallen.lib.util.ins_attr;

import java.util.Collections;
import java.util.Map;

public class EmptyIASystem<K, I, A extends AInsAttribute<I>> implements IInsAttributeSystem<K, I, A> {
    @Override
    public Map<K, A> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public void addModifier(K key, InsAttributeModifier modifier) {}

    @Override
    public InsAttributeModifier removeModifier(K key, String name) {
        return null;
    }

    @Override
    public Map<K, I> getInput() {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, I> output() {
        return Collections.emptyMap();
    }
}
