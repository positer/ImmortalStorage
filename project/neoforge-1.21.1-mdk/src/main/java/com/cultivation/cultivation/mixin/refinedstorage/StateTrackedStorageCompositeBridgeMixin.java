package com.cultivation.cultivation.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * RS 2.0.9 wraps every disk before adding it to a composite. Forwarding the
 * stable CompositeAwareChild lifecycle lets the exchange backend see its
 * drive/network composite without altering ordinary RS storage semantics.
 */
@Mixin(value = StateTrackedStorage.class, remap = false)
public abstract class StateTrackedStorageCompositeBridgeMixin implements CompositeAwareChild {
    @Shadow
    @Final
    private Storage delegate;

    @Override
    public void onAddedIntoComposite(ParentComposite parentComposite) {
        if (delegate instanceof CompositeAwareChild child) {
            child.onAddedIntoComposite(parentComposite);
        }
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parentComposite) {
        if (delegate instanceof CompositeAwareChild child) {
            child.onRemovedFromComposite(parentComposite);
        }
    }

    @Override
    public Amount compositeInsert(
            ResourceKey resource, long amount, Action action, Actor actor) {
        if (delegate instanceof CompositeAwareChild child) {
            return child.compositeInsert(resource, amount, action, actor);
        }
        StateTrackedStorage self = (StateTrackedStorage) (Object) this;
        long inserted = self.insert(resource, amount, action, actor);
        return inserted <= 0L ? Amount.ZERO : new Amount(inserted, inserted);
    }

    @Override
    public Amount compositeExtract(
            ResourceKey resource, long amount, Action action, Actor actor) {
        if (delegate instanceof CompositeAwareChild child) {
            return child.compositeExtract(resource, amount, action, actor);
        }
        StateTrackedStorage self = (StateTrackedStorage) (Object) this;
        long extracted = self.extract(resource, amount, action, actor);
        return extracted <= 0L ? Amount.ZERO : new Amount(extracted, extracted);
    }

    @Override
    public boolean contains(Storage storage) {
        return delegate instanceof CompositeAwareChild child && child.contains(storage);
    }
}
