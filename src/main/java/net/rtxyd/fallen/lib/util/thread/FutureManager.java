package net.rtxyd.fallen.lib.util.thread;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FutureManager {
    private final Map<String, SimpleFuture<?>> futureMap = new ConcurrentHashMap<>(16);

    public SimpleFuture<?> createFuture(String taskId) {
        if (futureMap.size() >= 15) throw new IndexOutOfBoundsException("Too many items! Currently it only supports max 16.");
        SimpleFuture<?> future = new SimpleFuture<>();
        futureMap.put(taskId, future);
        return future;
    }

    @SuppressWarnings("unchecked")
    @Nullable <R> SimpleFuture<R> getFuture(String taskId) {
        return (SimpleFuture<R>) futureMap.get(taskId);
    }

    public boolean containsFuture(String taskId) {
        return futureMap.containsKey(taskId);
    }

    @SuppressWarnings("unchecked")
    @Nullable public <R> SimpleFuture<R> getAndRemoveFuture(String taskId) {
        // remove to avoid leaking
        return (SimpleFuture<R>) futureMap.remove(taskId);
    }

    @SuppressWarnings("unchecked")
    public <R> void setResult(String taskId, R result) {
        SimpleFuture<R> future = (SimpleFuture<R>) futureMap.get(taskId);
        if (future != null) {
            future.setResult(result);
        }
    }

    public void setException(String taskId, Exception e) {
        SimpleFuture<?> future = futureMap.get(taskId);
        if (future != null) {
            future.setException(e);
        }
    }

    public void removeFuture(String taskId) {
        futureMap.remove(taskId);
    }
}