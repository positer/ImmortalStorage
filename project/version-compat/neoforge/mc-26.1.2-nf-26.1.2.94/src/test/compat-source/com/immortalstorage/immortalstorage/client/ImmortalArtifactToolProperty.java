package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 26.1 client-item selector for the Immortal Artifact's adaptive mining tool.
 *
 * <p>1.21.1 exposes this as the float {@code immortalstorage:artifact_tool}
 * predicate ({@code SpiritStaffItem.miningVisual}). The legacy predicate API is
 * gone in 26.1, so the same selection is expressed as a named select property:
 * the artifact keeps the pickaxe texture by default and follows the crosshair
 * target for axe, shovel, hoe and shears just like the canonical lane.</p>
 */
public record ImmortalArtifactToolProperty() implements SelectItemModelProperty<String> {
    public static final Type<ImmortalArtifactToolProperty, String> TYPE = Type.create(
            MapCodec.unit(new ImmortalArtifactToolProperty()), Codec.STRING);

    @Override
    public String get(ItemStack stack, ClientLevel level, LivingEntity entity,
                      int seed, ItemDisplayContext displayContext) {
        if (level == null) {
            return "pick";
        }
        net.minecraft.world.phys.HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof net.neoforged.neoforge.common.IShearable) {
            return "shears";
        }
        if (!(hit instanceof BlockHitResult blockHit)) {
            return "pick";
        }
        return switch (SpiritStaffItem.miningVisual(stack, level.getBlockState(blockHit.getBlockPos()))) {
            case 1 -> "axe";
            case 2 -> "shovel";
            case 3 -> "hoe";
            case 4 -> "shears";
            default -> "pick";
        };
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public Type<ImmortalArtifactToolProperty, String> type() {
        return TYPE;
    }
}
