package com.cultivation.cultivation.compat.merchant;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

import java.util.ArrayList;
import java.util.List;

/** Transactional payment planner for vanilla and mods using MerchantMenu. */
public final class MerchantStoragePayments {
    private MerchantStoragePayments() {}

    public static void fill(MerchantMenu menu, Merchant trader, int offerIndex) {
        if (trader.isClientSide() || offerIndex < 0 || offerIndex >= menu.getOffers().size()) return;
        if (!(trader.getTradingPlayer() instanceof ServerPlayer player)) return;
        CultivationPlayerData data = CultivationPlayerData.get(player);
        var offer = menu.getOffers().get(offerIndex);
        List<Payment> payments = new ArrayList<>(2);
        payments.add(new Payment(0, offer.getItemCostA()));
        offer.getItemCostB().ifPresent(cost -> payments.add(new Payment(1, cost)));

        List<Plan> plan = new ArrayList<>();
        List<ItemStack> snapshot = data.snapshotStorage(!data.isStorageIsKongqiaoLegacy()).stream()
                .map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (Payment payment : payments) {
            ItemStack current = menu.getSlot(payment.slot()).getItem();
            int needed = Math.max(0, payment.cost().count() - (current.isEmpty() ? 0 : current.getCount()));
            if (needed == 0) continue;
            long available = 0L;
            for (ItemStack stored : snapshot) {
                if (!stored.isEmpty() && payment.cost().test(stored)) available += stored.getCount();
            }
            if (available < needed) return;
            int remaining = needed;
            for (ItemStack stored : snapshot) {
                if (remaining <= 0) break;
                if (!stored.isEmpty() && payment.cost().test(stored)) {
                    int take = Math.min(remaining, stored.getCount());
                    plan.add(new Plan(payment.slot(), stored.copyWithCount(take)));
                    stored.shrink(take);
                    remaining -= take;
                }
            }
        }
        List<ItemStack> extractedAll = new ArrayList<>();
        for (Plan entry : plan) {
            ItemStack extracted = data.extractStack(entry.template(), entry.template().getCount());
            if (extracted.getCount() != entry.template().getCount()) {
                if (!extracted.isEmpty()) extractedAll.add(extracted);
                for (ItemStack rollback : extractedAll) data.insertStack(rollback, true);
                return;
            }
            extractedAll.add(extracted.copy());
        }
        for (int index = 0; index < plan.size(); index++) {
            Plan entry = plan.get(index);
            ItemStack extracted = extractedAll.get(index);
            ItemStack payment = menu.getSlot(entry.slot()).getItem();
            ItemStack merged = payment.isEmpty() ? extracted : payment.copyWithCount(payment.getCount() + extracted.getCount());
            menu.getSlot(entry.slot()).setByPlayer(merged);
        }
        menu.broadcastChanges();
    }

    private record Payment(int slot, ItemCost cost) {}
    private record Plan(int slot, ItemStack template) {}
}
