package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
public final class SubstitutePuppetItem extends Item {
    public static final int MAX_DURABILITY = 16;
    private static final String OWNER = "Owner";
    private static final String OWNER_NAME = "OwnerName";
    private static final String ANCHOR_DIMENSION = "AnchorDimension";
    private static final String ANCHOR_POS = "AnchorPos";

    public SubstitutePuppetItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack puppet = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(puppet);
        CompoundTag tag = data(puppet);
        UUID stableOwner = PersistentPlayerIdentity.id(serverPlayer);
        if (!tag.hasUUID(OWNER)) {
            tag.putUUID(OWNER, stableOwner);
            tag.putString(OWNER_NAME, player.getGameProfile().getName());
            write(puppet, tag);
            player.displayClientMessage(Component.translatable("message.immortalstorage.substitute_puppet.bound"), true);
            return InteractionResultHolder.consume(puppet);
        }
        if (!migrateOwner(serverPlayer, puppet, tag)) return InteractionResultHolder.fail(puppet);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.is(ModItems.NURTURING_CRYSTAL.get())) {
            int missing = puppet.getDamageValue();
            int repairs = Math.min(missing, other.getCount() / 8);
            if (repairs > 0) {
                other.shrink(repairs * 8);
                puppet.setDamageValue(missing - repairs);
                player.displayClientMessage(Component.translatable(
                        "message.immortalstorage.substitute_puppet.repaired", repairs), true);
                return InteractionResultHolder.consume(puppet);
            }
        }
        tag.remove(ANCHOR_DIMENSION);
        tag.remove(ANCHOR_POS);
        write(puppet, tag);
        player.displayClientMessage(Component.translatable("message.immortalstorage.substitute_puppet.anchor_cleared"), true);
        return InteractionResultHolder.consume(puppet);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        ItemStack puppet = context.getItemInHand();
        CompoundTag tag = data(puppet);
        if (!migrateOwner(player, puppet, tag)) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) return InteractionResult.PASS;
        tag.putString(ANCHOR_DIMENSION, context.getLevel().dimension().location().toString());
        tag.putLong(ANCHOR_POS, pos.asLong());
        write(puppet, tag);
        player.displayClientMessage(Component.translatable("message.immortalstorage.substitute_puppet.anchor_bound"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.durability",
                MAX_DURABILITY - stack.getDamageValue(), MAX_DURABILITY));
        tooltip.add(tag.hasUUID(OWNER)
                ? Component.translatable("tooltip.immortalstorage.substitute_puppet.owner",
                tag.contains(OWNER_NAME) ? tag.getString(OWNER_NAME) : tag.getUUID(OWNER).toString())
                : Component.translatable("tooltip.immortalstorage.substitute_puppet.owner_unbound"));
        if (tag.contains(ANCHOR_DIMENSION) && tag.contains(ANCHOR_POS)) {
            BlockPos pos = BlockPos.of(tag.getLong(ANCHOR_POS));
            tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.anchor",
                    tag.getString(ANCHOR_DIMENSION), pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.immortalstorage.substitute_puppet.anchor_none"));
        }
    }

    public static boolean isOwnedBy(ItemStack stack, UUID owner) {
        CompoundTag tag = data(stack);
        return stack.is(ModItems.SUBSTITUTE_PUPPET.get()) && tag.hasUUID(OWNER) && tag.getUUID(OWNER).equals(owner);
    }

    public static java.util.Optional<UUID> owner(ItemStack stack) {
        CompoundTag tag = data(stack);
        return stack != null && stack.is(ModItems.SUBSTITUTE_PUPPET.get()) && tag.hasUUID(OWNER)
                ? java.util.Optional.of(tag.getUUID(OWNER)) : java.util.Optional.empty();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        CompoundTag tag = data(stack);
        if (!migrateOwner(player, stack, tag)) return;
        String currentName = player.getGameProfile().getName();
        if (!currentName.equals(tag.getString(OWNER_NAME))) {
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
        if (!dimension.location().toString().equals(tag.getString(ANCHOR_DIMENSION))
                || tag.getLong(ANCHOR_POS) != pos.asLong()) return false;
        clearAnchor(stack, tag);
        return true;
    }

    public static boolean clearInvalidAnchor(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = data(stack);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(ANCHOR_DIMENSION));
        if (dimensionId == null || !tag.contains(ANCHOR_POS)) return false;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        var targetLevel = player.server.getLevel(key);
        if (targetLevel == null) {
            targetLevel = com.immortalstorage.immortalstorage.dimension.RealmHelper
                    .resolveOwnedPersonalRealm(player, key);
        }
        if (targetLevel == null || targetLevel.getBlockState(BlockPos.of(tag.getLong(ANCHOR_POS))).is(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        clearAnchor(stack, tag);
        return true;
    }

    public static boolean teleportToAnchor(ServerPlayer player, ItemStack stack) {
        if (clearInvalidAnchor(player, stack)) return false;
        CompoundTag tag = data(stack);
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(ANCHOR_DIMENSION));
        if (dimensionId == null || !tag.contains(ANCHOR_POS)) return false;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        var targetLevel = player.server.getLevel(key);
        if (targetLevel == null) return false;
        BlockPos pos = BlockPos.of(tag.getLong(ANCHOR_POS));
        player.teleportTo(targetLevel, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
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
        if (!tag.hasUUID(OWNER)) return false;
        UUID stable = PersistentPlayerIdentity.migrate(player, tag.getUUID(OWNER));
        if (stable == null) return false;
        if (!stable.equals(tag.getUUID(OWNER))) {
            tag.putUUID(OWNER, stable);
            tag.putString(OWNER_NAME, player.getGameProfile().getName());
            write(stack, tag);
        }
        return true;
    }
}
