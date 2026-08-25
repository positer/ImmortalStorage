package com.immortalstorage.immortalstorage.combat;

import com.immortalstorage.core.combat.AuraGuardSettlement;
import com.immortalstorage.immortalstorage.effect.ModEffects;
import com.immortalstorage.immortalstorage.compat.accessory.AccessoryTalismanBridge;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ImmortalMasterTalismanService {
    private static final Identifier ARMOR_ID = Identifier.fromNamespaceAndPath(
            "immortalstorage", "immortal_master_talisman_armor");
    private static final AttributeModifier ARMOR = new AttributeModifier(
            ARMOR_ID, 10.0D, AttributeModifier.Operation.ADD_VALUE);
    private static final ThreadLocal<Map<ServerPlayer, Double>> EXPECTED_HEALTH_DAMAGE =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private ImmortalMasterTalismanService() {
    }

    public static boolean isEquipped(LivingEntity entity) {
        return isTalisman(entity.getItemBySlot(EquipmentSlot.CHEST))
                || isTalisman(entity.getItemBySlot(EquipmentSlot.LEGS))
                || isTalisman(entity.getItemBySlot(EquipmentSlot.BODY))
                || AccessoryTalismanBridge.isEquipped(entity);
    }

    private static boolean isTalisman(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.IMMORTAL_MASTER_TALISMAN.get());
    }

    public static void tick(LivingEntity entity) {
        boolean equipped = isEquipped(entity);
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (equipped) armor.addOrUpdateTransientModifier(ARMOR);
            else armor.removeModifier(ARMOR_ID);
        }
        if (!equipped) return;
        if (entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SPIRITUAL_AURA_GUARD.get()),
                    40, 0, true, false, true));
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 4, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 3, true, false, true));
        }
    }

    public static void intercept(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EXPECTED_HEALTH_DAMAGE.get().remove(player);
        if (!hasAuraGuard(player)) return;
        if (event.getSource().is(DamageTypeTags.IS_FALL)
                || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)) {
            event.setNewDamage(0.0F);
            return;
        }
        float requested = event.getNewDamage();
        if (!(requested > 0.0F) || !Float.isFinite(requested)) return;
        AuraGuardSettlement.Result result = settle(player, requested);
        EXPECTED_HEALTH_DAMAGE.get().put(player, result.healthDamage());
        event.setNewDamage((float) result.healthDamage());
    }

    /**
     * Repairs health that a damage path managed to remove despite the pre-damage
     * guard. The post event reports only this sequence's actual health loss, so
     * the repair cannot restore older wounds or recurse through another damage
     * or healing event.
     */
    public static void repairBypassedDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Double expected = EXPECTED_HEALTH_DAMAGE.get().remove(player);
        if (!hasAuraGuard(player)) return;
        float lostHealth = event.getHealthDamage();
        double bypassed = expected == null ? lostHealth : Math.max(0.0D, lostHealth - expected);
        if (!(bypassed > 1.0E-6D) || !Double.isFinite(bypassed)) return;
        AuraGuardSettlement.Result result = settle(player, bypassed);
        if (!(result.blockedDamage() > 0.0D)) return;
        player.setHealth(Math.min(player.getMaxHealth(),
                player.getHealth() + (float) result.blockedDamage()));
    }

    private static AuraGuardSettlement.Result settle(ServerPlayer player, double requested) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        AuraGuardSettlement.Result result = AuraGuardSettlement.settle(
                requested, data.getTrueYuan(), data.getImmortalYuan(), data.getAuraGuardCredit());
        if (result.trueYuanSpent() > 0L && !data.consumeTrueYuan(result.trueYuanSpent())) {
            return AuraGuardSettlement.settle(requested, 0L, 0L, data.getAuraGuardCredit());
        }
        if (result.immortalYuanSpent() > 0L && !data.consumeImmortalYuan(result.immortalYuanSpent())) {
            data.depositTrueYuan(result.trueYuanSpent());
            return AuraGuardSettlement.settle(requested, 0L, 0L, data.getAuraGuardCredit());
        }
        if (result.convertedTrueYuanRemainder() > 0L) {
            data.depositTrueYuan(result.convertedTrueYuanRemainder());
        }
        data.setAuraGuardCredit(result.endingCredit());
        return result;
    }

    public static boolean hasAuraGuard(LivingEntity entity) {
        return entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SPIRITUAL_AURA_GUARD.get()));
    }
}
