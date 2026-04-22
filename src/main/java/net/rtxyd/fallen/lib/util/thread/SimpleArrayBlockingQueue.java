package net.rtxyd.fallen.lib.util.thread;

import java.util.Collection;

public class SimpleArrayBlockingQueue<T> {
    private final Object[] items;
    private int capacity;
    private int putIndex;
    private int takeIndex;
    private int count;
    private final Object lock = new Object();

    public SimpleArrayBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.items = new Object[capacity];
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        synchronized (lock) {
            while (count == items.length) {
                lock.wait();
            }
            items[putIndex] = item;
            if (++putIndex == items.length) putIndex = 0;
            count++;
            lock.notifyAll();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        synchronized (lock) {
            while (count == 0) {
                lock.wait();
            }
            T item = (T) items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length) takeIndex = 0;
            count--;
            lock.notifyAll();
            return item;
        }
    }

    public int getCount() {
        return count;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean offer(T item) {
        synchronized (lock) {
            if (count == items.length) return false;
            items[putIndex] = item;
            if (++putIndex == items.length) putIndex = 0;
            count++;
            lock.notifyAll();
            return true;
        }
    }

    public void waitA(long timeoutMillis) throws InterruptedException {
        synchronized (lock) {
            lock.wait(timeoutMillis);
        }
    }

    @SuppressWarnings("unchecked")
    public int drainTo(Collection<T> c) {
        synchronized (lock) {
            int drained = 0;
            while (count > 0) {
                T item = (T) items[takeIndex];
                items[takeIndex] = null;
                if (++takeIndex  == items.length) takeIndex = 0;
                count--;
                c.add(item);
                drained++;
            }
            lock.notifyAll();
            return drained;
        }
    }
}