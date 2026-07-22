package com.immortalstorage.immortalstorage.entity;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.SpawnEggItem;

/** Persistent, server-authoritative two-second non-kill conversion animation. */
public final class PrimordialQiConversion {
    private static final String START = "ImmortalStoragePrimordialQiStart";
    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalStorageMod.MODID, "primordial_qi_shrink");
    public static final int DURATION = 10;

    public static void begin(LivingEntity entity) {
        entity.getPersistentData().putLong(START, entity.level().getGameTime());
        entity.setInvulnerable(true);
    }

    public static boolean isConverting(Entity entity) { return entity.getPersistentData().contains(START); }

    public static void tick(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !(entity.level() instanceof ServerLevel level)
                || !isConverting(entity)) return;
        long elapsed = Math.max(0L, level.getGameTime() - entity.getPersistentData().getLong(START));
        double scale = Math.max(0.01D, 1.0D - elapsed / (double) DURATION);
        var attribute = living.getAttribute(Attributes.SCALE);
        if (attribute != null) attribute.addOrUpdateTransientModifier(new AttributeModifier(
                SCALE_ID, scale - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (elapsed < DURATION) return;
        if (living instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            var fight = level.getDragonFight();
            if (fight != null) fight.setDragonKilled(dragon);
        }
        SpawnEggItem egg = findRegisteredSpawnEgg(living.getType());
        if (egg != null) level.addFreshEntity(new ItemEntity(level, living.getX(), living.getY(), living.getZ(),
                egg.getDefaultInstance()));
        living.discard();
    }

    static SpawnEggItem findRegisteredSpawnEgg(EntityType<?> entityType) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof SpawnEggItem)
                .map(item -> (SpawnEggItem) item)
                // Resolve against each globally registered egg's real default stack.  This also
                // covers eggs whose entity type is supplied by a mod or by stack components.
                .filter(candidate -> candidate.getType(candidate.getDefaultInstance()) == entityType)
                .findFirst().orElse(null);
    }

    private PrimordialQiConversion() {}
}
