package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 26.1 client-item selector for the persistent Spirit Staff mode component.
 *
 * <p>The old 1.21.1 item-property callback was removed with the legacy model
 * override format.  Returning the stable serialized mode name keeps the
 * renderer data-driven and makes the same ItemStack state usable in inventory,
 * hand, creative and recipe-viewer renders.</p>
 */
public record SpiritStaffModeProperty() implements SelectItemModelProperty<String> {
    public static final Type<SpiritStaffModeProperty, String> TYPE = Type.create(
            MapCodec.unit(new SpiritStaffModeProperty()), Codec.STRING);

    @Override
    public String get(ItemStack stack, ClientLevel level, LivingEntity entity,
                      int seed, ItemDisplayContext displayContext) {
        return switch (SpiritStaffItem.getMode(stack)) {
            case SpiritStaffItem.MODE_WRENCH -> "wrench";
            case SpiritStaffItem.MODE_PICK -> "pick";
            case SpiritStaffItem.MODE_BUILD -> "build";
            case SpiritStaffItem.MODE_TELEPORT -> "teleport";
            default -> "explore";
        };
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public Type<SpiritStaffModeProperty, String> type() {
        return TYPE;
    }
}
