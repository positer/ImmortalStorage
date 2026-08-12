package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.item.custom.SpiritSwordItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritSwordTempering;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts a completed vanilla furnace temper into the next input cycle with no output item. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceSpiritSwordTemperingMixin {
    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private static void immortalstorage$temperSpiritSword(
            
            
            NonNullList<ItemStack> items, ItemStack fuel, ItemStack result,
            
            CallbackInfo cir) {
        ItemStack input = items.get(0);
        if (!(input.getItem() instanceof SpiritSwordItem)) return;
        items.set(0, SpiritSwordTempering.temper(input));
        cir.cancel();
    }
}
