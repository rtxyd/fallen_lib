package net.rtxyd.fallen.lib.util.thread;

@FunctionalInterface
public interface IExpirationCheck {
    boolean isExpired() throws Exception;
}