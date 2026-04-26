package net.rtxyd.fallen.lib.util.call;

import java.util.HashMap;
import java.util.Map;

public class TunnelCallbackBox2<T, P1, P2> {

    private final ThreadLocal<Map<T, IHandler2<P1, P2>>> storage = ThreadLocal.withInitial(HashMap::new);

    protected Map<T, IHandler2<P1, P2>> getBox() {
        return storage.get();
    }

    public IHandler2<P1, P2> submit(T key, IHandler2<P1, P2> value) {
        return getBox().put(key, value);
    }

    public IHandler2<P1, P2> get(T key) {
        return getBox().get(key);
    }

    public IHandler2<P1, P2> take(T key) {
        return getBox().remove(key);
    }

    public void clear() {
        getBox().clear();
    }

    public void executeAll(P1 p1, P2 p2) {
        try {
            storage.get().entrySet().removeIf(e -> e.getValue().test(p1, p2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
