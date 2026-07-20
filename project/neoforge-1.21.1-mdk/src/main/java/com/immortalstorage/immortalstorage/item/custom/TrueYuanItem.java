package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public class TrueYuanItem extends BlockItem {
    public static final int VANILLA_BURN_TICKS = 2_000;
    public TrueYuanItem(Block block, Properties props) {
        super(block, props);
    }

    /** Detached-test constructor; production registration uses the real light block. */
    public TrueYuanItem(Properties props) { this(net.minecraft.world.level.block.Blocks.AIR, props); }

    @Override public String getDescriptionId() { return "item.immortalstorage.true_yuan"; }

    @Override public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return VANILLA_BURN_TICKS;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return YuanLightPlacement.use(level, player, hand, false);
    }
}
