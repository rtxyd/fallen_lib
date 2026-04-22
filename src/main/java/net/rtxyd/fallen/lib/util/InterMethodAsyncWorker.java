package net.rtxyd.fallen.lib.util;


import net.rtxyd.fallen.lib.util.thread.IExpirationCheck;
import net.rtxyd.fallen.lib.util.thread.InterMethodExecutor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class InterMethodAsyncWorker implements ISingleUseAsyncWorker {
    private final InterMethodExecutor EXECUTOR = new InterMethodExecutor();

    @Override
    public void addTask(String name, Callable<?> call) throws InterruptedException {
        EXECUTOR.submit(name, call);
    }

    @Override
    public void addTaskExpire(String name, Callable<?> call, IExpirationCheck checkExpire) throws InterruptedException {
        EXECUTOR.submitExpire(name, call, checkExpire);
    }

    @Override
    public <T> Optional<T> getResult(String name, Consumer<Exception> processEx) {
        return readAndBurn(name, processEx);
    }

    @Override
    public <T> T getResultOrThrow(String id) {
        return readAndBurnOrThrow(id);
    }

    @Override
    public <T> Optional<T> readAndBurn(String id, Consumer<Exception> processEx) {
        IThrowableSupplier<T> sup = EXECUTOR.get(id);
        try {
            if (sup == null) return Optional.empty();
            return Optional.ofNullable(sup.get());
        } catch (Exception e) {
            processEx.accept(e);
        }
        return Optional.empty();
    }

    @Override
    public <T> T readAndBurnOrThrow(String id) {
        IThrowableSupplier<T> sup = EXECUTOR.get(id);
        if (sup == null) {
            throw new NoSuchElementException("No task named: " + id);
        }
        try {
            return sup.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
