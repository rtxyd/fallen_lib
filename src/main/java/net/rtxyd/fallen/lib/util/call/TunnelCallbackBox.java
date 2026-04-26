package net.rtxyd.fallen.lib.util.call;

import java.util.HashMap;
import java.util.Map;

public abstract class TunnelCallbackBox<T> {

    private final ThreadLocal<Map<T, AnyHandler>> storage = ThreadLocal.withInitial(HashMap::new);

    private Map<T, AnyHandler> getBox() {
        return storage.get();
    }

    public AnyHandler submit(T key, AnyHandler value) {
        return getBox().put(key, value);
    }

    public AnyHandler get(T key) {
        return getBox().get(key);
    }

    public AnyHandler take(T key) {
        return getBox().remove(key);
    }

    public void clear() {
        getBox().clear();
    }

    public void executeAll(Object... args) {
        try {
            storage.get().entrySet().removeIf(e -> e.getValue().invoke(args));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
