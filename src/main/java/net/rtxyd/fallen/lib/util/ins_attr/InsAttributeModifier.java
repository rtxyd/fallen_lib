package net.rtxyd.fallen.lib.util.ins_attr;

public class InsAttributeModifier {
    private final Type type;
    private final String name;
    private final float value;
    public static final InsAttributeModifier EMPTY = new InsAttributeModifier(Type.NONE, "none", 0);

    public InsAttributeModifier(Type type, String name, float value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public Type getType() {
        return type;
    }
    public float getValue() {
        return value;
    }
    public String getName() {
        return name;
    }
    public boolean isEmpty() {
        return type == Type.NONE || value == 0;
    }

    public static enum Type {
        NONE,
        MULTIPLY_BASE,
        ADD_BASE,
        SET_BASE,
        MULTIPLY_FINAL,
        ADD_FINAL,
        SET_FINAL
    }
}
