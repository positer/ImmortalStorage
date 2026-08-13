package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import net.minecraft.core.component.DataComponentType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * ImmortalStorage-owned AE2 identity for an optional resource stored in Xianqiao.
 *
 * <p>The key deliberately contains no target-mod classes. Mod-specific
 * capability adapters translate their native resource to the stable channel
 * and resource id, so the same key remains serializable when another optional
 * integration is absent.</p>
 */
public final class ImmortalStorageExternalResourceKey extends AEKey {
    static final MapCodec<ImmortalStorageExternalResourceKey> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("channel").forGetter(key -> key.resource.channel()),
                    Codec.STRING.fieldOf("resource").forGetter(key -> key.resource.resourceId()))
                    .apply(instance, ImmortalStorageExternalResourceKey::new));

    private final ResourceChannelKey resource;

    public ImmortalStorageExternalResourceKey(ResourceChannelKey resource) {
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    private ImmortalStorageExternalResourceKey(String channel, String resourceId) {
        this(new ResourceChannelKey(channel, resourceId));
    }

    public ResourceChannelKey resource() {
        return resource;
    }

    @Override
    public AEKeyType getType() {
        return ImmortalStorageExternalResourceKeyType.TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public void toTag(ValueOutput output) {
        output.putString("channel", resource.channel());
        output.putString("resource", resource.resourceId());
    }

    static @Nullable ImmortalStorageExternalResourceKey fromTag(ValueInput input) {
        String channel = input.getString("channel").orElse(null);
        String resourceId = input.getString("resource").orElse(null);
        if (channel == null || resourceId == null) return null;
        try {
            return new ImmortalStorageExternalResourceKey(channel, resourceId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public Object getPrimaryKey() {
        return resource;
    }

    @Override
    public Identifier getId() {
        return Identifier.parse(resource.resourceId());
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        data.writeUtf(resource.channel());
        data.writeUtf(resource.resourceId());
    }

    static ImmortalStorageExternalResourceKey fromPacket(RegistryFriendlyByteBuf data) {
        return new ImmortalStorageExternalResourceKey(data.readUtf(), data.readUtf());
    }

    @Override
    protected Component computeDisplayName() {
        return ExternalResourceCatalog.displayName(resource);
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // Non-item resources have no lossless world-drop representation.
    }

    @Override
    public boolean isTagged(TagKey<?> tag) {
        return false;
    }

    @Override
    public <T> @Nullable T get(DataComponentType<T> type) {
        return null;
    }

    @Override
    public boolean hasComponents() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof ImmortalStorageExternalResourceKey key
                && resource.equals(key.resource);
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public String toString() {
        return "ImmortalStorageExternalResourceKey[" + resource + "]";
    }
}
