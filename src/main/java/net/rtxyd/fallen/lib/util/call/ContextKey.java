package net.rtxyd.fallen.lib.util.call;

/**
 * Format: abc.cde.efg
 * @param <T>
 */
public final class ContextKey<T> {

    private final String id;

    private ContextKey(String id) {
        this.id = id;
    }

    static <T> ContextKey<T> create(String id) {
        ContextKeyRegistry.validate(id);
        return new ContextKey<>(id);
    }

    public String getId() {
        return id;
    }
}