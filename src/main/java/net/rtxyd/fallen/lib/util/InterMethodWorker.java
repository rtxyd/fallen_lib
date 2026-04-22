package net.rtxyd.fallen.lib.util;


import net.rtxyd.fallen.lib.util.thread.IExpirationCheck;
import net.rtxyd.fallen.lib.util.thread.InterMethodExecutor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class InterMethodWorker implements ISingleUseAsyncWorker {
    private final InterMethodExecutor EXECUTOR = new InterMethodExecutor();

    private InterMethodWorker(){};

    public static ISingleUseAsyncWorker create() {
        return new InterMethodWorker();
    }

    @Override
    public void addTask(String name, Callable<?> call) {
        try {
            EXECUTOR.submit(name, call);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void addTaskExpire(String name, Callable<?> call, IExpirationCheck checkExpire) {
        try {
            EXECUTOR.submitExpire(name, call, checkExpire);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Use {@link InterMethodWorker#readAndBurn(String, Consumer)}
     */
    @Override
    @Deprecated
    public <T> Optional<T> getResult(String name, Consumer<Exception> processEx) {
        return readAndBurn(name, processEx);
    }

    /**
     * Use {@link InterMethodWorker#readAndBurnOrThrow(String)}
     */
    @Override
    @Deprecated
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

    @Override
    public void shutDown() {
        EXECUTOR.shutdown();
    }
}
