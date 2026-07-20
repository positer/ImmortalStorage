package com.cultivation.cultivation.compat.refinedstorage;

import com.cultivation.core.resource.ResourceChannelKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.network.impl.node.grid.GridOperationsImpl;
import com.refinedmods.refinedstorage.api.network.node.grid.GridOperations;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Serializable RS resource type for all Cultivation external channels. */
public final class RsExternalResourceType implements ResourceType {
    public static final RsExternalResourceType INSTANCE = new RsExternalResourceType();
    static final MapCodec<RsExternalResource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("channel").forGetter(key -> key.resource().channel()),
            Codec.STRING.fieldOf("resource").forGetter(key -> key.resource().resourceId())
    ).apply(instance, (channel, resource) ->
            new RsExternalResource(new ResourceChannelKey(channel, resource))));
    static final StreamCodec<RegistryFriendlyByteBuf, RsExternalResource> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public RsExternalResource decode(RegistryFriendlyByteBuf input) {
                    return new RsExternalResource(new ResourceChannelKey(
                            input.readUtf(), input.readUtf()));
                }
                @Override public void encode(RegistryFriendlyByteBuf output, RsExternalResource value) {
                    output.writeUtf(value.resource().channel());
                    output.writeUtf(value.resource().resourceId());
                }
            };
    private static final ResourceLocation FALLBACK_SPRITE = ResourceLocation.fromNamespaceAndPath(
            "refinedstorage", "widget/side_button/resource_type/item");

    private RsExternalResourceType() {}

    @Override public long normalizeAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0D) return 0L;
        return amount >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) amount;
    }
    @Override public double getDisplayAmount(long amount) { return amount; }
    @Override public long getInterfaceExportLimit() { return Long.MAX_VALUE; }
    @Override public GridOperations createGridOperations(RootStorage rootStorage, Actor actor) {
        return new GridOperationsImpl(rootStorage, actor,
                resource -> resource instanceof RsExternalResource ? Long.MAX_VALUE : 0L, 1L);
    }
    @Override @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<PlatformResourceKey> getMapCodec() { return (MapCodec) MAP_CODEC; }
    @Override @SuppressWarnings({"unchecked", "rawtypes"})
    public StreamCodec<RegistryFriendlyByteBuf, PlatformResourceKey> getStreamCodec() {
        return (StreamCodec) STREAM_CODEC;
    }
    @Override public MutableComponent getTitle() {
        return Component.translatable("resource.cultivation.external_resources");
    }
    @Override public ResourceLocation getSprite() { return FALLBACK_SPRITE; }
}
