package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XianqiaoRsStorageReadInteractionTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void repeatedLongReadsShareOneSnapshotAndRevisionChangesRefreshIt() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000421");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000422");
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        AtomicReference<List<StorageItemSummary>> entries = new AtomicReference<>(
                List.of(new StorageItemSummary(diamond, 73L)));
        AtomicInteger revision = new AtomicInteger(1);
        AtomicInteger snapshots = new AtomicInteger();
        TerminalItemStorage items = new TerminalItemStorage() {
            @Override public long revision() { return revision.get(); }
            @Override public List<StorageItemSummary> snapshot() {
                snapshots.incrementAndGet();
                return entries.get();
            }
            @Override public long insert(TerminalEntryKey ignored, long amount, TerminalStorageAction action) {
                return 0L;
            }
            @Override public long extract(TerminalEntryKey ignored, long amount, TerminalStorageAction action) {
                return 0L;
            }
        };
        PersonalStorageEndpoint endpoint = endpoint(owner, items);
        XianqiaoRsStorage storage = new XianqiaoRsStorage(
                owner, disk, (server, requestedOwner) ->
                        owner.equals(requestedOwner) ? endpoint : null);

        ResourceAmount first = storage.getAll().iterator().next();
        assertEquals(73L, first.amount());
        assertEquals(73L, storage.getStored());
        assertEquals(1, snapshots.get(), "getStored must reuse the current long read view");

        long additional = 2L * Integer.MAX_VALUE + 17L;
        entries.set(List.of(new StorageItemSummary(diamond, 73L + additional)));
        revision.incrementAndGet();

        assertEquals(73L + additional, storage.getStored());
        assertEquals(2, snapshots.get(),
                "a changed storage revision must trigger exactly one refreshed scan");
        assertEquals(73L + additional, storage.getAll().iterator().next().amount());
        assertEquals(2, snapshots.get(), "the refreshed long view must remain reusable");
    }

    private static PersonalStorageEndpoint endpoint(UUID owner, TerminalItemStorage items) {
        IItemHandler unusedStackBridge = new ItemStackHandler();
        return new PersonalStorageEndpoint() {
            @Override public UUID owner() { return owner; }
            @Override public int stage() { return 6; }
            @Override public boolean online() { return true; }
            @Override public IItemHandler itemHandler() { return unusedStackBridge; }
            @Override public ItemStack insert(ItemStack stack, boolean simulate) { return stack; }
            @Override public ItemStack extract(ItemStack template, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
            @Override public TerminalItemStorage itemStorage() { return items; }
        };
    }
}
