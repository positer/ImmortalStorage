package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.YuanLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class YuanLightPlacement {
    static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, boolean immortal) {
        ItemStack held = player.getItemInHand(hand);
        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(held);
        }
        BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        var block = immortal ? ModBlocks.IMMORTAL_YUAN_LIGHT.get() : ModBlocks.TRUE_YUAN_LIGHT.get();
        var state = block.defaultBlockState().setValue(YuanLightBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE);
        if (!level.getBlockState(pos).canBeReplaced() || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)) return InteractionResultHolder.fail(held);
        if (!level.isClientSide) {
            if (!level.setBlock(pos, state, 3)) return InteractionResultHolder.fail(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    private YuanLightPlacement() {}
}
