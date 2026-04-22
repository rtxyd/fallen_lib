package net.rtxyd.fallen.lib.util.ins_attr;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class AInsAttribute<I> {
    private final I instance;
    private Map<String, InsAttributeModifier> modifiers;
    protected float initBase;
    protected float initFinal;
    private float multiplyBase;
    private float addBase;
    private float setBase;
    private float multiplyFinal;
    private float addFinal;
    private float setFinal;

    public AInsAttribute(I instance, Map<String, InsAttributeModifier> modifiers, float initBase, float initFinal) {
        this.instance = instance;
        this.modifiers = modifiers;
        this.initBase = initBase;
        this.initFinal = initFinal;
        this.multiplyBase = 1.0f;
        this.addBase = 0;
        this.setBase = initBase;
        this.multiplyFinal = 1.0f;
        this.addFinal = 0;
        this.setFinal = initFinal;
    }

    public void reset() {
        this.multiplyBase = 1.0f;
        this.addBase = 0;
        this.setBase = initBase;
        this.multiplyFinal = 1.0f;
        this.addFinal = 0;
        this.setFinal = initFinal;
        clearModifier();
    }

    public final void addModifier(String name, InsAttributeModifier modifier) {
        modifiers.put(name, modifier);
    }

    public final InsAttributeModifier removeModifier(String name) {return modifiers.remove(name);}

    public final Map<String, InsAttributeModifier> getModifiers() {
        return modifiers;
    }

    protected final void clearModifier() {
        modifiers = new HashMap<>();
    }

    protected final void calc(InsAttributeModifier modifier) {
        float value = modifier.getValue();
        switch (modifier.getType()) {
            case MULTIPLY_BASE -> {
                this.calcMultiplyBase(value);
            }
            case ADD_BASE -> {
                this.calcAddBase(value);
            }
            case SET_BASE -> {
                this.calcSetBase(value);
            }
            case MULTIPLY_FINAL -> {
                this.calcMultiplyFinal(value);
            }
            case ADD_FINAL -> {
                this.calcAddFinal(value);
            }
            case SET_FINAL -> {
                this.calcSetFinal(value);
            }
            case NONE -> {}
            default -> {
                throw new UnsupportedOperationException("Attempt to add null modifier!");
            }
        }
    }

    public I output(BiFunction<I, Float, I> function) {
        modifiers.values().forEach(this::calc);
        computeFinal();
        return function.apply(instance, setFinal);
    }

    protected final void computeFinal() {
        calcSetBase(initBase * multiplyBase + addBase);
        calcSetFinal(setBase * multiplyFinal + addFinal);
    }

    protected final void calcMultiplyBase(float value) {
        this.multiplyBase += value;
    }

    protected final void calcAddBase(float value) {
        this.addBase += value;
    }

    protected final void calcSetBase(float value) {
        if (this.setBase < value) {
            this.setBase = value;
        }
    }

    protected final void calcMultiplyFinal(float value) {
        this.multiplyFinal += value;
    }

    protected final void calcAddFinal(float value) {
        this.addFinal += value;
    }

    protected final void calcSetFinal(float value) {
        if (this.setFinal < value) {
            this.setFinal = value;
        }
    }
}