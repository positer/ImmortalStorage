package com.cultivation.cultivation.compat.botania;

import com.cultivation.core.resource.AtomicEnergyRefill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Official Botania int facade over one live long-valued Xianqiao mana store.
 *
 * <p>The supplier is deliberately re-resolved for every operation. Logging
 * out, dropping below stage eight or replacing the owning attachment therefore
 * fails closed even when NeoForge has cached this capability object. Botania's
 * spark network has no face context, so this adapter intentionally ignores the
 * interface's six sided item/fluid/energy modes and transacts directly with the
 * mana cache. Spark augments decide network flow direction.</p>
 */
public final class XianqiaoBotaniaManaAdapter implements ManaPool, SparkAttachable {
    private final Level level;
    private final BlockPos pos;
    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;

    public XianqiaoBotaniaManaAdapter(
            Level level,
            BlockPos pos,
            Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        this.level = Objects.requireNonNull(level, "level");
        this.pos = Objects.requireNonNull(pos, "pos").immutable();
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return pos;
    }

    @Override
    public int getCurrentMana() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current == null ? 0 : BotaniaManaWindow.currentMana(current);
    }

    @Override
    public int getMaxMana() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFull() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current == null || BotaniaManaWindow.isFull(current);
    }

    @Override
    public void receiveMana(int delta) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return;
        BotaniaManaWindow.receiveMana(current, delta);
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return storage.get() != null && !isFull();
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }

    @Override
    public void attachSpark(ManaSpark entity) {
        // No local spark state is needed; Botania owns and queries the entity.
    }

    @Override
    public int getAvailableSpaceForMana() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return 0;
        return BotaniaManaWindow.availableSpace(current);
    }

    @Override
    public boolean areIncomingTransfersDone() {
        return storage.get() == null || isFull();
    }

}
