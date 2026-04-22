package net.rtxyd.fallen.lib.util.thread;

import net.rtxyd.fallen.lib.FallenCoreLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class InterMethodExecutor {
    private SimpleArrayBlockingQueue<Task<?>> taskQueue = new SimpleArrayBlockingQueue<>(15);
    private SimpleArrayBlockingQueue<Guard> guardQueue = new SimpleArrayBlockingQueue<>(15);
    private FutureManager manager = new FutureManager();
    private final Thread worker;
    private final Thread guard;
    private volatile boolean running = true;

    public InterMethodExecutor() {
        worker = new Thread(() -> {
            while (running) {
                try {
                    Task<?> task = taskQueue.take();
                    executeTask(task);
                } catch (InterruptedException e) {
                    if (!running) {
                        break;
                    }
                } catch (Throwable e) {
                    FallenCoreLib.LOGGER.debug("Unexpected worker exception: ", e);
                    if (!running) {
                        break;
                    }
                }
            }
        }, "fallen.lib.worker");
        guard = new Thread(() -> {
            while (running) {
                try {
                    List<Guard> pending = new ArrayList<>();
                    guardQueue.drainTo(pending);

                    List<Guard> retryList = new ArrayList<>();
                    for (Guard g : pending) {
                        if (executeGuard(g)) {
                            manager.removeFuture(g.id);
                        } else {
                            retryList.add(g);
                        }
                    }

                    for (Guard g : retryList) {
                        guardQueue.put(g);
                    }

                    guardQueue.waitA(50);
                } catch (InterruptedException e) {
                    if (!running) {
                        break;
                    }
                } catch (Throwable e) {
                    FallenCoreLib.LOGGER.debug("Unexpected worker exception: ", e);
                    if (!running) {
                        break;
                    }
                }
            }
        }, "fallen.lib.guard");
        guard.setDaemon(true);
        worker.setDaemon(true);
        guard.start();
        worker.start();
    }

    private <R> void executeTask(Task<R> task) {
        try {
            R result = compute(task.input);
            task.future.setResult(result);
        } catch (Exception e) {
            task.future.setException(e);
        }
    }

    private boolean executeGuard(Guard guard) {
        try {
            return guard.checkExpire.isExpired();
        } catch (Exception e) {
            SimpleFuture<?> future = manager.getFuture(guard.id);
            if (future != null) {
                future.setException(e);
            }
            return true;
        }
    }

    private <R> R compute(Callable<R> input) throws Exception {
        R result;
        result = input.call();
        return result;
    }

    @SuppressWarnings("unchecked")
    public <R> SimpleFuture<R> submit(String id, Callable<R> input) throws InterruptedException {
        SimpleFuture<R> future = (SimpleFuture<R>) manager.createFuture(id);
        Task<R> task = new Task<>(input, future);
        taskQueue.put(task);
        return future;
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<SimpleFuture<R>> submitExpire(String id, Callable<R> input, IExpirationCheck checkExpire) throws InterruptedException {
        if (manager.containsFuture(id)) return Optional.empty();
        SimpleFuture<R> future = (SimpleFuture<R>) manager.createFuture(id);
        Task<R> task = new Task<>(input, future);
        Guard guard = new Guard(id, checkExpire);
        taskQueue.put(task);
        guardQueue.put(guard);
        return Optional.ofNullable(future);
    }

    public <R> SimpleFuture<R> get(String id) {
        return manager.getAndRemoveFuture(id);
    }

    public void shutdown() {
        running = false;

        worker.interrupt();
        guard.interrupt();

        try {
            worker.join();
            guard.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        manager = null;
        taskQueue = null;
        guardQueue = null;
    }

    class Task<R> {
        final Callable<R> input;
        final SimpleFuture<R> future;

        Task(Callable<R> input, SimpleFuture<R> future) {
            this.input = input;
            this.future = future;
        }
    }

    class Guard {
        final String id;
        final IExpirationCheck checkExpire;

        Guard(String id, IExpirationCheck checkExpire) {
            this.id = id;
            this.checkExpire = checkExpire;
        }
    }
}