package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WhiteDayThunderItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public WhiteDayThunderItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide()) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            d.setStage(10);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.STAGE_10.trigger(sp);
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.WHITE_DAY_THUNDER_USED.trigger(sp);
            }
        }
        p.getCooldowns().addCooldown(st, 20);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
