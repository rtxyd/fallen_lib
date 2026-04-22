package net.rtxyd.fallen.lib.util.ins_attr;

import java.util.function.Predicate;

public interface IFilterableIAMProvider<T> extends Predicate<T>, IInsAModifierProvider {
    InsAttributeModifier getModifierBy(T value);
}
