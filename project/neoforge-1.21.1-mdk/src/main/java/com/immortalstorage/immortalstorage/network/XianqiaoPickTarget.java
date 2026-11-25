package com.immortalstorage.immortalstorage.network;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.XianqiaoPickItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Resolves a world target using the same clone/pick APIs as native creative picking. */
public final class XianqiaoPickTarget {
    private XianqiaoPickTarget() {}

    public static void handle(ServerPlayer player, ModPayloads.PickXianqiaoItem request) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || data.getStage() < 6
                || player.containerMenu != player.inventoryMenu) return;
        ItemStack target = ItemStack.EMPTY;
        if (request.entityId() >= 0) {
            Entity entity = player.serverLevel().getEntity(request.entityId());
            if (entity != null && player.canInteractWithEntity(entity, 3.0) && player.hasLineOfSight(entity)) {
                target = entity.getPickResult();
            }
        } else {
            BlockPos pos = BlockPos.of(request.pos());
            if (!player.serverLevel().isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
            HitResult hit = player.pick(player.blockInteractionRange() + 1.0, 0, false);
            if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)) return;
            var state = player.serverLevel().getBlockState(pos);
            if (!state.isAir()) target = state.getCloneItemStack(blockHit, player.serverLevel(), pos, player);
        }
        if (target == null || target.isEmpty() || !target.isItemEnabled(player.level().enabledFeatures())) return;
        if (!XianqiaoPickItem.moveToHand(data, player.getInventory(), player.getInventory().selected, target)) {
            int slot = player.getInventory().findSlotMatchingItem(target);
            if (slot >= 0 && slot < 9) player.getInventory().selected = slot;
            else if (slot >= 0) player.getInventory().pickSlot(slot);
        }
        // Publish only after the selected stack and both storage destinations are final.
        data.syncTo(player);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(player.getInventory().selected));
        player.inventoryMenu.broadcastChanges();
    }
}
