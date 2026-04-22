package net.rtxyd.fallen.lib.util;

public class MapPutPair<A, B> {
    private final A previous;
    private final B current;

    public MapPutPair(A previous, B current) {
        this.previous = previous;
        this.current = current;
    }


    public A getPrevious() {
        return previous;
    }

    public B getCurrent() {
        return current;
    }
}
