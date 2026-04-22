package net.rtxyd.fallen.lib.util.ins_attr;

import java.util.HashMap;
import java.util.Map;

public abstract class ADefaultInsAttributeSystem<K, I, A extends AInsAttribute<I>> implements IInsAttributeSystem<K, I, A> {

    protected final Map<K, I> instances;
    protected final Map<K, A> attributes;

    protected ADefaultInsAttributeSystem(Map<K, I> instances) {
        this.instances = instances;
        this.attributes = new HashMap<>();
        ensureInit();
    }

    protected void ensureInit() {
        if (attributes.isEmpty()) {
            instances.forEach((k, i) -> {
                attributes.put(k, createAttributeWith(i));
            });
        }
    }

    @Override
    public final Map<K, A> getAttributes() {
        return attributes;
    }

    @Override
    public void addModifier(K key, InsAttributeModifier modifier) {
        if (modifier.getType() == InsAttributeModifier.Type.NONE) return;
        AInsAttribute<I> attribute = attributes.get(key);
        if (attribute != null) {
            attribute.addModifier(modifier.getName(), modifier);
        }
    }

    @Override
    public InsAttributeModifier removeModifier(K key, String name) {
        AInsAttribute<I> attribute = attributes.get(key);
        if (attribute != null) {
            return attribute.removeModifier(name);
        }
        return null;
    }


    public ADefaultInsAttributeSystem<K,I,A> updateWith(Map<K, I> instances) {
        attributes.keySet().retainAll(instances.keySet());
        instances.forEach((k,v) -> {
            if (!attributes.containsKey(k)) {
                attributes.put(k, createAttributeWith(v));
            }
        });
        return this;
    }

    public abstract A createAttributeWith(I instance);

    public abstract I createInsWith(I old, float value);

    @Override
    public final Map<K, I> getInput() {
        return instances;
    }

    @Override
    public final Map<K, I> output() {
        Map<K, I> result = new HashMap<>();
        for (Map.Entry<K, A> entry : attributes.entrySet()) {
            result.put(entry.getKey(), entry.getValue().output(this::createInsWith));
        }
        return result;
    }
}
