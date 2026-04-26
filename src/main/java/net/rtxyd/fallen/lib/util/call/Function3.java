package net.rtxyd.fallen.lib.util.call;

@FunctionalInterface
public interface Function3<P1, P2, P3, R> {
    R accept(P1 p1, P2 p2, P3 p3);
}
