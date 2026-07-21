package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

public class SpiritSwordItem extends SwordItem {
    public SpiritSwordItem(Item.Properties props) {
        super(ModItemsTierAccess.SPIRIT_MATERIAL, props.fireResistant().durability(2500));
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return new ItemStack(Items.NETHERITE_SWORD).supportsEnchantment(enchantment);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player p) {
        super.onCraftedBy(stack, level, p);
        if (!level.isClientSide) {
            ModEnchantments.applySpiritRepair(stack, level.registryAccess());
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        markUsed(stack, attacker.level().getGameTime());
        float ordinaryDamage = SpiritSwordCombatModel.BASE_DAMAGE;
        if (attacker instanceof Player p && !p.level().isClientSide) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            SpiritSwordCombatModel.Profile profile = SpiritSwordCombatModel.forStage(d.getStage());
            boolean paid = switch (profile.cost()) {
                case NONE -> profile.stage() >= 10;
                case TRUE -> d.consumeTrueYuan(profile.costAmount());
                case IMMORTAL -> d.consumeImmortalYuan(profile.costAmount());
            };
            float stageBonus = paid ? profile.bonusDamage() : 0.0F;
            ordinaryDamage += stageBonus;
            long tempering = SpiritSwordTempering.consumeHalf(stack);
            float temperingBonus = SpiritSwordTempering.bonusDamage(stack, ordinaryDamage, tempering);
            float combinedBonus = stageBonus + temperingBonus;
            if (combinedBonus > 0.0F) target.hurt(p.damageSources().playerAttack(p), combinedBonus);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    public static void markUsed(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong("spiritSwordLastUsed", Math.max(0L, gameTime));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long lastUsed(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getLong("spiritSwordLastUsed");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.immortalstorage.spirit_sword.tempering",
                SpiritSwordTempering.points(stack)));
    }

}
