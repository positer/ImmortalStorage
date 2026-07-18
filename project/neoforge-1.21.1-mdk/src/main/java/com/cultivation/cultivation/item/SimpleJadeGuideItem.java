package com.cultivation.cultivation.item;

import com.cultivation.cultivation.player.CultivationPlayerData;
import com.cultivation.cultivation.network.ModPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class SimpleJadeGuideItem extends Item {
    public SimpleJadeGuideItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide) {
            CultivationPlayerData d = CultivationPlayerData.get(p);
            if (d.getStage() == 0) {
                if (!d.isCarryingJade()) {
                    d.setCarryingJade(true, level.getDayTime());
                }
                p.displayClientMessage(Component.translatable("message.cultivation.jade_guide.resonates"), false);
            }
            if (canOpenGuide(d.getStage()) && p instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ModPayloads.OpenJadeGuideScreen());
            } else if (!canOpenGuide(d.getStage())) {
                p.displayClientMessage(Component.translatable("message.cultivation.jade_guide.locked"), true);
            }
        }
        p.getCooldowns().addCooldown(st.getItem(), 20);
        return level.isClientSide ? InteractionResultHolder.success(st) : InteractionResultHolder.consume(st);
    }

    static boolean canOpenGuide(int stage) {
        return stage >= 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity ent, int slot, boolean sel) {
        super.inventoryTick(stack, level, ent, slot, sel);
        if (level.isClientSide) return;
        if (!(ent instanceof ServerPlayer p)) return;
        CultivationPlayerData d = CultivationPlayerData.get(p);
        if (d.getStage() == 0 && !d.isCarryingJade()) {
            d.setCarryingJade(true, level.getDayTime());
        }
    }
}
