package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.network.ModPayloads;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

final class XianqiaoPickInput {
    static void onPick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isPickBlock() || event.isCanceled() || mc.player == null || mc.screen != null
                || mc.player.isSpectator() || mc.player.isCreative()
                || ImmortalStoragePlayerData.get(mc.player).getStage() < 6
                || mc.getConnection() == null || !mc.getConnection().hasChannel(ModPayloads.PickXianqiaoItem.TYPE)) return;
        ModPayloads.PickXianqiaoItem request;
        if (mc.hitResult instanceof BlockHitResult hit) {
            request = new ModPayloads.PickXianqiaoItem(hit.getBlockPos().asLong(), -1);
        } else if (mc.hitResult instanceof EntityHitResult hit) {
            request = new ModPayloads.PickXianqiaoItem(0, hit.getEntity().getId());
        } else return;
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(request);
    }
}
