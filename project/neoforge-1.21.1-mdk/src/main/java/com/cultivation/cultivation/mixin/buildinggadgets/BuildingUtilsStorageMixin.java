package com.cultivation.cultivation.mixin.buildinggadgets;

import com.cultivation.cultivation.compat.buildinggadgets.BuildingGadgetsStorageBridge;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(targets = "com.direwolf20.buildinggadgets2.util.BuildingUtils", remap = false)
public abstract class BuildingUtilsStorageMixin {
    @Redirect(
            method = "removeStacksFromInventory",
            at = @At(value = "INVOKE", target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;checkInventoryForItems(Lnet/minecraft/world/entity/player/Inventory;Ljava/util/List;Z)V"),
            require = 1)
    private static void cultivation$readPersonalStorage(
            Inventory inventory, List<ItemStack> requested, boolean simulate) {
        BuildingGadgetsStorageBridge.satisfyRequestedItems(inventory.player, requested, simulate);
        if (!requested.isEmpty()) scanInventory(inventory, requested, simulate);
    }

    @Inject(method = "countItemStacks", at = @At("RETURN"), cancellable = true, require = 1)
    private static void cultivation$countPersonalStorage(
            Player player, ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        long combined = (long) callback.getReturnValue()
                + BuildingGadgetsStorageBridge.countAvailable(player, stack);
        callback.setReturnValue((int) Math.min(Integer.MAX_VALUE, combined));
    }

    private static void scanInventory(Inventory inventory, List<ItemStack> requested, boolean simulate) {
        for (int slot = 0; slot < inventory.getContainerSize() && !requested.isEmpty(); slot++) {
            ItemStack stored = inventory.getItem(slot);
            var handler = stored.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (handler != null) {
                scanHandler(handler, requested, simulate);
                continue;
            }
            Optional<ItemStack> match = requested.stream()
                    .filter(need -> ItemStack.isSameItem(need, stored) && stored.getCount() >= need.getCount())
                    .findFirst();
            if (match.isPresent()) {
                ItemStack need = match.get();
                if (!simulate) stored.shrink(need.getCount());
                requested.remove(need);
            }
        }
    }

    private static void scanHandler(net.neoforged.neoforge.items.IItemHandler handler,
                                    List<ItemStack> requested, boolean simulate) {
        for (int slot = 0; slot < handler.getSlots() && !requested.isEmpty(); slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            Optional<ItemStack> match = requested.stream()
                    .filter(need -> ItemStack.isSameItem(need, stored) && stored.getCount() >= need.getCount())
                    .findFirst();
            if (match.isPresent()) {
                ItemStack need = match.get();
                handler.extractItem(slot, need.getCount(), simulate);
                requested.remove(need);
            }
        }
    }
}
