package com.immortalstorage.immortalstorage.compat.botania;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
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

    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;

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

    private BotaniaCompat() {
    }
}
