package net.rtxyd.fallen.lib.util;

import java.util.HashMap;
import java.util.Map;

public final class ObjectCaky {

    private final Map<String, CakyEntry<?, ?>> entries = new HashMap<>();

    @SuppressWarnings({"unchecked", "Synchronization", "SynchronizationOnLocalVariableOrMethodParameter"})
    public <S, T> T resolve(S obj, String key, CakyLoader<S, T> loader, CakyReviewer<S> reviewer) {
        CakyEntry<S, T> entry = (CakyEntry<S, T>) entries.get(key);

        if (entry == null) {
            synchronized (this) {
                entry = (CakyEntry<S, T>) entries.get(key);
                if (entry == null) {
                    T value = loader.load(obj);
                    entry = new CakyEntry<>(value, reviewer.review(obj), loader, reviewer);
                    entries.put(key, entry);
                }
            }
        } else {
            int newFingerprint = entry.reviewer.review(obj);
            if (entry.fingerprint != newFingerprint) {
                synchronized (entry) {
                    if (entry.fingerprint != newFingerprint) {
                        entry.value = entry.loader.load(obj);
                        entry.fingerprint = entry.reviewer.review(obj);
                    }
                }
            }
        }
        return entry.value;
    }

    static final class CakyEntry<S, T> {
        volatile T value;
        volatile int fingerprint;
        CakyLoader<S, T> loader;
        CakyReviewer<S> reviewer;

        CakyEntry(T value, int fingerprint, CakyLoader<S, T> loader, CakyReviewer<S> reviewer) {
            this.value = value;
            this.fingerprint = fingerprint;
            this.loader = loader;
            this.reviewer = reviewer;
        }
    }

    @FunctionalInterface
    public static interface CakyLoader<S, T> {
        T load(S obj);
    }

    @FunctionalInterface
    public static interface CakyReviewer<S> {
        int review(S obj);
    }
}