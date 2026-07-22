package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.entity.PrimordialQiConversion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Converts a living entity into its spawn egg after a two-second visual collapse. */
public final class PrimordialQiItem extends Item {
    public PrimordialQiItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(player)
                .isTribulationActive()) return InteractionResult.FAIL;
        if (target instanceof Player || PrimordialQiConversion.isConverting(target)) return InteractionResult.PASS;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (ImmortalStorageConfig.PRIMORDIAL_QI_ENTITY_BLACKLIST.get().contains(typeId.toString())) {
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer) {
            PrimordialQiConversion.begin(target);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
