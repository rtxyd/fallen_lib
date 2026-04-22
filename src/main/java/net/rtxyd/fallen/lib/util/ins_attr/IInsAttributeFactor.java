package net.rtxyd.fallen.lib.util.ins_attr;

public interface IInsAttributeFactor<K, I, A extends AInsAttribute<I>> {
    A buildAttrWith(K k, I value);
}
