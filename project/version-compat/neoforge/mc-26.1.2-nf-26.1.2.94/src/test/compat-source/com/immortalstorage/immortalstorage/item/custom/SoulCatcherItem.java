package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Reusable single-entity containment tool. */
public final class SoulCatcherItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    private static final String ENTITY_TAG = "containedEntity";
    private static final String NAME_TAG = "containedName";

    public SoulCatcherItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (ImmortalStoragePlayerData.get(player).isTribulationActive() || hasEntity(stack)
                || target instanceof Player || !target.isAlive()) return InteractionResult.FAIL;
        CompoundTag entityTag = new CompoundTag();
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.saveEntity(target, entityTag)) return InteractionResult.FAIL;
        entityTag.remove("UUID");
        entityTag.remove("Pos");
        entityTag.remove("Motion");
        entityTag.remove("Rotation");
        CompoundTag root = custom(stack);
        root.put(ENTITY_TAG, entityTag);
        root.putString(NAME_TAG, target.getDisplayName().getString());
        ItemStack captured = stack.copy();
        captured.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        player.setItemInHand(hand, captured);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
        }
        target.discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || ImmortalStoragePlayerData.get(player).isTribulationActive() || !hasEntity(stack)) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel level = (ServerLevel) context.getLevel();
        CompoundTag root = custom(stack);
        CompoundTag entityTag = root.getCompoundOrEmpty(ENTITY_TAG);
        Vec3 spawn = context.getClickLocation().add(new Vec3(context.getClickedFace().getStepX(), context.getClickedFace().getStepY(), context.getClickedFace().getStepZ())
                .scale(0.55D));
        Entity entity = EntityType.loadEntityRecursive(entityTag, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> {
            loaded.snapTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
            return loaded;
        });
        if (entity == null || !level.noCollision(entity) || !level.addFreshEntity(entity)) {
            return InteractionResult.FAIL;
        }
        root.remove(ENTITY_TAG);
        root.remove(NAME_TAG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return InteractionResult.CONSUME;
    }

    public static boolean hasEntity(ItemStack stack) {
        return custom(stack).contains(ENTITY_TAG);
    }

    public static CompoundTag containedEntity(ItemStack stack) {
        return custom(stack).getCompoundOrEmpty(ENTITY_TAG).copy();
    }

    private static CompoundTag custom(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEntity(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag tag = custom(stack);
        tooltip.add(Component.translatable("tooltip.immortalstorage.soul_catcher.contained",
                tag.contains(NAME_TAG) ? tag.getStringOr(NAME_TAG, "")
                        : Component.translatable("tooltip.immortalstorage.soul_catcher.empty").getString()));
        tooltip.add(Component.translatable("tooltip.immortalstorage.soul_catcher.use"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.soul_catcher.tribulation"));
    }
}
