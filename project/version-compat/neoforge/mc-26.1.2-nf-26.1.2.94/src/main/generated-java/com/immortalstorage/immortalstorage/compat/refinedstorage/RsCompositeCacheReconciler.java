package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.Storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reconciles an RS composite cache while preserving root-list change events. */
public final class RsCompositeCacheReconciler {
    public static void reconcile(MutableResourceList cache, List<Storage> sources) {
        Map<ResourceKey, Long> expected = new LinkedHashMap<>();
        for (Storage source : sources) {
            for (ResourceAmount entry : source.getAll()) {
                expected.merge(entry.resource(), entry.amount(), RsAmountPolicy::saturatedSum);
            }
        }

        for (ResourceAmount current : cache.copyState()) {
            long targetAmount = expected.getOrDefault(current.resource(), 0L);
            if (current.amount() > targetAmount) {
                cache.remove(current.resource(), current.amount() - targetAmount);
            }
        }
        for (Map.Entry<ResourceKey, Long> target : expected.entrySet()) {
            long currentAmount = cache.get(target.getKey());
            if (target.getValue() > currentAmount) {
                cache.add(target.getKey(), target.getValue() - currentAmount);
            }
        }
    }

    private RsCompositeCacheReconciler() {
    }
}
