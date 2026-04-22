package net.rtxyd.fallen.lib.util;

import net.rtxyd.fallen.lib.util.thread.IExpirationCheck;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface IAsyncWorker {

    <T> Optional<T> getResult(String name, Consumer<Exception> processEx);

    <T> T getResultOrThrow(String id);

    void addTask(String id, Callable<?> call);

    void addTaskExpire(String id, Callable<?> call, IExpirationCheck checkExpire);

    void shutDown();
}
