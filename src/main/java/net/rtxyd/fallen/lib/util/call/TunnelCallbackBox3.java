package net.rtxyd.fallen.lib.util.call;

import java.util.HashMap;
import java.util.Map;

public class TunnelCallbackBox3<T, P1, P2, P3> {

    private final ThreadLocal<Map<T, IHandler3<P1, P2, P3>>> storage = ThreadLocal.withInitial(HashMap::new);

    protected Map<T, IHandler3<P1, P2, P3>> getBox() {
        return storage.get();
    }

    public IHandler3<P1, P2, P3> submit(T key, IHandler3<P1, P2, P3> value) {
        return getBox().put(key, value);
    }

    public IHandler3<P1, P2, P3> get(T key) {
        return getBox().get(key);
    }

    public IHandler3<P1, P2, P3> take(T key) {
        return getBox().remove(key);
    }

    public void clear() {
        getBox().clear();
    }

    public void executeAll(P1 p1, P2 p2, P3 p3) {
        try {
            storage.get().entrySet().removeIf(e -> e.getValue().test(p1, p2, p3));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
