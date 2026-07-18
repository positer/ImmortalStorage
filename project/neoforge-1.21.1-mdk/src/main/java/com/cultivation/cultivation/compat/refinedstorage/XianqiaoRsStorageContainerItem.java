package com.cultivation.cultivation.compat.refinedstorage;

import com.cultivation.cultivation.item.custom.XianqiaoRsExchangeDiskItem;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.common.api.storage.StorageRepository;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** RS-loaded item implementation accepted by the official Disk Drive validator. */
public final class XianqiaoRsStorageContainerItem
        extends XianqiaoRsExchangeDiskItem implements StorageContainerItem {
    public XianqiaoRsStorageContainerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Optional<SerializableStorage> resolve(
            StorageRepository storageRepository, ItemStack stack) {
        Optional<UUID> owner = owner(stack);
        Optional<UUID> disk = diskId(stack);
        if (owner.isEmpty() || disk.isEmpty()) return Optional.empty();
        return Optional.of(new XianqiaoRsStorage(owner.get(), disk.get()));
    }

    @Override
    public Optional<StorageInfo> getInfo(StorageRepository storageRepository, ItemStack stack) {
        return resolve(storageRepository, stack).map(StorageInfo::of);
    }
}
