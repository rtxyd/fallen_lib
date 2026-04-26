package net.rtxyd.fallen.lib.util.call;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class ThreadLocalCallBox {

    private final ThreadLocal<Map<ContextKey<?>, Callable<?>>> storage = ThreadLocal.withInitial(HashMap::new);

    private Map<ContextKey<?>, Callable<?>> getCallBox() {
        return storage.get();
    }

    @SuppressWarnings("unchecked")
    public <T> Callable<T> submit(ContextKey<T> key, Callable<T> value) {
        return (Callable<T>) getCallBox().put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Callable<T> get(ContextKey<T> key) {
        return (Callable<T>) getCallBox().get(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getAndCallIfPresent(ContextKey<T> key, Consumer<Exception> handleEx) {
        Callable<T> call = (Callable<T>) getCallBox().get(key);
        if (call != null) {
            try {
                return call.call();
            } catch (Exception e) {
                handleEx.accept(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T takeAndCallIfPresent(ContextKey<T> key, Consumer<Exception> handleEx) {
        Callable<T> call = (Callable<T>) getCallBox().remove(key);
        if (call != null) {
            try {
                return call.call();
            } catch (Exception e) {
                handleEx.accept(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Callable<T> take(ContextKey<Callable<T>> key) {
        return (Callable<T>) getCallBox().remove(key);
    }

    public void remove(ContextKey<?> key) {
        getCallBox().remove(key);
    }

    public void clear() {
        getCallBox().clear();
    }

    public boolean isEmpty() {
        return getCallBox().isEmpty();
    }
}