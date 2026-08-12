package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Three-stage, entity-piercing beam weapon forged from the Spirit Sword line. */
public final class OneQiReturningOriginSwordItem extends SpiritSwordItem {
    public static final int MAX_USE_TICKS = 72_000;
    private static final double RANGE = 64.0D;

    public OneQiReturningOriginSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
        int elapsed = MAX_USE_TICKS - remainingUseDuration;
        if (elapsed < 5) return;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (elapsed == 5 && !data.consumeImmortalYuan(1L)) {
            player.stopUsingItem();
            return;
        }
        if (elapsed == 25 && !data.consumeImmortalYuan(2L)) {
            player.stopUsingItem();
            return;
        }

        int phase = elapsed > 50 ? 3 : elapsed >= 25 ? 2 : 1;
        int interval = phase == 1 ? 10 : 5;
        if ((elapsed - (phase == 1 ? 5 : phase == 2 ? 25 : 50)) % interval != 0) return;
        if (phase == 3) {
            long points = SpiritSwordTempering.points(stack);
            if (points < 10L) {
                player.stopUsingItem();
                return;
            }
            SpiritSwordTempering.setPoints(stack, points - 10L);
        }
        fireBeam(serverLevel, player, stack, phase);
    }

    private static void fireBeam(ServerLevel level, ServerPlayer player, ItemStack stack, int phase) {
        SpiritSwordItem.markUsed(stack, level.getGameTime());
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(RANGE));
        double radius = phase == 1 ? 0.20D : phase == 2 ? 0.38D : 0.58D;
        AABB search = new AABB(start, end).inflate(radius);
        float attack = SpiritSwordCombatModel.forStage(
                ImmortalStoragePlayerData.get(player).getStage()).successfulHitDamage();
        float damage = attack * (phase == 3 ? 0.50F : 0.20F);
        level.getEntitiesOfClass(LivingEntity.class, search,
                        target -> target != player && target.isAlive()
                                && target.getBoundingBox().inflate(radius).clip(start, end).isPresent())
                .stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .forEach(target -> target.hurt(player.damageSources().playerAttack(player), damage));

    }

    public static int beamPhase(LivingEntity user) {
        if (!(user.getUseItem().getItem() instanceof OneQiReturningOriginSwordItem)) return 0;
        int elapsed = user.getTicksUsingItem();
        return elapsed < 5 ? 0 : elapsed > 50 ? 3 : elapsed >= 25 ? 2 : 1;
    }

    public static double beamRange() { return RANGE; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.contract"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.melee"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.phase_1"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.phase_2"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.phase_3"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.one_qi_sword.limit",
                SpiritSwordTempering.MAX_POINTS));
    }
}
