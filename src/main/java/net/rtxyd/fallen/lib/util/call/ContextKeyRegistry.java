package net.rtxyd.fallen.lib.util.call;

import java.util.HashMap;
import java.util.Map;

public final class ContextKeyRegistry {

    private final Map<String, ContextKey<?>> REGISTRY = new HashMap<>();

    public <T> ContextKey<T> register(String id) {
        validate(id);
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicated ContextKey: " + id);
        }
        ContextKey<T> key = ContextKey.create(id);
        REGISTRY.put(id, key);
        return key;
    }

    @SuppressWarnings("unchecked")
    public <T> ContextKey<T> get(String id) {
        return (ContextKey<T>) REGISTRY.get(id);
    }

    public static void validate(String id) {
        String[] parts = id.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid ContextKey: " + id);
        }
        for (String p : parts) {
            if (p.isEmpty()) {
                throw new IllegalArgumentException("Empty segment: " + id);
            }
        }
    }
}