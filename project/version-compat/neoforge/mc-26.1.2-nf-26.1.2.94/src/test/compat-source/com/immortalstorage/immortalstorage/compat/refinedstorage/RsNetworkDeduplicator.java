package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Elects one stable disk backend per owner and per RS root composite.
 *
 * <p>RS wraps every disk storage in {@link StateTrackedStorage} and then nests
 * a drive-local composite below the network root. The official container item
 * callback does not expose a network ID. The exact-version mixin therefore
 * forwards the stable composite lifecycle and exposes cache rebuilds; this
 * class keeps the policy itself deterministic and independently testable.</p>
 */
public final class RsNetworkDeduplicator {
    private static final Comparator<XianqiaoRsStorage> PRIMARY_ORDER = Comparator
            .comparing(storage -> storage.diskId().toString());

    public static void rebalanceFrom(ParentComposite startingComposite) {
        if (startingComposite == null) return;
        synchronized (startingComposite) {
            Set<ParentComposite> roots = identitySet();
            collectRoots(startingComposite, roots, identitySet());
            for (ParentComposite root : roots) {
                if (root instanceof CompositeStorage composite) {
                    rebalanceRoot(composite);
                }
            }
        }
    }

    static boolean containsOwner(Storage storage, UUID owner) {
        if (storage == null || owner == null) return false;
        Set<Storage> visited = identitySet();
        List<XianqiaoRsStorage> found = new ArrayList<>();
        collectStorages(storage, visited, found);
        return found.stream().anyMatch(candidate -> owner.equals(candidate.owner()));
    }

    private static void rebalanceRoot(CompositeStorage root) {
        List<XianqiaoRsStorage> storages = new ArrayList<>();
        collectStorages(root, identitySet(), storages);
        if (storages.isEmpty()) return;

        Map<UUID, List<XianqiaoRsStorage>> byOwner = new LinkedHashMap<>();
        for (XianqiaoRsStorage storage : storages) {
            byOwner.computeIfAbsent(storage.owner(), ignored -> new ArrayList<>()).add(storage);
        }
        boolean primaryChanged = false;
        for (List<XianqiaoRsStorage> sameOwner : byOwner.values()) {
            XianqiaoRsStorage primary = sameOwner.stream().min(PRIMARY_ORDER).orElseThrow();
            for (XianqiaoRsStorage storage : sameOwner) {
                boolean shouldBePrimary = storage == primary;
                if (storage.isNetworkPrimary() != shouldBePrimary) {
                    storage.setNetworkPrimary(shouldBePrimary);
                    primaryChanged = true;
                }
            }
        }
        if (primaryChanged) {
            rebuildAffectedCaches(storages);
        }
    }

    private static void collectStorages(
            Storage source, Set<Storage> visited, List<XianqiaoRsStorage> result) {
        if (source == null || !visited.add(source)) return;
        Storage unwrapped = source;
        while (unwrapped instanceof StateTrackedStorage tracked) {
            unwrapped = tracked.getDelegate();
            if (!visited.add(unwrapped)) break;
        }
        if (unwrapped instanceof XianqiaoRsStorage xianqiao) {
            result.add(xianqiao);
            return;
        }
        if (unwrapped instanceof CompositeStorage composite) {
            for (Storage child : composite.getSources()) {
                collectStorages(child, visited, result);
            }
        }
    }

    private static void collectRoots(
            ParentComposite current,
            Set<ParentComposite> roots,
            Set<ParentComposite> visited) {
        if (!visited.add(current)) return;
        if (current instanceof RsCompositeCacheAccess access) {
            Set<ParentComposite> parents = access.immortalstorage$getParentComposites();
            if (!parents.isEmpty()) {
                parents.forEach(parent -> collectRoots(parent, roots, visited));
                return;
            }
        }
        roots.add(current);
    }

    private static void rebuildAffectedCaches(List<XianqiaoRsStorage> storages) {
        Set<ParentComposite> composites = identitySet();
        for (XianqiaoRsStorage storage : storages) {
            collectAncestors(storage.directParent(), composites);
        }
        List<ParentComposite> bottomUp = new ArrayList<>(composites);
        bottomUp.sort(Comparator.comparingInt(
                (ParentComposite composite) -> depth(composite)).reversed());
        for (ParentComposite composite : bottomUp) {
            if (composite instanceof RsCompositeCacheAccess access) {
                access.immortalstorage$rebuildCache();
            }
        }
    }

    private static void collectAncestors(
            ParentComposite current, Set<ParentComposite> result) {
        if (current == null || !result.add(current)) return;
        if (current instanceof RsCompositeCacheAccess access) {
            access.immortalstorage$getParentComposites()
                    .forEach(parent -> collectAncestors(parent, result));
        }
    }

    private static int depth(ParentComposite composite) {
        return depth(composite, identitySet());
    }

    private static int depth(
            ParentComposite composite, Set<ParentComposite> visited) {
        if (!visited.add(composite) || !(composite instanceof RsCompositeCacheAccess access)) {
            return 0;
        }
        int parentDepth = access.immortalstorage$getParentComposites().stream()
                .mapToInt(parent -> depth(parent, visited))
                .max()
                .orElse(-1);
        return parentDepth + 1;
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private RsNetworkDeduplicator() {
    }
}
