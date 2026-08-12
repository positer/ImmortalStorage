package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Collects one Primordial Qi only beyond the dimension's vertical build bounds. */
public final class QiCollectingBottleItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    private final boolean reusable;

    public QiCollectingBottleItem(Properties properties, boolean reusable) {
        super(properties);
        this.reusable = reusable;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack bottle = player.getItemInHand(hand);
        if (player.pick(player.blockInteractionRange(), 0.0F, false).getType() != HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        }
        if (player.getY() >= level.getMaxY() + 1 || player.getY() < level.getMinY()) {
            ItemStack result = new ItemStack(ModItems.PRIMORDIAL_QI.get());
            serverPlayer.getInventory().add(result);
            if (!result.isEmpty()) {
                PersonalStorageEndpoint endpoint = PersonalStorageApi.resolve(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(serverPlayer.level()),
                        com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(serverPlayer));
                if (endpoint != null) result = endpoint.insert(result, false);
            }
            boolean delivered = result.isEmpty();
            if (!delivered) return InteractionResult.FAIL;
            if (reusable) {
                bottle.hurtAndBreak(1, serverPlayer, com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.slotForHand(hand));
            } else if (!serverPlayer.getAbilities().instabuild) {
                bottle.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
