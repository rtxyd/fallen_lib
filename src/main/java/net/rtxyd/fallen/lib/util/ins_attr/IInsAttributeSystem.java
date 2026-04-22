package net.rtxyd.fallen.lib.util.ins_attr;

import java.util.Map;

public interface IInsAttributeSystem<K, I, A extends AInsAttribute<I>> {

    Map<K, A> getAttributes();

    void addModifier(K key, InsAttributeModifier modifier);

    InsAttributeModifier removeModifier(K key, String name);

    Map<K, I> getInput();

    Map<K, I> output();
}