package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Collects one Primordial Qi only beyond the dimension's vertical build bounds. */
public final class QiCollectingBottleItem extends Item {
    private final boolean reusable;

    public QiCollectingBottleItem(Properties properties, boolean reusable) {
        super(properties);
        this.reusable = reusable;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bottle = player.getItemInHand(hand);
        if (player.pick(player.blockInteractionRange(), 0.0F, false).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(bottle);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(bottle, level.isClientSide);
        }
        if (player.getY() >= level.getMaxBuildHeight() || player.getY() < level.getMinBuildHeight()) {
            ItemStack result = new ItemStack(ModItems.PRIMORDIAL_QI.get());
            serverPlayer.getInventory().add(result);
            if (!result.isEmpty()) {
                PersonalStorageEndpoint endpoint = PersonalStorageApi.resolve(serverPlayer.server, serverPlayer.getUUID());
                if (endpoint != null) result = endpoint.insert(result, false);
            }
            boolean delivered = result.isEmpty();
            if (!delivered) return InteractionResultHolder.fail(bottle);
            if (reusable) {
                bottle.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
            } else if (!serverPlayer.getAbilities().instabuild) {
                bottle.shrink(1);
            }
            return InteractionResultHolder.consume(bottle);
        }
        return InteractionResultHolder.fail(bottle);
    }
}
