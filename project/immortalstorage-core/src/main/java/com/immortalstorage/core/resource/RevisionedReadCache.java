package com.immortalstorage.core.resource;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small synchronized cache for immutable long-valued read views.
 *
 * <p>Optional storage APIs ask for the same directory more than once while
 * building a network view. The backing storage publishes a revision, so a
 * read can be reused until that revision changes. The loader is invoked while
 * holding the cache monitor; this keeps concurrent callers from observing a
 * partially built view or performing duplicate full scans.</p>
 *
 * @param <S> revision stamp type
 * @param <V> immutable read-view type
 */
public final class RevisionedReadCache<S, V> {
    private S cachedStamp;
    private V cachedValue;

    /** Returns the cached value for {@code stamp}, or loads and publishes it. */
    public synchronized V get(S stamp, Supplier<? extends V> loader) {
        Objects.requireNonNull(stamp, "stamp");
        Objects.requireNonNull(loader, "loader");
        if (cachedValue != null && stamp.equals(cachedStamp)) return cachedValue;

        V loaded = Objects.requireNonNull(loader.get(), "loader returned null");
        cachedStamp = stamp;
        cachedValue = loaded;
        return loaded;
    }

    /** Discards the view after a committed write or ownership transition. */
    public synchronized void invalidate() {
        cachedStamp = null;
        cachedValue = null;
    }
}
