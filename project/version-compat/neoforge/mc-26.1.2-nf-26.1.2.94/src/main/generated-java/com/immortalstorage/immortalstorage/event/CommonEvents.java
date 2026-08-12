package com.immortalstorage.immortalstorage.event;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import com.immortalstorage.immortalstorage.effect.ModEffects;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
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
import net.minecraft.world.item.Items;
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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

public class CommonEvents {
    public CommonEvents() {}

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        com.immortalstorage.immortalstorage.entity.PrimordialQiConversion.tick(event.getEntity());
        com.immortalstorage.immortalstorage.entity.AbsoluteRestraint.tick(event.getEntity());
    }

    @SubscribeEvent
    public void onSpawnerConversion(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.SPAWNER)
                || event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.TRIAL_SPAWNER))) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean valid = main.is(ModItems.PRIMORDIAL_QI.get()) && off.is(ModItems.SPIRIT_CORE.get());
        if (!valid) return;
        event.getLevel().setBlockAndUpdate(event.getPos(),
                com.immortalstorage.immortalstorage.block.ModBlocks.SIMULATED_REINCARNATION_FURNACE.get()
                        .defaultBlockState());
        if (!player.getAbilities().instabuild) {
            main.shrink(1);
        }
        com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers
                .SPAWNER_CONVERTED.trigger(player);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMobSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) return;
        if (event.getSpawnType() != net.minecraft.world.entity.EntitySpawnReason.NATURAL
                && event.getSpawnType() != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION) return;
        if (event.getEntity().getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                && com.immortalstorage.immortalstorage.block.entity.YuanLightIndex.suppresses(
                        level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var opened = event.getContainer();
            com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).execute(() -> {
                if (player.containerMenu == opened) {
                    com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.transferOpenedLootMenu(
                            player, opened);
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            com.immortalstorage.immortalstorage.dimension.RealmHelper.realmId(p);
            com.immortalstorage.immortalstorage.dimension.RealmHelper.ensureRespawnRealmRegistered(p);
            grantStartingJade(p);
            TribulationHelper.reconcile(p);
            ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(p);
            clearInvalidPuppetAnchors(p, data);
            data.syncTo(p);
            restoreStageEffects(p);
        }
    }

    private static void grantStartingJade(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!ImmortalStorageConfig.START_WITH_JADE_GUIDE.get() || data.isStartingJadeGranted()) return;
        data.markStartingJadeGranted();
        if (hasJadeGuide(player)) return;
        ItemStack jade = new ItemStack(ModItems.JADE_GUIDE.get());
        if (!player.getInventory().add(jade)) player.drop(jade, false);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            ImmortalStoragePlayerData.get(p).syncTo(p);
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
        // Reconcile on the server task queue so ImmortalStorage flight and effects win last.
        com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(p.level()).execute(() -> restoreStageEffects(p));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            com.immortalstorage.immortalstorage.player.HeldItemAutoRefill.clear(player);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent e) {
        markPersonalRealmModified(e.getLevel(), e.getPos());
        if (ImmortalStorageConfig.SOURCE_ALLOW_OTHER_PLAYER_BREAK.get()) return;
        if (!(e.getLevel() instanceof Level level)) return;
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(e.getPos()) instanceof SourceVeinBlockEntity source)) return;
        Player player = e.getPlayer();
        if (player instanceof ServerPlayer sp && com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.hasPermissions(sp, 2)) return;
        if (!source.isUnowned() && !com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.matches(player, source.getOwner())) {
            e.setCanceled(true);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, net.minecraft.network.chat.Component.literal("Only the source block owner can break it."), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRespawnAnchorBroken(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) return;
        for (ServerPlayer player : com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).getPlayerList().getPlayers()) {
            clearMatchingPuppetAnchors(player, level.dimension(), event.getPos());
        }
    }

    private static void clearMatchingPuppetAnchors(ServerPlayer player,
                                                   net.minecraft.resources.ResourceKey<Level> dimension,
                                                   BlockPos pos) {
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .clearAnchorIfMatches(stack, dimension, pos);
        }
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.armor(player)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .clearAnchorIfMatches(stack, dimension, pos);
        }
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.slot(player, net.minecraft.world.entity.EquipmentSlot.OFFHAND)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .clearAnchorIfMatches(stack, dimension, pos);
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        for (ItemStack stack : data.getKongqiaoItems()) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .clearAnchorIfMatches(stack, dimension, pos);
        }
        java.util.List<ItemStack> xianqiao = data.getXianqiaoStorageItems();
        for (int slot = 0; slot < xianqiao.size(); slot++) {
            ItemStack stack = xianqiao.get(slot);
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .clearAnchorIfMatches(stack, dimension, pos)) {
                data.setXianqiaoSlot(slot, stack);
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        markPersonalRealmModified(e.getLevel(), e.getPos());
    }

    private void markPersonalRealmModified(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        // Chokepoint for the realm "player-modified chunks" set that drives
        // forced chunk loading. Only entity-driven break/place events reach
        // here; vanilla weather snow uses setBlock with a null entity and so
        // can never be recorded as a player modification.
        if (!(level instanceof ServerLevel serverLevel)) return;
        java.util.Optional<java.util.UUID> ownerId =
                com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions.personalRealmOwner(serverLevel.dimension());
        if (ownerId.isEmpty()) return;
        ServerPlayer owner = com.immortalstorage.immortalstorage.dimension.RealmHelper
                .onlinePlayerForRealm(serverLevel.getServer(), ownerId.get());
        if (owner != null) {
            com.immortalstorage.immortalstorage.dimension.RealmHelper.markModifiedChunk(owner, pos);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof Level level)) return;
        if (level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) {
            for (BlockPos pos : e.getAffectedBlocks()) {
                if (!level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) continue;
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    clearMatchingPuppetAnchors(player, serverLevel.dimension(), pos);
                }
            }
        }
        if (ImmortalStorageConfig.SOURCE_ALLOW_MOB_BREAK.get()) return;
        if (e.getExplosion().getDirectSourceEntity() instanceof Player) return;
        e.getAffectedBlocks().removeIf(pos -> level.getBlockEntity(pos) instanceof SourceVeinBlockEntity);
    }

    @SubscribeEvent
    public void onPlayerSleep(CanPlayerSleepEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide()) return;
        if (!(p instanceof ServerPlayer serverPlayer)) return;
        ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
        if (d.tryJadeSleepInitiation(hasJadeGuide(p))) {
            com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.STAGE_1.trigger(serverPlayer);
        }
    }

    private static void tickPersonalStorageMagnet(ServerPlayer player, ImmortalStoragePlayerData data) {
        if (data.getStage() < 4 || !data.isMagnetEnabled()) return;
        var endpoint = com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi.resolve(
                com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()), com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player));
        if (endpoint == null) return;
        for (ItemEntity entity : ((net.minecraft.server.level.ServerLevel) player.level()).getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(6.0D), ItemEntity::isAlive)) {
            ItemStack original = entity.getItem();
            if (original.isEmpty()) continue;
            ItemStack remainder = endpoint.insert(original.copy(), false);
            entity.setItem(remainder);
            if (remainder.isEmpty()) entity.discard();
        }
    }

    private static boolean hasJadeGuide(Player p) {
        for (ItemStack s : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(p)) if (s.is(ModItems.JADE_GUIDE.get())) return true;
        for (ItemStack s : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.slot(p, net.minecraft.world.entity.EquipmentSlot.OFFHAND)) if (s.is(ModItems.JADE_GUIDE.get())) return true;
        return false;
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre e) {
        ItemEntity ent = e.getItemEntity();
        if (ent == null) return;
        if (ent.getItem().getItem() instanceof com.immortalstorage.immortalstorage.item.custom.TrueYuanItem) {
            e.setCanPickup(net.minecraft.util.TriState.FALSE);
            Player p = e.getPlayer();
            if (p instanceof ServerPlayer sp) {
                ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
                long accepted = d.depositTrueYuan(ent.getItem().getCount());
                if (accepted > 0L) ent.getItem().shrink((int) accepted);
                if (ent.getItem().isEmpty()) ent.discard();
            }
            return;
        }
        if (ent.getItem().getItem() instanceof com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem) {
            e.setCanPickup(net.minecraft.util.TriState.FALSE);
            Player p = e.getPlayer();
            if (p instanceof ServerPlayer sp) {
                ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
                long accepted = d.depositImmortalYuan(ent.getItem().getCount());
                if (accepted > 0L) ent.getItem().shrink((int) accepted);
                if (ent.getItem().isEmpty()) ent.discard();
            }
            return;
        }
        // The explicit stage-four magnet performs direct, transactional storage insertion on player tick.
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent e) {
        // TrueYuan/ImmortalYuan item classes already cancel their own toss.
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof LivingEntity dead
                && com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(dead.getPersistentData(), TribulationHelper.NBT_TRIB_ATTEMPT)) {
            TribulationHelper.onTargetDeath(dead);
        }
        if (e.getEntity() instanceof ServerPlayer p) {
            ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(p);
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
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, net.minecraft.network.chat.Component.literal(
                        "Tribulation failed. Immortal Yuan was cleared."), true);
                return;
            }
            SubstitutePuppetLocation puppetLocation = findOwnedSubstitutePuppet(p, data);
            ItemStack puppet = puppetLocation.stack();
            if (!puppet.isEmpty()
                    && com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.consumeUse(puppet)) {
                e.setCanceled(true);
                p.setHealth(1.0F);
                p.removeAllEffects();
                p.clearFire();
                p.fallDistance = 0.0F;
                p.invulnerableTime = 40;
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.teleportToAnchor(p, puppet);
                puppetLocation.commit().run();
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                        new com.immortalstorage.immortalstorage.network.ModPayloads.ShowSubstitutePuppetActivation(
                                puppet.copyWithCount(1)));
                restoreStageEffects(p);
            }
        }
    }

    private static SubstitutePuppetLocation findOwnedSubstitutePuppet(ServerPlayer player, ImmortalStoragePlayerData data) {
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) {
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .isOwnedBy(stack, com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player)) && stack.getDamageValue() < 16) return new SubstitutePuppetLocation(stack, () -> {});
        }
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.slot(player, net.minecraft.world.entity.EquipmentSlot.OFFHAND)) {
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .isOwnedBy(stack, com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player)) && stack.getDamageValue() < 16) return new SubstitutePuppetLocation(stack, () -> {});
        }
        for (ItemStack stack : data.getKongqiaoItems()) {
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .isOwnedBy(stack, com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player)) && stack.getDamageValue() < 16) return new SubstitutePuppetLocation(stack, () -> {});
        }
        java.util.List<ItemStack> xianqiao = data.getXianqiaoStorageItems();
        for (int slot = 0; slot < xianqiao.size(); slot++) {
            ItemStack stack = xianqiao.get(slot);
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem
                    .isOwnedBy(stack, com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player)) && stack.getDamageValue() < 16) {
                int storageSlot = slot;
                return new SubstitutePuppetLocation(stack, () -> data.setXianqiaoSlot(storageSlot, stack));
            }
        }
        return new SubstitutePuppetLocation(ItemStack.EMPTY, () -> {});
    }

    private record SubstitutePuppetLocation(ItemStack stack, Runnable commit) {}

    private static void clearInvalidPuppetAnchors(ServerPlayer player, ImmortalStoragePlayerData data) {
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.clearInvalidAnchor(player, stack);
        }
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.armor(player)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.clearInvalidAnchor(player, stack);
        }
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.slot(player, net.minecraft.world.entity.EquipmentSlot.OFFHAND)) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.clearInvalidAnchor(player, stack);
        }
        for (ItemStack stack : data.getKongqiaoItems()) {
            com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.clearInvalidAnchor(player, stack);
        }
        java.util.List<ItemStack> xianqiao = data.getXianqiaoStorageItems();
        for (int slot = 0; slot < xianqiao.size(); slot++) {
            ItemStack stack = xianqiao.get(slot);
            if (com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.clearInvalidAnchor(player, stack)) {
                data.setXianqiaoSlot(slot, stack);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingIncomingDamageEvent e) {
        // Reserved for cultivation-specific incoming-damage rules.
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity().level().isClientSide()) return;
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
        tickPersonalStorageMagnet(p, d);
        com.immortalstorage.immortalstorage.player.HeldItemAutoRefill.tick(p, d);
        if (d.tickJadeInitiation(hasJadeGuide(p))) {
            com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.STAGE_1.trigger(p);
        }
        d.serverTick(p);
        if (d.getStage() >= 5) {
            d.getEmbeddedImmortalFurnace().tick(p);
        }
        if (d.isTribulationActive()) {
            tickTribulation(p, d);
            suppressImmortalStorageBuffs(p);
        } else {
            applyStageEffects(p, d);
        }
        // Personal realm: keep the owner's active chunks force-loaded.
        if (com.immortalstorage.immortalstorage.dimension.RealmHelper.isInOwnRealm(p)) {
            com.immortalstorage.immortalstorage.dimension.RealmHelper.enforcePlayerBoundary(p);
            com.immortalstorage.immortalstorage.dimension.RealmHelper.ensureChunksForced(p);
        }
    }

    private void tickTribulation(ServerPlayer p, ImmortalStoragePlayerData d) {
        if (!com.immortalstorage.immortalstorage.dimension.RealmHelper.isInOwnRealm(p)) {
            TribulationHelper.fail(p);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, net.minecraft.network.chat.Component.literal(
                    "Tribulation failed after leaving the personal realm."), true);
            return;
        }
        if (com.immortalstorage.immortalstorage.progression.TribulationPolicy.requiresBlindness(d.getStage())) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false, true));
        }
        if (com.immortalstorage.immortalstorage.progression.TribulationPolicy.requiresWither(d.getStage())) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, false, true));
        }
        TribulationHelper.reconcile(p);
    }

    private static void suppressImmortalStorageBuffs(ServerPlayer p) {
        p.removeEffect(MobEffects.STRENGTH);
        p.removeEffect(MobEffects.SPEED);
        p.removeEffect(MobEffects.REGENERATION);
        p.removeEffect(MobEffects.RESISTANCE);
        p.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.LINGQI_SATURATION.get()));
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    public static void restoreStageEffects(ServerPlayer p) {
        if (p == null) return;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(p);
        if (data.isTribulationActive()) {
            suppressImmortalStorageBuffs(p);
            return;
        }
        applyStageEffects(p, data);
    }

    private static void applyStageEffects(ServerPlayer p, ImmortalStoragePlayerData d) {
        boolean weak = d.isAdvancedWeak() && d.getAdvancedWeakTicks() > 0;
        if (weak) {
            Holder<net.minecraft.world.effect.MobEffect> advancedWeakness = BuiltInRegistries.MOB_EFFECT
                    .wrapAsHolder(ModEffects.ADVANCED_WEAKNESS.get());
            MobEffectInstance currentWeakness = p.getEffect(advancedWeakness);
            if (currentWeakness == null) {
                p.addEffect(new MobEffectInstance(advancedWeakness, d.getAdvancedWeakTicks(),
                        0, false, true, true));
            }
            p.removeEffect(MobEffects.STRENGTH);
            p.removeEffect(MobEffects.SPEED);
            p.removeEffect(MobEffects.REGENERATION);
            p.removeEffect(MobEffects.RESISTANCE);
            return;
        }
        int st = d.getStage();
        int strength = st >= 9 ? 4 : st >= 7 ? 3 : st >= 5 ? 2 : st >= 3 ? 1 : st >= 1 ? 0 : -1;
        int speed = st >= 3 ? 1 : st >= 1 ? 0 : -1;
        int regeneration = st >= 10 ? 7 : st >= 9 ? 4 : st >= 8 ? 3 : st >= 7 ? 2
                : st >= 4 ? 1 : st >= 2 ? 0 : -1;
        int resistance = st >= 10 ? 4 : st >= 9 ? 3 : st >= 6 ? 2 : st >= 4 ? 1
                : st >= 2 ? 0 : -1;

        ensureStageEffect(p, MobEffects.STRENGTH, strength);
        ensureStageEffect(p, MobEffects.SPEED, speed);
        ensureStageEffect(p, MobEffects.REGENERATION, regeneration);
        ensureStageEffect(p, MobEffects.RESISTANCE, resistance);
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
