package com.immortalstorage.immortalstorage.mixin.core;

import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SmithingMenu.class)
public interface SmithingMenuAccessor {
    @Invoker("shrinkStackInSlot")
    void immortalstorage$shrinkStackInSlot(int slot);
}
