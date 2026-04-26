package net.rtxyd.fallen.lib.util.call;

@FunctionalInterface
public interface AnyHandler {
    boolean invoke(Object... args);
}