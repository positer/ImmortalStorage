package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** RS external-storage-bus view of only the manager's non-item/non-fluid channels. */
final class XianqiaoRsManagerExternalStorageProvider implements ExternalStorageProvider {
    private final ServerLevel level;
    private final BlockPos targetPos;

    XianqiaoRsManagerExternalStorageProvider(ServerLevel level, BlockPos targetPos, Direction side) {
        this.level = level;
        this.targetPos = targetPos.immutable();
    }

    @Override
    public Iterator<ResourceAmount> iterator() {
        ExternalResourceStorage storage = storage();
        if (storage == null) return List.<ResourceAmount>of().iterator();
        List<ResourceAmount> result = new ArrayList<>();
        storage.snapshot().forEach(entry -> {
            long amount = RsAmountPolicy.advertised(entry.amount());
            ResourceKey key = RsExternalResourceKeyBridges.toRsKey(entry.key());
            if (key != null && amount > 0L) result.add(new ResourceAmount(key, amount));
        });
        return List.copyOf(result).iterator();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        ExternalResourceStorage storage = storage();
        ResourceChannelKey key = RsExternalResourceKeyBridges.toResourceKey(resource);
        if (storage == null || key == null || amount <= 0L || action == null) return 0L;
        return RsAmountPolicy.boundedTransfer(storage.insert(key, amount, transferAction(action)), amount);
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        ExternalResourceStorage storage = storage();
        ResourceChannelKey key = RsExternalResourceKeyBridges.toResourceKey(resource);
        if (storage == null || key == null || amount <= 0L || action == null) return 0L;
        return RsAmountPolicy.boundedTransfer(storage.extract(key, amount, transferAction(action)), amount);
    }

    private ExternalResourceStorage storage() {
        if (!(level.getBlockEntity(targetPos) instanceof XianqiaoManagerBlockEntity manager)) return null;
        PersonalStorageEndpoint endpoint = manager.storageEndpoint();
        return endpoint == null || endpoint.stage() < 8 ? null : endpoint.externalResourceStorage();
    }

    private static ResourceTransferAction transferAction(Action action) {
        return action == Action.SIMULATE
                ? ResourceTransferAction.SIMULATE
                : ResourceTransferAction.EXECUTE;
    }
}
