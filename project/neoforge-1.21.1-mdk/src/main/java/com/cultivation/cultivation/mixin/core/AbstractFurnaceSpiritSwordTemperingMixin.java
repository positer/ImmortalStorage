package com.cultivation.cultivation.mixin.core;

import com.cultivation.cultivation.item.custom.SpiritSwordItem;
import com.cultivation.cultivation.item.custom.SpiritSwordTempering;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts a completed vanilla furnace temper into the next input cycle with no output item. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceSpiritSwordTemperingMixin {
    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private static void cultivation$temperSpiritSword(
            net.minecraft.core.RegistryAccess registries,
            net.minecraft.world.item.crafting.RecipeHolder<?> recipe,
            NonNullList<ItemStack> items, int maxStack,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Boolean> cir) {
        ItemStack input = items.get(0);
        if (!(input.getItem() instanceof SpiritSwordItem) || recipe == null) return;
        items.set(0, SpiritSwordTempering.temper(input));
        cir.setReturnValue(true);
    }
}
