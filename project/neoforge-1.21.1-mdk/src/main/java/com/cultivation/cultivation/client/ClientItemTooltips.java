package com.cultivation.cultivation.client;

import com.cultivation.cultivation.item.ModItems;
import com.cultivation.cultivation.item.custom.SpiritSwordCombatModel;
import com.cultivation.cultivation.item.custom.SpiritSwordTempering;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

final class ClientItemTooltips {
    static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModItems.SPIRIT_SWORD.get())) return;
        int stage = event.getEntity() == null ? 0 : CultivationPlayerData.get(event.getEntity()).getStage();
        SpiritSwordCombatModel.Profile profile = SpiritSwordCombatModel.forStage(stage);
        long tempering = SpiritSwordTempering.points(event.getItemStack());
        long percent = tempering > Long.MAX_VALUE / 1L ? Long.MAX_VALUE : tempering;
        float unpaidTemperingDamage = SpiritSwordTempering.bonusDamage(
                SpiritSwordCombatModel.BASE_DAMAGE, tempering);
        float paidTemperingDamage = SpiritSwordTempering.bonusDamage(
                profile.successfulHitDamage(), tempering);
        event.getToolTip().add(Component.translatable("tooltip.cultivation.spirit_sword.formula",
                SpiritSwordCombatModel.BASE_DAMAGE, profile.stage(), profile.bonusDamage())
                .withStyle(ChatFormatting.AQUA));
        String paymentKey = switch (profile.cost()) {
            case TRUE -> "tooltip.cultivation.spirit_sword.payment.true";
            case IMMORTAL -> "tooltip.cultivation.spirit_sword.payment.immortal";
            case NONE -> profile.stage() >= 10
                    ? "tooltip.cultivation.spirit_sword.payment.free"
                    : "tooltip.cultivation.spirit_sword.payment.inactive";
        };
        event.getToolTip().add(Component.translatable(paymentKey, profile.costAmount())
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.cultivation.spirit_sword.tempering_detail",
                tempering, percent, paidTemperingDamage, unpaidTemperingDamage)
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.cultivation.spirit_sword.tempering_decay")
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("tooltip.cultivation.spirit_sword.server_rule")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private ClientItemTooltips() {}
}
