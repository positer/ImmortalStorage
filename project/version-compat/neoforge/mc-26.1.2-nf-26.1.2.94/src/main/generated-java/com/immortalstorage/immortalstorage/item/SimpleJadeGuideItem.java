package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.compat.patchouli.PatchouliJadeGuideCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SimpleJadeGuideItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public SimpleJadeGuideItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide()) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            if (d.getStage() == 0) {
                if (!d.isCarryingJade()) {
                    d.setCarryingJade(true, level.getOverworldClockTime());
                }
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.translatable("message.immortalstorage.jade_guide.resonates"), false);
            }
            if (canOpenGuide(d.getStage()) && p instanceof ServerPlayer serverPlayer) {
                PatchouliJadeGuideCompat.open(serverPlayer);
            } else if (!canOpenGuide(d.getStage())) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.translatable("message.immortalstorage.jade_guide.locked"), true);
            }
        }
        p.getCooldowns().addCooldown(st, 20);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    static boolean canOpenGuide(int stage) {
        return stage >= 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity ent, int slot, boolean sel) {
        super.inventoryTick(stack, level, ent, slot, sel);
        if (level.isClientSide()) return;
        if (!(ent instanceof ServerPlayer p)) return;
        ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
        if (d.getStage() == 0 && !d.isCarryingJade()) {
            d.setCarryingJade(true, level.getOverworldClockTime());
        }
    }
}
