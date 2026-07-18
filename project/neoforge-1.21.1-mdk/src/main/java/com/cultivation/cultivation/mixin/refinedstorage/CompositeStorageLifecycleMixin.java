package com.cultivation.cultivation.mixin.refinedstorage;

import com.cultivation.cultivation.compat.refinedstorage.RsCompositeCacheAccess;
import com.cultivation.cultivation.compat.refinedstorage.RsCompositeCacheReconciler;
import com.cultivation.cultivation.compat.refinedstorage.RsNetworkDeduplicator;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

/** Exact-RS-2.0.9 composite lifecycle/cache bridge for owner deduplication. */
@Mixin(value = CompositeStorageImpl.class, remap = false)
public abstract class CompositeStorageLifecycleMixin implements RsCompositeCacheAccess {
    @Shadow
    @Final
    private List<Storage> insertSources;

    @Shadow
    @Final
    private MutableResourceList list;

    @Shadow
    @Final
    private Set<ParentComposite> parentComposites;

    @Override
    public Set<ParentComposite> cultivation$getParentComposites() {
        return Set.copyOf(parentComposites);
    }

    @Override
    public void cultivation$rebuildCache() {
        RsCompositeCacheReconciler.reconcile(list, insertSources);
    }

    @Inject(method = "addSource", at = @At("TAIL"))
    private void cultivation$afterSourceAdded(Storage source, CallbackInfo callback) {
        RsNetworkDeduplicator.rebalanceFrom((ParentComposite) (Object) this);
    }

    @Inject(method = "removeSource", at = @At("TAIL"))
    private void cultivation$afterSourceRemoved(Storage source, CallbackInfo callback) {
        RsNetworkDeduplicator.rebalanceFrom((ParentComposite) (Object) this);
    }

    @Inject(method = "onAddedIntoComposite", at = @At("TAIL"))
    private void cultivation$afterAttachedToParent(
            ParentComposite parent, CallbackInfo callback) {
        RsNetworkDeduplicator.rebalanceFrom(parent);
    }

    @Inject(method = "onRemovedFromComposite", at = @At("TAIL"))
    private void cultivation$afterDetachedFromParent(
            ParentComposite formerParent, CallbackInfo callback) {
        RsNetworkDeduplicator.rebalanceFrom(formerParent);
        RsNetworkDeduplicator.rebalanceFrom((ParentComposite) (Object) this);
    }
}
