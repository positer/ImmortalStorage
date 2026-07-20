package com.cultivation.cultivation.compat.buildinggadgets;

import com.cultivation.cultivation.api.storage.terminal.StorageItemSummary;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Optional Building Gadgets 2 material access without a hard runtime dependency. */
public final class BuildingGadgetsStorageBridge {
    private static final ResourceLocation COPY_PASTE_GADGET =
            ResourceLocation.fromNamespaceAndPath("buildinggadgets2", "gadget_copy_paste");

    private BuildingGadgetsStorageBridge() {
    }

    public static void satisfyRequestedItems(Player player, List<ItemStack> requested, boolean simulate) {
        if (player == null || requested == null || requested.isEmpty() || !holdsCopyPasteGadget(player)) return;
        CultivationPlayerData data = CultivationPlayerData.get(player);
        Map<TerminalEntryKey, Long> available = availableItems(data);
        for (Iterator<ItemStack> iterator = requested.iterator(); iterator.hasNext();) {
            ItemStack request = iterator.next();
            if (request == null || request.isEmpty()) continue;
            TerminalEntryKey key = TerminalEntryKey.of(request);
            long remaining = available.getOrDefault(key, 0L);
            if (remaining < request.getCount()) continue;
            if (!simulate) {
                long planned = data.getStage() >= 6
                        ? data.extractXianqiaoItem(key, request.getCount(), TerminalStorageAction.SIMULATE)
                        : data.simulateExtractStack(request, request.getCount()).getCount();
                if (planned != request.getCount()) continue;
                long extracted = data.getStage() >= 6
                        ? data.extractXianqiaoItem(key, planned, TerminalStorageAction.EXECUTE)
                        : data.extractStack(request, (int) planned).getCount();
                if (extracted != request.getCount()) continue;
            }
            available.put(key, remaining - request.getCount());
            iterator.remove();
        }
    }

    public static int countAvailable(Player player, ItemStack template) {
        if (player == null || template == null || template.isEmpty() || !holdsCopyPasteGadget(player)) return 0;
        long count = availableItems(CultivationPlayerData.get(player))
                .getOrDefault(TerminalEntryKey.of(template), 0L);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, count));
    }

    private static Map<TerminalEntryKey, Long> availableItems(CultivationPlayerData data) {
        Map<TerminalEntryKey, Long> available = new HashMap<>();
        if (data.getStage() >= 6) {
            for (StorageItemSummary summary : data.getXianqiaoItemSummary()) {
                available.merge(TerminalEntryKey.of(summary.prototype()), summary.amount(), Long::sum);
            }
        } else {
            for (ItemStack stack : data.getKongqiaoItems()) {
                if (!stack.isEmpty()) available.merge(TerminalEntryKey.of(stack), (long) stack.getCount(), Long::sum);
            }
        }
        return available;
    }

    private static boolean holdsCopyPasteGadget(Player player) {
        return isCopyPasteGadget(player.getMainHandItem()) || isCopyPasteGadget(player.getOffhandItem());
    }

    private static boolean isCopyPasteGadget(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && COPY_PASTE_GADGET.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
