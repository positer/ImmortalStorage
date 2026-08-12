package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes the owner-bound drive a reusable credential in vanilla furnace families. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceSpiritDriveMixin {
    @Shadow protected abstract int getBurnDuration(net.minecraft.world.level.block.entity.FuelValues fuelValues, ItemStack stack);

    @Redirect(method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/level/block/entity/FuelValues;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int immortalstorage$paySpiritDrive(
            AbstractFurnaceBlockEntity furnace, net.minecraft.world.level.block.entity.FuelValues fuelValues, ItemStack fuel,
            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            AbstractFurnaceBlockEntity tickingFurnace) {
        if (!(fuel.getItem() instanceof SpiritDriveItem)) {
            return ((AbstractFurnaceSpiritDriveMixin) (Object) furnace).getBurnDuration(fuelValues, fuel);
        }
        return level instanceof ServerLevel serverLevel
                ? SpiritDriveItem.payVanillaFurnaceFuel(serverLevel, pos, fuel) : 0;
    }

    @Redirect(method = "consumeFuel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void immortalstorage$keepSpiritDrive(
            ItemStack fuel, int amount,
            net.minecraft.core.NonNullList<ItemStack> items,
            ItemStack fuelArgument) {
        if (!(fuel.getItem() instanceof SpiritDriveItem)) fuel.shrink(amount);
    }
}
