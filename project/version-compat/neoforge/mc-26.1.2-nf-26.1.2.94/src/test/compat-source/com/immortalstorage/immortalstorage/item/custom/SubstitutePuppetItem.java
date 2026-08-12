package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.UUID;

/** Owner-bound, rechargeable death substitute with an optional respawn-anchor target. */
public final class SubstitutePuppetItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public static final int MAX_DURABILITY = 16;
    private static final String OWNER = "Owner";
    private static final String OWNER_NAME = "OwnerName";
    private static final String ANCHOR_DIMENSION = "AnchorDimension";
    private static final String ANCHOR_POS = "AnchorPos";

    public SubstitutePuppetItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack puppet = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        CompoundTag tag = data(puppet);
        UUID stableOwner = PersistentPlayerIdentity.id(serverPlayer);
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER)) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(tag, OWNER, stableOwner);
            tag.putString(OWNER_NAME, player.getGameProfile().name());
            write(puppet, tag);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.substitute_puppet.bound"), true);
            return InteractionResult.CONSUME;
        }
        if (!migrateOwner(serverPlayer, puppet, tag)) return InteractionResult.FAIL;
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.is(ModItems.NURTURING_CRYSTAL.get())) {
            int missing = puppet.getDamageValue();
            int repairs = Math.min(missing, other.getCount() / 8);
            if (repairs > 0) {
                other.shrink(repairs * 8);
                puppet.setDamageValue(missing - repairs);
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable(
                        "message.immortalstorage.substitute_puppet.repaired", repairs), true);
                return InteractionResult.CONSUME;
            }
        }
        tag.remove(ANCHOR_DIMENSION);
        tag.remove(ANCHOR_POS);
        write(puppet, tag);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.substitute_puppet.anchor_cleared"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        ItemStack puppet = context.getItemInHand();
        CompoundTag tag = data(puppet);
        if (!migrateOwner(player, puppet, tag)) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) return InteractionResult.PASS;
        tag.putString(ANCHOR_DIMENSION, context.getLevel().dimension().identifier().toString());
        tag.putLong(ANCHOR_POS, pos.asLong());
        write(puppet, tag);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.substitute_puppet.anchor_bound"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.durability",
                MAX_DURABILITY - stack.getDamageValue(), MAX_DURABILITY));
        tooltip.add(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER)
                ? Component.translatable("tooltip.immortalstorage.substitute_puppet.owner",
                tag.contains(OWNER_NAME) ? tag.getStringOr(OWNER_NAME, "") : com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER).toString())
                : Component.translatable("tooltip.immortalstorage.substitute_puppet.owner_unbound"));
        if (tag.contains(ANCHOR_DIMENSION) && tag.contains(ANCHOR_POS)) {
            BlockPos pos = BlockPos.of(tag.getLongOr(ANCHOR_POS, 0L));
            tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.anchor",
                    tag.getStringOr(ANCHOR_DIMENSION, ""), pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.anchor_none"));
        }
    }

    public static boolean isOwnedBy(ItemStack stack, UUID owner) {
        CompoundTag tag = data(stack);
        return stack.is(ModItems.SUBSTITUTE_PUPPET.get()) && com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER) && com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER).equals(owner);
    }

    public static java.util.Optional<UUID> owner(ItemStack stack) {
        CompoundTag tag = data(stack);
        return stack != null && stack.is(ModItems.SUBSTITUTE_PUPPET.get()) && com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER)
                ? java.util.Optional.of(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER)) : java.util.Optional.empty();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        CompoundTag tag = data(stack);
        if (!migrateOwner(player, stack, tag)) return;
        String currentName = player.getGameProfile().name();
        if (!currentName.equals(tag.getStringOr(OWNER_NAME, ""))) {
            tag.putString(OWNER_NAME, currentName);
            write(stack, tag);
        }
        clearInvalidAnchor(player, stack);
    }

    public static boolean consumeUse(ItemStack stack) {
        if (stack.getDamageValue() >= MAX_DURABILITY) return false;
        if (stack.has(DataComponents.UNBREAKABLE)) return true;
        stack.setDamageValue(stack.getDamageValue() + 1);
        return true;
    }

    public static boolean clearAnchorIfMatches(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        if (!stack.is(ModItems.SUBSTITUTE_PUPPET.get())) return false;
        CompoundTag tag = data(stack);
        if (!tag.contains(ANCHOR_DIMENSION) || !tag.contains(ANCHOR_POS)) return false;
        if (!dimension.identifier().toString().equals(tag.getStringOr(ANCHOR_DIMENSION, ""))
                || tag.getLongOr(ANCHOR_POS, 0L) != pos.asLong()) return false;
        clearAnchor(stack, tag);
        return true;
    }

    public static boolean clearInvalidAnchor(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = data(stack);
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(ANCHOR_DIMENSION, ""));
        if (dimensionId == null || !tag.contains(ANCHOR_POS)) return false;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        var targetLevel = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).getLevel(key);
        if (targetLevel == null) {
            targetLevel = com.immortalstorage.immortalstorage.dimension.RealmHelper
                    .resolveOwnedPersonalRealm(player, key);
        }
        if (targetLevel == null || targetLevel.getBlockState(BlockPos.of(tag.getLongOr(ANCHOR_POS, 0L))).is(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        clearAnchor(stack, tag);
        return true;
    }

    public static boolean teleportToAnchor(ServerPlayer player, ItemStack stack) {
        if (clearInvalidAnchor(player, stack)) return false;
        CompoundTag tag = data(stack);
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(ANCHOR_DIMENSION, ""));
        if (dimensionId == null || !tag.contains(ANCHOR_POS)) return false;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        var targetLevel = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).getLevel(key);
        if (targetLevel == null) return false;
        BlockPos pos = BlockPos.of(tag.getLongOr(ANCHOR_POS, 0L));
        player.teleportTo(targetLevel, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        return true;
    }

    private static void clearAnchor(ItemStack stack, CompoundTag tag) {
        tag.remove(ANCHOR_DIMENSION);
        tag.remove(ANCHOR_POS);
        write(stack, tag);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Accept and atomically upgrade a pre-0.0.9 session-UUID owner. */
    private static boolean migrateOwner(ServerPlayer player, ItemStack stack, CompoundTag tag) {
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER)) return false;
        UUID stable = PersistentPlayerIdentity.migrate(player, com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER));
        if (stable == null) return false;
        if (!stable.equals(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER))) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(tag, OWNER, stable);
            tag.putString(OWNER_NAME, player.getGameProfile().name());
            write(stack, tag);
        }
        return true;
    }
}
