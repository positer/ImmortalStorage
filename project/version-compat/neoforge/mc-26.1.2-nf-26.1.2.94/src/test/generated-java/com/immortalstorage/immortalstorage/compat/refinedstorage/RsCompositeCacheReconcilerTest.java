package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.resource.list.listenable.ListenableResourceList;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RsCompositeCacheReconcilerTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final ResourceKey RESOURCE = new TestResource("shared");

    @Test
    void duplicateDeactivationPublishesOneRemovalDeltaInsteadOfSilentClearAndReadd() {
        ListenableResourceList cache = new ListenableResourceList(MutableResourceListImpl.create());
        cache.add(RESOURCE, 200L);
        List<MutableResourceList.OperationResult> changes = new java.util.ArrayList<>();
        cache.addListener(changes::add);

        RsCompositeCacheReconciler.reconcile(cache, List.of(new FixedStorage(RESOURCE, 100L)));

        assertEquals(100L, cache.get(RESOURCE));
        assertEquals(1, changes.size());
        assertEquals(-100L, changes.getFirst().change());
        assertEquals(100L, changes.getFirst().amount());
    }

    @Test
    void unchangedCacheDoesNotPublishSpuriousListenerEvents() {
        ListenableResourceList cache = new ListenableResourceList(MutableResourceListImpl.create());
        cache.add(RESOURCE, 100L);
        List<MutableResourceList.OperationResult> changes = new java.util.ArrayList<>();
        cache.addListener(changes::add);

        RsCompositeCacheReconciler.reconcile(cache, List.of(new FixedStorage(RESOURCE, 100L)));

        assertEquals(100L, cache.get(RESOURCE));
        assertEquals(List.of(), changes);
    }

    @Test
    void expectedSnapshotSaturatesInsteadOfOverflowingAcrossSources() {
        MutableResourceList cache = MutableResourceListImpl.create();

        RsCompositeCacheReconciler.reconcile(cache, List.of(
                new FixedStorage(RESOURCE, Long.MAX_VALUE - 5L),
                new FixedStorage(RESOURCE, 10L)));

        assertEquals(Long.MAX_VALUE, cache.get(RESOURCE));
    }

    private record TestResource(String id) implements ResourceKey {
    }

    private record FixedStorage(ResourceKey resource, long amount) implements Storage {
        @Override
        public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
            return 0L;
        }

        @Override
        public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
            return 0L;
        }

        @Override
        public Collection<ResourceAmount> getAll() {
            return List.of(new ResourceAmount(resource, amount));
        }

        @Override
        public long getStored() {
            return amount;
        }
    }
}
