package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @Redirect(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V", ordinal = 1))
    private void immortalstorage$keepJadeGuide(SmithingMenu menu, int slot,
                                                Player player, ItemStack result) {
        if (!result.is(ModItems.IMMORTAL_MASTER_TALISMAN.get())) {
            ((SmithingMenuAccessor) menu).immortalstorage$shrinkStackInSlot(slot);
        }
    }
}
