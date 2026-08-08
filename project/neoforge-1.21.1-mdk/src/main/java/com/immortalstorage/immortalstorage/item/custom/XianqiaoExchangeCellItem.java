package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * AE2-facing exchange-cell item without a hard AE2 class reference.
 *
 * <p>The optional integration module interprets this item's immutable owner
 * and disk identities. Keeping the base item API-only makes a ImmortalStorage
 * installation without AE2 safe to class-load.</p>
 */
public final class XianqiaoExchangeCellItem extends Item {
    private static final String OWNER_TAG = "immortalstorageOwner";
    private static final String DISK_TAG = "immortalstorageExchangeDisk";
    private static final String OWNER_NAME_TAG = "immortalstorageOwnerName";
    private static final int MAX_OWNER_NAME_LENGTH = 64;

    public XianqiaoExchangeCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean bindUnbound(ItemStack stack, UUID owner, UUID diskId) {
        return bindUnbound(stack, owner, diskId, null);
    }

    /**
     * Atomically installs the immutable mount identities on an unbound stack.
     *
     * <p>The display name is deliberately separate from the owner UUID. It can
     * be refreshed after a player rename, while AE2 mounting and per-grid
     * duplicate election continue to use the UUID only.</p>
     */
    public static boolean bindUnbound(
            ItemStack stack, UUID owner, UUID diskId, String ownerName) {
        if (stack == null || stack.isEmpty() || owner == null || diskId == null) return false;
        CompoundTag tag = customTag(stack);
        boolean ownerFieldPresent = tag.contains(OWNER_TAG);
        boolean diskFieldPresent = tag.contains(DISK_TAG);

        if (!ownerFieldPresent && !diskFieldPresent) {
            tag.putUUID(OWNER_TAG, owner);
            tag.putUUID(DISK_TAG, diskId);
            putOwnerName(tag, ownerName);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return true;
        }

        // The first exchange-cell implementation persisted only the owner.
        // Repair that historical shape atomically, but only for the same
        // player. A disk-only, malformed, or foreign stack is never claimable.
        if (tag.hasUUID(OWNER_TAG)
                && owner.equals(tag.getUUID(OWNER_TAG))
                && !diskFieldPresent) {
            tag.putUUID(DISK_TAG, diskId);
            putOwnerName(tag, ownerName);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return true;
        }
        return false;
    }

    public static Optional<UUID> owner(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.hasUUID(OWNER_TAG) ? Optional.of(tag.getUUID(OWNER_TAG)) : Optional.empty();
    }

    public static Optional<UUID> diskId(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.hasUUID(DISK_TAG) ? Optional.of(tag.getUUID(DISK_TAG)) : Optional.empty();
    }

    /** Human-readable metadata only; never use this value as a mount key. */
    public static Optional<String> ownerName(ItemStack stack) {
        return normalizeOwnerName(customTag(stack).getString(OWNER_NAME_TAG));
    }

    /**
     * Refreshes display metadata only on a complete disk owned by {@code owner}.
     * The immutable owner and disk UUID fields are never written by this path.
     */
    public static boolean refreshOwnerName(ItemStack stack, UUID owner, String ownerName) {
        if (stack == null || stack.isEmpty() || owner == null) return false;
        Optional<String> normalized = normalizeOwnerName(ownerName);
        if (normalized.isEmpty()) return false;
        CompoundTag tag = customTag(stack);
        if (!tag.hasUUID(OWNER_TAG)
                || !owner.equals(tag.getUUID(OWNER_TAG))
                || !tag.hasUUID(DISK_TAG)) {
            return false;
        }
        if (normalized.get().equals(ownerName(stack).orElse(null))) return false;
        tag.putString(OWNER_NAME_TAG, normalized.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static boolean isBoundTo(ItemStack stack, UUID owner, UUID diskId) {
        if (owner == null || diskId == null) return false;
        return XianqiaoExchangeCellItem.owner(stack).filter(owner::equals).isPresent()
                && XianqiaoExchangeCellItem.diskId(stack).filter(diskId::equals).isPresent();
    }

    /** True only when both identities exist and the owner matches. */
    public static boolean isBoundToOwner(ItemStack stack, UUID owner) {
        if (owner == null) return false;
        return XianqiaoExchangeCellItem.owner(stack).filter(owner::equals).isPresent()
                && XianqiaoExchangeCellItem.diskId(stack).isPresent();
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (level.isClientSide) return;
        if (ImmortalStoragePlayerData.get(player).getStage() >= 6) {
            bindUnbound(stack, PersistentPlayerIdentity.id(player), UUID.randomUUID(), playerName(player));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.xianqiao_exchange_cell.requires_xianqiao"), true);
            return InteractionResultHolder.fail(stack);
        }
        UUID stableOwner = PersistentPlayerIdentity.id(player);
        migrateLegacyOwner(stack, player, stableOwner);
        bindUnbound(stack, stableOwner, UUID.randomUUID(), playerName(player));
        if (!isBoundToOwner(stack, stableOwner)) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.xianqiao_exchange_cell.wrong_owner"), true);
            return InteractionResultHolder.fail(stack);
        }
        refreshOwnerName(stack, stableOwner, playerName(player));
        player.displayClientMessage(Component.translatable(
                "message.immortalstorage.xianqiao_exchange_cell.bound"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Optional<UUID> owner = owner(stack);
        Optional<UUID> disk = diskId(stack);
        if (owner.isEmpty() || disk.isEmpty()) {
            lines.add(Component.translatable(
                    "tooltip.immortalstorage.xianqiao_exchange_cell.unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Object displayOwner = ownerName(stack).<Object>map(name -> name).orElse(owner.get());
        lines.add(Component.translatable(
                "tooltip.immortalstorage.xianqiao_exchange_cell.owner", displayOwner)
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(
                "tooltip.immortalstorage.xianqiao_exchange_cell.disk", disk.get())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static CompoundTag customTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void putOwnerName(CompoundTag tag, String ownerName) {
        normalizeOwnerName(ownerName).ifPresent(name -> tag.putString(OWNER_NAME_TAG, name));
    }

    private static Optional<String> normalizeOwnerName(String ownerName) {
        if (ownerName == null) return Optional.empty();
        String normalized = ownerName.strip();
        return normalized.isEmpty() || normalized.length() > MAX_OWNER_NAME_LENGTH
                ? Optional.empty()
                : Optional.of(normalized);
    }

    private static String playerName(Player player) {
        return player.getGameProfile().getName();
    }

    private static void migrateLegacyOwner(ItemStack stack, Player player, UUID stableOwner) {
        CompoundTag tag = customTag(stack);
        if (!tag.hasUUID(OWNER_TAG) || !tag.hasUUID(DISK_TAG)) return;
        UUID stored = tag.getUUID(OWNER_TAG);
        if (!stored.equals(stableOwner) && stored.equals(player.getUUID())) {
            tag.putUUID(OWNER_TAG, stableOwner);
            putOwnerName(tag, playerName(player));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
