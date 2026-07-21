package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places the unbreakable field anchor represented by this one-stack item. */
public final class MiniatureImmortalRuinItem extends Item {
    public MiniatureImmortalRuinItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
        if (!level.isClientSide && level.setBlock(pos, ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState(), 3)
                && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
