package net.rtxyd.fallen.lib.util.call;

@FunctionalInterface
public interface IHandler2<P1, P2> {
    boolean test(P1 p1,P2 p2);
}
