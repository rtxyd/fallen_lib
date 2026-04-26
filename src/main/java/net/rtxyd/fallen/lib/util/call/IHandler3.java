package net.rtxyd.fallen.lib.util.call;

@FunctionalInterface
public interface IHandler3<P1, P2, P3> {
    boolean test(P1 p1,P2 p2, P3 p3);
}
