package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** Official RS 3.x grid-type registration for ImmortalStorage external keys. */
final class RsExternalGridResourceType implements GridResourceType {
    static final RsExternalGridResourceType INSTANCE = new RsExternalGridResourceType();

    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath(
            "refinedstorage", "widget/side_button/resource_type/item");

    private static final MapCodec<GridResource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("channel")
                    .forGetter(RsExternalGridResourceType::channel),
            com.mojang.serialization.Codec.STRING.fieldOf("resource")
                    .forGetter(RsExternalGridResourceType::resourceId)
    ).apply(instance, (channel, resourceId) -> RsExternalGridResourceMapper.INSTANCE.apply(
            new RsExternalResource(new ResourceChannelKey(channel, resourceId)))));

    private RsExternalGridResourceType() {
    }

    @Override
    public GridResource apply(ResourceKey resource) {
        return RsExternalGridResourceMapper.INSTANCE.apply(resource);
    }

    @Override
    public MapCodec<GridResource> getMapCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getTitle() {
        return Component.translatable("resource.immortalstorage.external_resources");
    }

    @Override
    public Identifier getSprite() {
        return SPRITE;
    }

    @Override
    public Class<? extends ResourceKey> getResourceType() {
        return RsExternalResource.class;
    }

    private static String channel(GridResource resource) {
        return ((RsExternalGridResource) resource).externalResource().resource().channel();
    }

    private static String resourceId(GridResource resource) {
        return ((RsExternalGridResource) resource).externalResource().resource().resourceId();
    }
}
