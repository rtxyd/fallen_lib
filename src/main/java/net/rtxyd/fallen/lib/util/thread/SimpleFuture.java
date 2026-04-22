package net.rtxyd.fallen.lib.util.thread;

import net.rtxyd.fallen.lib.util.IThrowableSupplier;

public class SimpleFuture<T> implements IThrowableSupplier<T> {
    private T result;
    private boolean ready = false;
    private boolean cancelled = false;
    private Exception exception;

    public synchronized void setResult(T result) {
        if (ready || cancelled) return;
        this.result = result;
        this.ready = true;
        this.notifyAll();
    }

    public synchronized void setException(Exception e) {
        if (ready || cancelled) return;
        this.exception = e;
        this.ready = true;
        this.notifyAll();
    }

    // block
    public synchronized T get() throws InterruptedException, Exception {
        while (!ready && !cancelled) {
            wait();
        }
        if (cancelled) throw new InterruptedException("Task cancelled");
        if (exception != null) throw exception;
        return result;
    }

    // not block
    public synchronized T tryGet() {
        return ready ? result : null;
    }

    // cancel
    public synchronized void cancel() {
        cancelled = true;
        notifyAll();
    }

    public synchronized boolean isDone() {
        return ready || cancelled;
    }
}