package com.cultivation.cultivation.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.cultivation.compat.ExternalResourceCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Cultivation-owned AE2 identity for an optional resource stored in Xianqiao.
 *
 * <p>The key deliberately contains no target-mod classes. Mod-specific
 * capability adapters translate their native resource to the stable channel
 * and resource id, so the same key remains serializable when another optional
 * integration is absent.</p>
 */
public final class CultivationExternalResourceKey extends AEKey {
    static final MapCodec<CultivationExternalResourceKey> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("channel").forGetter(key -> key.resource.channel()),
                    Codec.STRING.fieldOf("resource").forGetter(key -> key.resource.resourceId()))
                    .apply(instance, CultivationExternalResourceKey::new));

    private final ResourceChannelKey resource;

    public CultivationExternalResourceKey(ResourceChannelKey resource) {
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    private CultivationExternalResourceKey(String channel, String resourceId) {
        this(new ResourceChannelKey(channel, resourceId));
    }

    public ResourceChannelKey resource() {
        return resource;
    }

    @Override
    public AEKeyType getType() {
        return CultivationExternalResourceKeyType.TYPE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("channel", resource.channel());
        tag.putString("resource", resource.resourceId());
        return tag;
    }

    static @Nullable CultivationExternalResourceKey fromTag(CompoundTag tag) {
        if (tag == null || !tag.contains("channel") || !tag.contains("resource")) return null;
        try {
            return new CultivationExternalResourceKey(
                    tag.getString("channel"), tag.getString("resource"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public Object getPrimaryKey() {
        return resource;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.parse(resource.resourceId());
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        data.writeUtf(resource.channel());
        data.writeUtf(resource.resourceId());
    }

    static CultivationExternalResourceKey fromPacket(RegistryFriendlyByteBuf data) {
        return new CultivationExternalResourceKey(data.readUtf(), data.readUtf());
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
                || other instanceof CultivationExternalResourceKey key
                && resource.equals(key.resource);
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public String toString() {
        return "CultivationExternalResourceKey[" + resource + "]";
    }
}
