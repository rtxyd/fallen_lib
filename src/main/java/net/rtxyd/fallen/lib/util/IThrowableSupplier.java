package net.rtxyd.fallen.lib.util;

public interface IThrowableSupplier<T> {
    T get() throws Exception;
}
