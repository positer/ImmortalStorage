package com.immortalstorage.immortalstorage.compat.botania;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.CrystalResourceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Botania-only bootstrap. This class must be loaded reflectively only after
 * mod id {@code botania} and its audited version range are present.
 *
 * <p>Capability signatures follow Botania's official 1.21.1-porting API:
 * https://github.com/VazkiiMods/Botania/blob/1.21.1-porting/Xplat/src/main/java/vazkii/botania/api/mana/ManaReceiver.java
 * and SparkAttachable.java. No Botania type appears outside this package.</p>
 */
public final class BotaniaCompat {
    private static final Map<XianqiaoInterfaceBlockEntity, XianqiaoBotaniaManaAdapter> ADAPTERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EnergyCrystalBlockEntity, XianqiaoBotaniaManaAdapter>
            CRYSTAL_ADAPTERS = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;

    private static final CrystalResourceCompatHooks.Hook CRYSTAL_HOOK =
            new CrystalResourceCompatHooks.Hook() {
                @Override public boolean supports(CrystalKind kind) {
                    return kind == CrystalKind.MANA;
                }

                @Override public @Nullable Boolean pushToFace(
                        EnergyCrystalBlockEntity crystal, ServerLevel level,
                        Direction side, AtomicEnergyRefill.ResourceStore source) {
                    ManaReceiver receiver = ManaReceiver.LOOKUP.find(
                            level, crystal.getBlockPos().relative(side), side.getOpposite());
                    if (receiver == null || receiver.isFull()) return false;
                    long moved = 0L;
                    while (source.amount() > 0L && !receiver.isFull()) {
                        long available = receiver instanceof ManaPool pool
                                ? Math.max(0L, (long) pool.getMaxMana() - pool.getCurrentMana())
                                : Math.max(0L, (long) Integer.MAX_VALUE - receiver.getCurrentMana());
                        long offered = Math.min(source.amount(), Math.min(available, Integer.MAX_VALUE));
                        if (offered <= 0L) break;
                        int before = receiver.getCurrentMana();
                        receiver.receiveMana((int) offered);
                        int after = receiver.getCurrentMana();
                        long accepted = Math.max(0L, (long) after - before);
                        if (accepted <= 0L) break;
                        source.extract(accepted, ResourceTransferAction.EXECUTE);
                        moved += accepted;
                    }
                    return moved > 0L;
                }

                @Override public boolean acceptsInput(ItemStack stack) {
                    ManaItem item = ManaItem.LOOKUP.find(stack);
                    return item != null && item.getMaxMana() > 0;
                }

                @Override public @Nullable CrystalResourceCompatHooks.InputResult processInput(
                        EnergyCrystalBlockEntity crystal, ServerLevel level, ItemStack input,
                        AtomicEnergyRefill.ResourceStore local,
                        @Nullable AtomicEnergyRefill.ResourceStore external,
                        java.util.function.Predicate<ItemStack> canAcceptExtra) {
                    ManaItem item = ManaItem.LOOKUP.find(input);
                    if (item == null || item.getMaxMana() <= 0) return null;
                    ItemStack output = input.copyWithCount(1);
                    if (item.getMana() >= item.getMaxMana()) {
                        return canAcceptExtra.test(output)
                                ? CrystalResourceCompatHooks.InputResult.complete(output)
                                : new CrystalResourceCompatHooks.InputResult(false, null);
                    }
                    long moved = drainIntoManaItem(crystal, item, local);
                    if (external != null && item.getMana() < item.getMaxMana()) {
                        moved += drainIntoManaItem(crystal, item, external);
                    }
                    if (item.getMana() >= item.getMaxMana()) {
                        output = input.copyWithCount(1);
                        return canAcceptExtra.test(output)
                                ? CrystalResourceCompatHooks.InputResult.complete(output)
                                : new CrystalResourceCompatHooks.InputResult(moved > 0L, null);
                    }
                    return new CrystalResourceCompatHooks.InputResult(moved > 0L, null);
                }

                @Override public InteractionResult useItemOn(
                        EnergyCrystalBlockEntity crystal,
                        net.minecraft.world.entity.player.Player player,
                        ItemStack stack, net.minecraft.world.InteractionHand hand,
                        net.minecraft.world.phys.BlockHitResult hit) {
                    // Spark attachment is handled by Botania's registered
                    // SparkAttachable capability and keeps Botania's own rules.
                    return InteractionResult.PASS;
                }
            };

    /**
     * Installs loader-neutral live resolvers from the common endpoint layer.
     * The common layer may invoke this reflectively; its signatures contain no
     * Botania type and therefore remain safe when Botania is absent.
     */
    public static void installBridge(
            Function<XianqiaoInterfaceBlockEntity,
                    AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        ADAPTERS.clear();
        CRYSTAL_ADAPTERS.clear();
        CrystalResourceCompatHooks.register(CRYSTAL_HOOK);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        BlockCapability<ManaReceiver, Direction> manaReceiver =
                BotaniaForgeCapabilities.getBlockApiLookupById(ManaReceiver.LOOKUP);
        if (manaReceiver == null) {
            // Same token construction used by BotaniaForgeCapabilities itself;
            // this also avoids mod-event listener ordering assumptions.
            manaReceiver = BlockCapability.create(
                    ManaReceiver.ID, ManaReceiver.class, Direction.class);
        }

        BlockCapability<SparkAttachable, Void> sparkAttachable =
                BotaniaForgeCapabilities.getBlockApiLookupById(SparkAttachable.LOOKUP);
        if (sparkAttachable == null) {
            sparkAttachable = BlockCapability.createVoid(
                    SparkAttachable.ID, SparkAttachable.class);
        }

        event.registerBlockEntity(manaReceiver, ModBlockEntities.XIANQIAO_INTERFACE.get(),
                (blockEntity, direction) -> adapterOrNull(blockEntity));
        event.registerBlockEntity(sparkAttachable, ModBlockEntities.XIANQIAO_INTERFACE.get(),
                (blockEntity, context) -> adapterOrNull(blockEntity));
        event.registerBlockEntity(manaReceiver, ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get(),
                (blockEntity, direction) -> adapterOrNull(blockEntity));
        event.registerBlockEntity(sparkAttachable, ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get(),
                (blockEntity, context) -> adapterOrNull(blockEntity));
        if (ModBlockEntities.MANA_CRYSTAL != null) {
            event.registerBlockEntity(manaReceiver, ModBlockEntities.MANA_CRYSTAL.get(),
                    (blockEntity, direction) -> crystalAdapterOrNull(blockEntity));
            event.registerBlockEntity(sparkAttachable, ModBlockEntities.MANA_CRYSTAL.get(),
                    (blockEntity, context) -> crystalAdapterOrNull(blockEntity));
        }
        ImmortalStorageMod.LOG.info("[Compat/Botania] Registered owner-bound Xianqiao Interface mana and spark capabilities");
    }

    /**
     * Resolves a real Botania pool through its official capability and performs
     * one bounded server-thread refill. A personal-realm scheduler can invoke
     * this method reflectively without putting Botania types in common code.
     *
     * @return the transaction result, or {@code null} when the position does
     * not currently expose a Botania {@link ManaPool}
     */
    public static @Nullable AtomicEnergyRefill.Result refillPoolAt(
            Level level,
            BlockPos pos,
            @Nullable Direction side,
            boolean allowed,
            long perTickLimit,
            long manaPerImmortalYuan,
            AtomicEnergyRefill.ResourceStore manaStorage,
            AtomicEnergyRefill.ChargeSource immortalYuan,
            ResourceTransferAction action) {
        ManaReceiver receiver = ManaReceiver.LOOKUP.find(level, pos, side);
        if (!(receiver instanceof ManaPool pool)) return null;
        int current = pool.getCurrentMana();
        int maximum = pool.getMaxMana();
        int request = current < 0 || maximum < current ? 0 : maximum - current;
        return BotaniaManaTransfer.fillPoolIfAllowed(
                allowed, request, perTickLimit, manaPerImmortalYuan,
                manaStorage, immortalYuan, new BotaniaManaPoolTarget(pool), action);
    }

    private static @Nullable XianqiaoBotaniaManaAdapter adapterOrNull(
            XianqiaoInterfaceBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) return null;
        synchronized (ADAPTERS) {
            return ADAPTERS.computeIfAbsent(blockEntity, key ->
                    new XianqiaoBotaniaManaAdapter(
                            key.getLevel(), key.getBlockPos(),
                            () -> storageResolver.apply(key)));
        }
    }

    private static @Nullable XianqiaoBotaniaManaAdapter crystalAdapterOrNull(
            EnergyCrystalBlockEntity crystal) {
        if (crystal.getLevel() == null) return null;
        synchronized (CRYSTAL_ADAPTERS) {
            return CRYSTAL_ADAPTERS.computeIfAbsent(crystal, key ->
                    new XianqiaoBotaniaManaAdapter(
                            key.getLevel(), key.getBlockPos(),
                            () -> key.getLevel() instanceof ServerLevel server
                                    ? key.integrationResourceStore(server) : null,
                            () -> crystalSparkAttached(key),
                            ignored -> key.markSparkAttached()));
        }
    }

    /**
     * Botania persists a spark as an entity above its attachable.  Reconcile
     * that official entity lookup with the block-data flag after a world
     * reload so the capability cannot accept a duplicate spark.
     */
    private static boolean crystalSparkAttached(EnergyCrystalBlockEntity crystal) {
        if (crystal.sparkAttached()) return true;
        Level level = crystal.getLevel();
        if (level == null) return false;
        if (SparkAttachable.getAttachedSpark(level, crystal.getBlockPos()) != null) {
            crystal.markSparkAttached();
            return true;
        }
        return false;
    }

    private static long drainIntoManaItem(
            BlockEntity crystal, ManaItem item, AtomicEnergyRefill.ResourceStore source) {
        if (!item.canReceiveManaFromPool(crystal)) return 0L;
        long room = Math.max(0L, (long) item.getMaxMana() - item.getMana());
        long offered = Math.min(source.amount(), Math.min(room, Integer.MAX_VALUE));
        if (offered <= 0L) return 0L;
        int before = item.getMana();
        item.addMana((int) offered);
        long accepted = Math.max(0L, (long) item.getMana() - before);
        if (accepted > 0L) source.extract(accepted, ResourceTransferAction.EXECUTE);
        return accepted;
    }

    private BotaniaCompat() {
    }
}
