package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Category icons carry real controller polarity; catalyst lookup still uses the ordinary item. */
public final class SpiritCompressorDisplayItems {
    public static ItemStack controllerStack(boolean reversed) {
        ItemStack stack = new ItemStack(ModItems.MINIATURE_IMMORTAL_RUIN.get());
        CompoundTag tag = new CompoundTag(); tag.putBoolean("Reversed", reversed);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
    private SpiritCompressorDisplayItems() {}
}
