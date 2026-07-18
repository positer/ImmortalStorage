package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Loader-safe identity and binding behavior for the Refined Storage exchange disk.
 *
 * <p>This class intentionally has no Refined Storage signature. When RS 2.0.9 is
 * present, the registry factory creates the isolated subclass that implements
 * RS's {@code StorageContainerItem}; otherwise the registered fallback remains
 * unobtainable because its recipe and creative entry are condition-gated.</p>
 */
public class XianqiaoRsExchangeDiskItem extends Item {
    private static final String OWNER_TAG = "cultivationRsOwner";
    private static final String DISK_TAG = "cultivationRsExchangeDisk";
    private static final String OWNER_NAME_TAG = "cultivationRsOwnerName";
    private static final int MAX_OWNER_NAME_LENGTH = 64;

    public XianqiaoRsExchangeDiskItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    public static boolean bindUnbound(ItemStack stack, UUID owner, UUID diskId) {
        return bindUnbound(stack, owner, diskId, null);
    }

    /** Atomically installs UUID mount identity plus optional display metadata. */
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
        } else if (ownerFieldPresent
                && tag.hasUUID(OWNER_TAG)
                && owner.equals(tag.getUUID(OWNER_TAG))
                && !diskFieldPresent) {
            // Repair the only supported historical partial form atomically:
            // a valid owner for this player with no disk field at all.
            tag.putUUID(DISK_TAG, diskId);
            putOwnerName(tag, ownerName);
        } else {
            // Reject disk-only, malformed, foreign-owner and already-bound
            // stacks without modifying their data.
            return false;
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static Optional<UUID> owner(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.hasUUID(OWNER_TAG) ? Optional.of(tag.getUUID(OWNER_TAG)) : Optional.empty();
    }

    public static Optional<UUID> diskId(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return tag.hasUUID(DISK_TAG) ? Optional.of(tag.getUUID(DISK_TAG)) : Optional.empty();
    }

    /** Human-readable metadata only; RS mounting and deduplication use UUIDs. */
    public static Optional<String> ownerName(ItemStack stack) {
        return normalizeOwnerName(customTag(stack).getString(OWNER_NAME_TAG));
    }

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
        return XianqiaoRsExchangeDiskItem.owner(stack).filter(owner::equals).isPresent()
                && XianqiaoRsExchangeDiskItem.diskId(stack).filter(diskId::equals).isPresent();
    }

    public static boolean isBoundToOwner(ItemStack stack, UUID owner) {
        if (owner == null) return false;
        return XianqiaoRsExchangeDiskItem.owner(stack).filter(owner::equals).isPresent()
                && XianqiaoRsExchangeDiskItem.diskId(stack).isPresent();
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide && CultivationPlayerData.get(player).getStage() >= 6) {
            bindUnbound(stack, player.getUUID(), UUID.randomUUID(), playerName(player));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (CultivationPlayerData.get(player).getStage() < 6) {
            player.displayClientMessage(Component.translatable(
                    "message.cultivation.xianqiao_rs_exchange_disk.requires_xianqiao"), true);
            return InteractionResultHolder.fail(stack);
        }
        bindUnbound(stack, player.getUUID(), UUID.randomUUID(), playerName(player));
        if (!isBoundToOwner(stack, player.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.cultivation.xianqiao_rs_exchange_disk.wrong_owner"), true);
            return InteractionResultHolder.fail(stack);
        }
        refreshOwnerName(stack, player.getUUID(), playerName(player));
        player.displayClientMessage(Component.translatable(
                "message.cultivation.xianqiao_rs_exchange_disk.bound"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Optional<UUID> owner = owner(stack);
        Optional<UUID> disk = diskId(stack);
        if (owner.isEmpty() || disk.isEmpty()) {
            lines.add(Component.translatable(
                    "tooltip.cultivation.xianqiao_rs_exchange_disk.unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Object displayOwner = ownerName(stack).<Object>map(name -> name).orElse(owner.get());
        lines.add(Component.translatable(
                "tooltip.cultivation.xianqiao_rs_exchange_disk.owner", displayOwner)
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(
                "tooltip.cultivation.xianqiao_rs_exchange_disk.disk", disk.get())
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
}
