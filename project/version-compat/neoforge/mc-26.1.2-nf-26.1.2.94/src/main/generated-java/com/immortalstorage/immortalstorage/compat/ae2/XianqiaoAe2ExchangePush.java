package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The one settlement path every AE2 exchange crafting provider shares.
 *
 * <p>Two hosts publish exchange patterns on a grid: the Xianqiao Manager node
 * ({@link XianqiaoAe2CraftingProvider}) and a drive that holds a Xianqiao exchange disk
 * ({@link XianqiaoAe2DriveCraftingProvider}). Both must take the network's Immortal Yuan and
 * produce through the same vein conversion; keeping that in one place is what prevents the two
 * hosts from drifting into two different payment orders.</p>
 *
 * <p><b>Order of operations (unchanged from the manager-only implementation):</b> simulate vein
 * production, simulate the network insert, then deposit the pushed yuan, remove it from the
 * pushed input buffer, execute production, and insert what was produced. A push that cannot
 * deliver a complete batch never takes the network's yuan.</p>
 *
 * <p><b>Known gaps (deliberately not guessed, inherited from the manager provider):</b></p>
 * <ol>
 *   <li>If the vein produces less than the simulated batch after the deposit, the deposit is not
 *       refunded. A wallet-level reserve/settle pair is still the real fix.</li>
 *   <li>Zero-cost offers are never published; see {@link XianqiaoAe2ExchangePattern#of}.</li>
 * </ol>
 */
final class XianqiaoAe2ExchangePush {
    private static final IActionSource NO_SOURCE = IActionSource.empty();

    private XianqiaoAe2ExchangePush() {}

    /** Pattern icon; display-only, AE2 indexes providers by the primary output. */
    static @Nullable AEItemKey definitionKey() {
        return AEItemKey.of(ModBlocks.XIANQIAO_MANAGER.get());
    }

    /** The single consumed input key of every published exchange pattern. */
    static @Nullable AEItemKey yuanKey() {
        return AEItemKey.of(ModItems.IMMORTAL_YUAN.get());
    }

    /**
     * Executes one pushed exchange pattern batch.
     *
     * @param node   the managed node that owns the provider (network access for the insert)
     * @param owner  the player whose Xianqiao storage and wallet pay for the batch
     * @param level  the provider host's level; only a {@link ServerLevel} can settle
     * @param offer  the offer the pushed pattern was built from
     * @param inputs the input buffers AE2 extracted for this push
     * @return {@code true} only when a complete batch entered the network
     */
    static boolean push(IManagedGridNode node, UUID owner, @Nullable Level level,
                        SourceExchangeOffer offer, KeyCounter[] inputs) {
        if (node == null || owner == null || offer == null) return false;
        if (inputs == null || inputs.length != 1) return false;
        if (offer.yuanPerBatch() <= 0L) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;
        MinecraftServer server = serverLevel.getServer();
        if (server == null) return false;

        IGrid grid = node.getGrid();
        if (grid == null) return false;
        MEStorage network = grid.getStorageService().getInventory();
        if (network == null) return false;

        AEItemKey yuan = yuanKey();
        if (yuan == null) return false;
        long availableYuan = inputs[0].get(yuan);
        if (availableYuan < offer.yuanPerBatch()) return false;
        // Finish the WHOLE order in this one push: AE2 hands a provider the yuan for the entire
        // craft, so treating that as a single batch leaves the order to drain one batch per tick.
        long batches = availableYuan / offer.yuanPerBatch();
        long yuanCost = batches * offer.yuanPerBatch();
        long outputs = batches * offer.outputsPerBatch();

        AEKey outputKey = XianqiaoAe2ExchangePattern.outputKey(offer);
        if (outputKey == null) return false;

        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(server, owner);
        if (player == null) return false;

        // Simulate both halves first: a push that cannot deliver the whole order must not take the
        // network's yuan or leave the vein's cache charged.
        if (produce(server, owner, offer, outputs, TerminalStorageAction.SIMULATE) < outputs) {
            return false;
        }
        if (network.insert(outputKey, outputs, Actionable.SIMULATE, NO_SOURCE) < outputs) {
            return false;
        }

        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        long cap = data.getImmortalYuanCapLong();
        long balance = data.getImmortalYuan();
        if (cap != Long.MAX_VALUE && balance > cap - yuanCost) return false;

        if (data.depositImmortalYuan(yuanCost) != yuanCost) return false;
        inputs[0].remove(yuan, yuanCost);

        long produced = produce(server, owner, offer, outputs, TerminalStorageAction.EXECUTE);
        if (produced <= 0L) return false;
        long inserted = network.insert(outputKey, produced, Actionable.MODULATE, NO_SOURCE);
        return inserted > 0L;
    }

    /**
     * Production is the existing on-demand conversion path: extracting the resource is what charges
     * the owner's yuan for exactly the batches that had to be converted.
     */
    private static long produce(MinecraftServer server, UUID owner, SourceExchangeOffer offer,
                                long outputs, TerminalStorageAction action) {
        if (offer.fluid()) {
            Fluid fluid = BuiltInRegistries.FLUID.get(offer.outputId()).map(net.minecraft.core.Holder.Reference::value).orElse(null);
            if (fluid == null) return 0L;
            FluidStack sample = new FluidStack(fluid, 1);
            return SourceVeinStorageIndex.extractFluid(server, owner,
                    TerminalFluidKey.of(sample), outputs, action);
        }
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(offer.outputId()).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        if (item == null) return 0L;
        ItemStack sample = new ItemStack(item, 1);
        return SourceVeinStorageIndex.extractItem(server, owner,
                TerminalEntryKey.of(sample), outputs, action);
    }
}
