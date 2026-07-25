package com.immortalstorage.immortalstorage.event;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.progression.TribulationPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;

import java.util.UUID;

public final class TribulationHelper {
    static final int TARGET_LOAD_GRACE_TICKS = 200;
    public static final String NBT_TRIB_OWNER = "immortalstorage_trib_owner";
    public static final String NBT_TRIB_ATTEMPT = "immortalstorage_trib_attempt";
    public static final String NBT_TRIB_BASE_HEALTH = "immortalstorage_trib_base_health";

    private TribulationHelper() {}

    public static boolean start(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        int stage = data.getStage();
        if (!TribulationPolicy.canStart(stage, TribulationPolicy.configuredMaximumStage())) return false;

        EntityType<? extends Mob> targetType = resolveTargetType(stage, player.serverLevel());
        Mob target = targetType.create(player.serverLevel());
        if (target == null) return false;

        UUID attemptId = UUID.randomUUID();
        UUID targetId = target.getUUID();
        if (!data.beginTribulation(attemptId, targetId, stage + 1)) return false;
        com.immortalstorage.immortalstorage.dimension.RealmHelper.refreshRealmTickRate(player);

        BlockPos center = player.blockPosition();
        target.setPos(center.getX() + 4.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        configureTarget(target, player, attemptId, stage);
        if (!player.serverLevel().addFreshEntity(target)) {
            data.abortTribulation();
            return false;
        }
        return true;
    }

    public static boolean onTargetDeath(LivingEntity dead) {
        if (!(dead.level() instanceof ServerLevel level)) return false;
        var tag = dead.getPersistentData();
        if (!tag.hasUUID(NBT_TRIB_OWNER) || !tag.hasUUID(NBT_TRIB_ATTEMPT)) return false;

        UUID ownerId = tag.getUUID(NBT_TRIB_OWNER);
        UUID attemptId = tag.getUUID(NBT_TRIB_ATTEMPT);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return true;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(owner);
        if (data.completeTribulation(attemptId, dead.getUUID(), owner)) {
            owner.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Tribulation succeeded. Advanced to the next stage."), true);
        }
        return true;
    }

    public static long fail(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!data.isTribulationActive()) return 0L;
        UUID attemptId = data.getTribulationAttemptId();
        removeAttemptTargets(player.server, player.getUUID(), attemptId);
        return data.failTribulation();
    }

    public static void reconcile(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!data.isTribulationActive()) return;
        UUID attemptId = data.getTribulationAttemptId();
        UUID targetId = data.getTribulationTargetId();
        if (attemptId == null || targetId == null) {
            fail(player);
            return;
        }
        Entity target = player.serverLevel().getEntity(targetId);
        if (!(target instanceof Mob mob)
                || !isBoundTarget(mob, player.getUUID(), attemptId)) {
            if (data.noteTribulationTargetMissing(TARGET_LOAD_GRACE_TICKS)) fail(player);
            return;
        }
        data.resetTribulationTargetMissing();
        configureTarget(mob, player, attemptId, data.getStage());
    }

    private static void configureTarget(Mob target, ServerPlayer player, UUID attemptId, int startStage) {
        var persistentData = target.getPersistentData();
        boolean firstProfile = !persistentData.contains(NBT_TRIB_BASE_HEALTH);
        persistentData.putUUID(NBT_TRIB_OWNER, player.getUUID());
        persistentData.putUUID(NBT_TRIB_ATTEMPT, attemptId);
        target.setPersistenceRequired();
        target.setGlowingTag(true);
        target.forceAddEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 2,
                true, false, true), null);
        if (startStage == 8) {
            target.forceAddEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0,
                    true, false, true), null);
        }

        AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double baseHealth = firstProfile
                    ? maxHealth.getBaseValue()
                    : persistentData.getDouble(NBT_TRIB_BASE_HEALTH);
            persistentData.putDouble(NBT_TRIB_BASE_HEALTH, baseHealth);
            maxHealth.setBaseValue(baseHealth * 10.0D);
            if (firstProfile) target.setHealth(target.getMaxHealth());
        }

        if (target instanceof Warden warden) {
            warden.getBrain().setMemoryWithExpiry(
                    MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, Long.MAX_VALUE);
            warden.increaseAngerAt(player, 150, false);
        } else {
            target.setTarget(player);
        }
        if (firstProfile && (startStage == 6 || startStage == 7 || startStage == 8)) {
            equipNetherite(target, player, startStage == 8);
        }
    }

    private static void equipNetherite(Mob target, ServerPlayer player, boolean axe) {
        target.setItemSlot(EquipmentSlot.HEAD, enchanted(player, Items.NETHERITE_HELMET, Enchantments.PROTECTION));
        target.setItemSlot(EquipmentSlot.CHEST, enchanted(player, Items.NETHERITE_CHESTPLATE, Enchantments.PROTECTION));
        target.setItemSlot(EquipmentSlot.LEGS, enchanted(player, Items.NETHERITE_LEGGINGS, Enchantments.PROTECTION));
        target.setItemSlot(EquipmentSlot.FEET, enchanted(player, Items.NETHERITE_BOOTS, Enchantments.PROTECTION));
        target.setItemSlot(EquipmentSlot.MAINHAND, enchanted(player,
                axe ? Items.NETHERITE_AXE : Items.NETHERITE_SWORD, Enchantments.SHARPNESS));
        for (EquipmentSlot slot : EquipmentSlot.values()) target.setDropChance(slot, 0.0F);
    }

    private static ItemStack enchanted(ServerPlayer player, net.minecraft.world.item.Item item,
                                       net.minecraft.resources.ResourceKey<Enchantment> key) {
        ItemStack stack = new ItemStack(item);
        Holder<Enchantment> enchantment = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        stack.enchant(enchantment, 3);
        return stack;
    }

    @SuppressWarnings("unchecked")
    static EntityType<? extends Mob> resolveTargetType(int stage, ServerLevel level) {
        ResourceLocation fallbackId = TribulationPolicy.defaultTargetId(stage);
        ResourceLocation configuredId = ResourceLocation.tryParse(TribulationPolicy.configuredTargetId(stage));
        EntityType<?> configured = configuredId == null ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(configuredId).orElse(null);
        if (isHostileMob(configured, level)) return (EntityType<? extends Mob>) configured;

        EntityType<?> fallback = BuiltInRegistries.ENTITY_TYPE.getOptional(fallbackId)
                .orElse(EntityType.ZOMBIE);
        ImmortalStorageMod.LOG.warn("Invalid tribulation target '{}' for stage {}; using '{}'",
                TribulationPolicy.configuredTargetId(stage), stage, fallbackId);
        return (EntityType<? extends Mob>) fallback;
    }

    private static boolean isHostileMob(EntityType<?> type, ServerLevel level) {
        if (type == null || type.getCategory() != MobCategory.MONSTER) return false;
        Entity candidate = type.create(level);
        return candidate instanceof Mob;
    }

    private static boolean isBoundTarget(Entity target, UUID ownerId, UUID attemptId) {
        var tag = target.getPersistentData();
        return ownerId != null && attemptId != null
                && tag.hasUUID(NBT_TRIB_OWNER) && ownerId.equals(tag.getUUID(NBT_TRIB_OWNER))
                && tag.hasUUID(NBT_TRIB_ATTEMPT) && attemptId.equals(tag.getUUID(NBT_TRIB_ATTEMPT));
    }

    private static void removeAttemptTargets(net.minecraft.server.MinecraftServer server,
                                             UUID ownerId, UUID attemptId) {
        if (ownerId == null || attemptId == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (isBoundTarget(entity, ownerId, attemptId)) entity.discard();
            }
        }
    }
}
