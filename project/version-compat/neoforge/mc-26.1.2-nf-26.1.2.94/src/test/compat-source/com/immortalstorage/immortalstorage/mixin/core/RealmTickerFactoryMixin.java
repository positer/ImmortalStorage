package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.dimension.RealmSafeBlockTicker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Covers direct getTicker calls made by accelerators using the normal ticker factory. */
@Mixin(BaseEntityBlock.class)
public abstract class RealmTickerFactoryMixin {
    @ModifyReturnValue(method = "createTickerHelper", at = @At("RETURN"))
    private static <T extends BlockEntity> BlockEntityTicker<T> immortalstorage$safeTicker(BlockEntityTicker<T> ticker) {
        return RealmSafeBlockTicker.wrap(ticker);
    }
}
