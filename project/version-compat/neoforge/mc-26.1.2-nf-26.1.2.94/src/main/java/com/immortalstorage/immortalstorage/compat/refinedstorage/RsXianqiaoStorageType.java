package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContents;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.core.UUIDUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** RS 3.x storage type using the official StorageContents codec contract. */
final class RsXianqiaoStorageType implements StorageType {
    static final RsXianqiaoStorageType INSTANCE = new RsXianqiaoStorageType();
    private static final UUID UNBOUND = new UUID(0L, 0L);

    private static final Codec<ResourceKey> RESOURCE_CODEC = Codec.either(
            ResourceCodecs.NATIVE_CODEC,
            RsExternalResourceType.MAP_CODEC.codec()
    ).xmap(either -> either.map(resource -> (ResourceKey) resource,
                    resource -> (ResourceKey) resource),
            resource -> resource instanceof RsExternalResource
                    ? Either.<ResourceKey, RsExternalResource>right((RsExternalResource) resource)
                    : Either.<ResourceKey, RsExternalResource>left(resource));

    private static final Codec<StorageContents.Changed> CHANGED_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("by").forGetter(StorageContents.Changed::by),
                    Codec.LONG.fieldOf("at").forGetter(StorageContents.Changed::at)
            ).apply(instance, StorageContents.Changed::new));

    private static final Codec<StorageContents.Stored> STORED_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RESOURCE_CODEC.fieldOf("resource").forGetter(StorageContents.Stored::resource),
                    Codec.LONG.fieldOf("amount").forGetter(StorageContents.Stored::amount),
                    CHANGED_CODEC.optionalFieldOf("changed")
                            .forGetter(StorageContents.Stored::changed)
            ).apply(instance, StorageContents.Stored::new));

    private static final MapCodec<StorageContents> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.LONG.optionalFieldOf("capacity")
                            .forGetter(StorageContents::capacity),
                    STORED_CODEC.listOf().fieldOf("resources")
                            .forGetter(StorageContents::stored)
            ).apply(instance, (capacity, stored) ->
                    new StorageContents(INSTANCE, capacity, stored)));

    private RsXianqiaoStorageType() {
    }

    @Override
    public SerializableStorage create(@Nullable Long capacity, Runnable listener) {
        return new XianqiaoRsStorage(UNBOUND, UNBOUND);
    }

    @Override
    public SerializableStorage create(StorageContents contents, Runnable listener) {
        return new XianqiaoRsStorage(UNBOUND, UNBOUND);
    }

    @Override
    public MapCodec<StorageContents> getCodec() {
        return CODEC;
    }

    @Override
    public boolean isAllowed(ResourceKey resource) {
        return resource instanceof ItemResource || resource instanceof FluidResource
                || resource instanceof RsExternalResource;
    }

    @Override
    public long getDiskInterfaceTransferQuota(boolean stackUpgrade) {
        return Long.MAX_VALUE;
    }
}
