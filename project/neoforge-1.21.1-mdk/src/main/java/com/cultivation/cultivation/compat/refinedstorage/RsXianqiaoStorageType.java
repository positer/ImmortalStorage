package com.cultivation.cultivation.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/** Registered RS storage type for owner/disk identity persistence. */
final class RsXianqiaoStorageType implements StorageType {
    static final RsXianqiaoStorageType INSTANCE = new RsXianqiaoStorageType();
    private static final UUID UNBOUND = new UUID(0L, 0L);

    private static final MapCodec<XianqiaoRsStorage> STORAGE_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf("owner").forGetter(XianqiaoRsStorage::owner),
                    UUIDUtil.CODEC.fieldOf("disk").forGetter(XianqiaoRsStorage::diskId)
            ).apply(instance, XianqiaoRsStorage::new));

    private RsXianqiaoStorageType() {
    }

    @Override
    public SerializableStorage create(@Nullable Long capacity, Runnable listener) {
        return new XianqiaoRsStorage(UNBOUND, UNBOUND);
    }

    @Override
    public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
        return STORAGE_CODEC.xmap(storage -> storage, storage -> (XianqiaoRsStorage) storage);
    }

    @Override
    public boolean isAllowed(ResourceKey resource) {
        return resource instanceof ItemResource || resource instanceof FluidResource
                || resource instanceof RsExternalResource;
    }

    @Override
    public long getDiskInterfaceTransferQuota(boolean stackUpgrade) {
        return Integer.MAX_VALUE;
    }
}
