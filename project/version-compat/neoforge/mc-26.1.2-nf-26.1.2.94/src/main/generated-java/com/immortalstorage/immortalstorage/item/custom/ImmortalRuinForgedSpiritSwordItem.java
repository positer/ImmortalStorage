package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Upgraded Spirit Sword retaining the base sword contract with stronger tempering and field control. */
public final class ImmortalRuinForgedSpiritSwordItem extends SpiritSwordItem {
    private static final int TELEPORT_RESTRAINT_TICKS = 40;

    public ImmortalRuinForgedSpiritSwordItem(Item.Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SpiritSwordItem.markUsed(stack, level.getGameTime());
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(serverPlayer);
        if (!data.consumeImmortalYuan(5L)) return InteractionResult.FAIL;
        Vec3 target = serverPlayer.position().add(serverPlayer.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize());
        AABB area = new AABB(serverPlayer.blockPosition()).inflate(13.0D);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> canTeleport(serverPlayer, entity))) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                    24, 0.25D, 0.5D, 0.25D, 0.2D);
            entity.teleportTo(target.x, target.y, target.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    TELEPORT_RESTRAINT_TICKS, 255, false, false, true));
            com.immortalstorage.immortalstorage.entity.AbsoluteRestraint.apply(
                    entity, TELEPORT_RESTRAINT_TICKS);
            serverLevel.sendParticles(ParticleTypes.PORTAL, target.x, target.y + 0.5D, target.z,
                    24, 0.25D, 0.5D, 0.25D, 0.2D);
        }
        return InteractionResult.CONSUME;
    }

    public static float temperingMultiplier() { return 0.015F; }
    public static double sweepRangeMultiplier() { return 1.5D; }

    private static boolean canTeleport(ServerPlayer wielder, LivingEntity entity) {
        return entity != wielder && entity.isAlive()
                && (!(entity instanceof Player)
                || ImmortalStorageConfig.IMMORTAL_RUIN_SWORD_AFFECTS_OTHER_PLAYERS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.immortalstorage.immortal_ruin_sword.base_contract"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.immortal_ruin_sword.tempering"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.immortal_ruin_sword.sweep"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.immortal_ruin_sword.pull"));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide()) {
            double radius = 1.5D;
            for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(radius), entity -> entity != attacker && entity != target && entity.isAlive())) {
                nearby.hurt(attacker.damageSources().mobAttack(attacker), 1.0F);
            }
        }

    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        stack.setDamageValue(0);
    }
}
