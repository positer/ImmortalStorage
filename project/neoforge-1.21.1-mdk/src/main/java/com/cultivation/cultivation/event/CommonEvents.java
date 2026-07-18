package com.cultivation.cultivation.event;

import com.cultivation.cultivation.effect.ModEffects;
import com.cultivation.cultivation.item.ModItems;
import com.cultivation.cultivation.player.CultivationPlayerData;
import com.cultivation.cultivation.block.entity.SourceVeinBlockEntity;
import com.cultivation.cultivation.config.CultivationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class CommonEvents {
    public CommonEvents() {}

    @SubscribeEvent
    public void onMobSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) return;
        if (event.getSpawnType() != net.minecraft.world.entity.MobSpawnType.NATURAL
                && event.getSpawnType() != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) return;
        if (event.getEntity().getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                && com.cultivation.cultivation.block.entity.YuanLightIndex.suppresses(
                        level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var opened = event.getContainer();
            player.server.execute(() -> {
                if (player.containerMenu == opened) {
                    com.cultivation.cultivation.item.custom.SpiritStaffItem.transferOpenedLootMenu(
                            player, opened);
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            grantStartingJade(p);
            TribulationHelper.reconcile(p);
            CultivationPlayerData.get(p).syncTo(p);
            restoreStageEffects(p);
        }
    }

    private static void grantStartingJade(ServerPlayer player) {
        CultivationPlayerData data = CultivationPlayerData.get(player);
        if (!CultivationConfig.START_WITH_JADE_GUIDE.get() || data.isStartingJadeGranted()) return;
        data.markStartingJadeGranted();
        if (hasJadeGuide(player)) return;
        ItemStack jade = new ItemStack(ModItems.JADE_GUIDE.get());
        if (!player.getInventory().add(jade)) player.drop(jade, false);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            CultivationPlayerData.get(p).syncTo(p);
            restoreStageEffects(p);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            restoreStageEffects(p);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.isCanceled()) return;
        // The vanilla game-mode transition rewrites abilities after this event fires.
        // Reconcile on the server task queue so Cultivation flight and effects win last.
        p.server.execute(() -> restoreStageEffects(p));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            com.cultivation.cultivation.player.HeldItemAutoRefill.clear(player);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent e) {
        markPersonalRealmModified(e.getLevel(), e.getPos());
        if (CultivationConfig.SOURCE_ALLOW_OTHER_PLAYER_BREAK.get()) return;
        if (!(e.getLevel() instanceof Level level)) return;
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(e.getPos()) instanceof SourceVeinBlockEntity source)) return;
        Player player = e.getPlayer();
        if (player instanceof ServerPlayer sp && sp.hasPermissions(2)) return;
        if (!source.isUnowned() && !source.isOwnedBy(player.getUUID())) {
            e.setCanceled(true);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Only the source block owner can break it."), true);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        markPersonalRealmModified(e.getLevel(), e.getPos());
    }

    private void markPersonalRealmModified(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        java.util.Optional<java.util.UUID> ownerId =
                com.cultivation.cultivation.dimension.CultivationDimensions.personalRealmOwner(serverLevel.dimension());
        if (ownerId.isEmpty()) return;
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId.get());
        if (owner != null) {
            com.cultivation.cultivation.dimension.RealmHelper.markModifiedChunk(owner, pos);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate e) {
        if (CultivationConfig.SOURCE_ALLOW_MOB_BREAK.get()) return;
        if (!(e.getLevel() instanceof Level level)) return;
        if (level.isClientSide) return;
        if (e.getExplosion().getDirectSourceEntity() instanceof Player) return;
        e.getAffectedBlocks().removeIf(pos -> level.getBlockEntity(pos) instanceof SourceVeinBlockEntity);
    }

    @SubscribeEvent
    public void onPlayerSleep(CanPlayerSleepEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        if (!(p instanceof ServerPlayer serverPlayer)) return;
        CultivationPlayerData d = CultivationPlayerData.get(p);
        if (d.tryJadeSleepInitiation(hasJadeGuide(p))) {
            com.cultivation.cultivation.advancement.CultivationCriteriaTriggers.STAGE_1.trigger(serverPlayer);
        }
    }

    private static boolean hasJadeGuide(Player p) {
        for (ItemStack s : p.getInventory().items) if (s.is(ModItems.JADE_GUIDE.get())) return true;
        for (ItemStack s : p.getInventory().offhand) if (s.is(ModItems.JADE_GUIDE.get())) return true;
        return false;
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre e) {
        ItemEntity ent = e.getItemEntity();
        if (ent == null) return;
        if (ent.getItem().getItem() instanceof com.cultivation.cultivation.item.custom.TrueYuanItem) {
            e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            Player p = e.getPlayer();
            if (p instanceof ServerPlayer sp) {
                CultivationPlayerData d = CultivationPlayerData.get(sp);
                long accepted = d.depositTrueYuan(ent.getItem().getCount());
                if (accepted > 0L) ent.getItem().shrink((int) accepted);
                if (ent.getItem().isEmpty()) ent.discard();
            }
            return;
        }
        if (ent.getItem().getItem() instanceof com.cultivation.cultivation.item.custom.ImmortalYuanItem) {
            e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            Player p = e.getPlayer();
            if (p instanceof ServerPlayer sp) {
                CultivationPlayerData d = CultivationPlayerData.get(sp);
                long accepted = d.depositImmortalYuan(ent.getItem().getCount());
                if (accepted > 0L) ent.getItem().shrink((int) accepted);
                if (ent.getItem().isEmpty()) ent.discard();
            }
            return;
        }
        // Magnet pickup: stages 4+ have a 5x5x5 vacuum around the player
        Player p = e.getPlayer();
        if (p instanceof ServerPlayer sp) {
            CultivationPlayerData d = CultivationPlayerData.get(sp);
            if (d.getStage() >= 4) {
                if (ent.isAlive() && ent.getItem().getItem() != ModItems.JADE_GUIDE.get()
                        && !(ent.getItem().getItem() instanceof com.cultivation.cultivation.item.custom.TrueYuanItem)
                        && !(ent.getItem().getItem() instanceof com.cultivation.cultivation.item.custom.ImmortalYuanItem)) {
                    Vec3 v = sp.position().add(0, 0.5, 0).subtract(ent.position());
                    ent.setDeltaMovement(v.scale(0.4));
                }
            }
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent e) {
        // TrueYuan/ImmortalYuan item classes already cancel their own toss.
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof LivingEntity dead
                && dead.getPersistentData().hasUUID(TribulationHelper.NBT_TRIB_ATTEMPT)) {
            TribulationHelper.onTargetDeath(dead);
        }
        if (e.getEntity() instanceof ServerPlayer p) {
            CultivationPlayerData data = CultivationPlayerData.get(p);
            if (data.isTribulationActive()) {
                TribulationHelper.fail(p);
                e.setCanceled(true);
                p.setHealth(p.getMaxHealth());
                p.clearFire();
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.WITHER);
                p.fallDistance = 0.0F;
                p.invulnerableTime = 60;
                restoreStageEffects(p);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Tribulation failed. Immortal Yuan was cleared."), true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingIncomingDamageEvent e) {
        // Reserved for cultivation-specific incoming-damage rules.
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity().level().isClientSide) return;
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CultivationPlayerData d = CultivationPlayerData.get(p);
        com.cultivation.cultivation.player.HeldItemAutoRefill.tick(p, d);
        if (d.tickJadeInitiation(hasJadeGuide(p))) {
            com.cultivation.cultivation.advancement.CultivationCriteriaTriggers.STAGE_1.trigger(p);
        }
        d.serverTick(p);
        if (d.getStage() >= 5) {
            d.getEmbeddedImmortalFurnace().tick(p);
        }
        if (d.isTribulationActive()) {
            tickTribulation(p, d);
            suppressCultivationBuffs(p);
        } else {
            applyStageEffects(p, d);
        }
        // Personal realm: keep the owner's active chunks force-loaded.
        if (com.cultivation.cultivation.dimension.CultivationDimensions.isPersonalRealmFor(p.level().dimension(), p.getUUID())) {
            com.cultivation.cultivation.dimension.RealmHelper.enforcePlayerBoundary(p);
            com.cultivation.cultivation.dimension.RealmHelper.ensureChunksForced(p);
        }
    }

    private void tickTribulation(ServerPlayer p, CultivationPlayerData d) {
        if (!com.cultivation.cultivation.dimension.CultivationDimensions
                .isPersonalRealmFor(p.level().dimension(), p.getUUID())) {
            TribulationHelper.fail(p);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Tribulation failed after leaving the personal realm."), true);
            return;
        }
        if (com.cultivation.cultivation.progression.TribulationPolicy.requiresBlindness(d.getStage())) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false, true));
        }
        if (com.cultivation.cultivation.progression.TribulationPolicy.requiresWither(d.getStage())) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, false, true));
        }
        TribulationHelper.reconcile(p);
    }

    private static void suppressCultivationBuffs(ServerPlayer p) {
        p.removeEffect(MobEffects.DAMAGE_BOOST);
        p.removeEffect(MobEffects.MOVEMENT_SPEED);
        p.removeEffect(MobEffects.REGENERATION);
        p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        p.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.LINGQI_SATURATION.get()));
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    public static void restoreStageEffects(ServerPlayer p) {
        if (p == null) return;
        CultivationPlayerData data = CultivationPlayerData.get(p);
        if (data.isTribulationActive()) {
            suppressCultivationBuffs(p);
            return;
        }
        applyStageEffects(p, data);
    }

    private static void applyStageEffects(ServerPlayer p, CultivationPlayerData d) {
        boolean weak = d.isAdvancedWeak() && d.getAdvancedWeakTicks() > 0;
        if (weak) {
            Holder<net.minecraft.world.effect.MobEffect> advancedWeakness = BuiltInRegistries.MOB_EFFECT
                    .wrapAsHolder(ModEffects.ADVANCED_WEAKNESS.get());
            MobEffectInstance currentWeakness = p.getEffect(advancedWeakness);
            if (currentWeakness == null) {
                p.addEffect(new MobEffectInstance(advancedWeakness, d.getAdvancedWeakTicks(),
                        0, false, true, true));
            }
            p.removeEffect(MobEffects.DAMAGE_BOOST);
            p.removeEffect(MobEffects.MOVEMENT_SPEED);
            p.removeEffect(MobEffects.REGENERATION);
            p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            return;
        }
        int st = d.getStage();
        int strength = st >= 9 ? 4 : st >= 7 ? 3 : st >= 5 ? 2 : st >= 3 ? 1 : st >= 1 ? 0 : -1;
        int speed = st >= 3 ? 1 : st >= 1 ? 0 : -1;
        int regeneration = st >= 10 ? 7 : st >= 9 ? 4 : st >= 8 ? 3 : st >= 7 ? 2
                : st >= 4 ? 1 : st >= 2 ? 0 : -1;
        int resistance = st >= 10 ? 4 : st >= 9 ? 3 : st >= 6 ? 2 : st >= 4 ? 1
                : st >= 2 ? 0 : -1;

        ensureStageEffect(p, MobEffects.DAMAGE_BOOST, strength);
        ensureStageEffect(p, MobEffects.MOVEMENT_SPEED, speed);
        ensureStageEffect(p, MobEffects.REGENERATION, regeneration);
        ensureStageEffect(p, MobEffects.DAMAGE_RESISTANCE, resistance);
        if (st >= 6) {
            boolean abilitiesChanged = !p.getAbilities().mayfly
                    || Float.compare(p.getAbilities().getFlyingSpeed(), 0.05f) != 0;
            if (abilitiesChanged) {
                p.getAbilities().mayfly = true;
                p.getAbilities().setFlyingSpeed(0.05f);
                p.onUpdateAbilities();
            }
        }
        if (d.getLingqiSaturatedLayers() > 0) {
            Holder<net.minecraft.world.effect.MobEffect> lingqiSatHolder = BuiltInRegistries.MOB_EFFECT
                    .wrapAsHolder(ModEffects.LINGQI_SATURATION.get());
            ensureStageEffect(p, lingqiSatHolder, 0);
        }
    }

    private static void ensureStageEffect(ServerPlayer player,
                                          Holder<net.minecraft.world.effect.MobEffect> effect,
                                          int amplifier) {
        if (amplifier < 0) return;
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() > amplifier
                || current.getAmplifier() == amplifier && current.getDuration() > 40)) return;
        player.addEffect(new MobEffectInstance(effect, 100,
                amplifier, true, false, true));
    }
}
