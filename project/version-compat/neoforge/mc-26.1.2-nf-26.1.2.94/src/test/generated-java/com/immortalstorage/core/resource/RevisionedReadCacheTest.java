package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RevisionedReadCacheTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void reusesOneLongReadUntilRevisionChangesAndSupportsExplicitInvalidation() {
        RevisionedReadCache<Long, Long> cache = new RevisionedReadCache<>();
        AtomicInteger loads = new AtomicInteger();

        assertEquals(Long.MAX_VALUE,
                cache.get(7L, () -> {
                    loads.incrementAndGet();
                    return Long.MAX_VALUE;
                }));
        assertEquals(Long.MAX_VALUE, cache.get(7L, () -> {
            loads.incrementAndGet();
            return 1L;
        }));
        assertEquals(1, loads.get());

        assertEquals(2L, cache.get(8L, () -> {
            loads.incrementAndGet();
            return 2L;
        }));
        assertEquals(2, loads.get());

        cache.invalidate();
        assertEquals(3L, cache.get(8L, () -> {
            loads.incrementAndGet();
            return 3L;
        }));
        assertEquals(3, loads.get());
    }
}
