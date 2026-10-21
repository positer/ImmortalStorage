package com.immortalstorage.immortalstorage.item.custom;

import java.util.List;

import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftService;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Guixu Time-Shift Staff.
 *
 * <p>The stack carries only the tier that the <em>next</em> application will
 * grant; every already-shifted block keeps its own record, so changing the held
 * tier never alters an applied acceleration.  The staff itself has no durability
 * and never stacks.</p>
 */
public class TimeShiftStaffItem extends Item {
    public TimeShiftStaffItem(Properties properties) {
        super(properties);
    }

    public static int tierOf(ItemStack stack) {
        return TimeShiftTiers.clampTier(stack.getOrDefault(ModDataComponents.TIME_SHIFT_TIER.get(), 0));
    }

    public static void setTier(ItemStack stack, int tier) {
        stack.set(ModDataComponents.TIME_SHIFT_TIER.get(), TimeShiftTiers.clampTier(tier));
    }

    /**
     * Handles the block click at the earliest point of the interaction chain.
     *
     * <p>2026-10-20 user ruling: while the staff is held, acceleration takes
     * priority over every block interaction, so no block screen may open and no
     * block use behaviour may run. Vanilla skips {@code onItemUseFirst} only
     * while the player is sneaking (that path is what {@link #useOn} still
     * covers), so hooking both methods leaves no vanilla branch that can fall
     * through to the block's own {@code use}.</p>
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return handleBlockClick(context);
    }

    /** Fallback for the sneaking path, which never reaches {@link #onItemUseFirst}. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return handleBlockClick(context);
    }

    private InteractionResult handleBlockClick(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        TimeShiftService.handleBlockUse(level, player, context.getItemInHand(), pos, player.isShiftKeyDown());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("immortalstorage.time_shift.tooltip.tier",
                TimeShiftTiers.label(TimeShiftTiers.permanentMultiplier(tierOf(stack))))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("immortalstorage.time_shift.tooltip.usage")
                .withStyle(ChatFormatting.GRAY));
    }
}
