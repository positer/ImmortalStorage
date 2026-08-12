package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owner-bound reusable fuel credential for a placed Immortal Furnace. */
public class SpiritDriveItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    private static final String OWNER_TAG = "immortalstorageOwner";
    private static final String OWNER_NAME_TAG = "immortalstorageOwnerName";
    private static final int MAX_OWNER_NAME_LENGTH = 64;
    private static final ConcurrentHashMap<String, Long> VANILLA_RETRY_TICKS = new ConcurrentHashMap<>();

    public SpiritDriveItem(Item.Properties props) {
        super(props.stacksTo(1));
    }

    public static Optional<UUID> owner(ItemStack stack) {
        CompoundTag tag = customTag(stack);
        return com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER_TAG) ? Optional.of(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER_TAG)) : Optional.empty();
    }

    /** Human-readable metadata only; furnace payment authority always uses the UUID. */
    public static Optional<String> ownerName(ItemStack stack) {
        return normalizeOwnerName(customTag(stack).getStringOr(OWNER_NAME_TAG, ""));
    }

    /**
     * Binds an unowned drive or refreshes display metadata for the same owner.
     * A foreign owner UUID is immutable and cannot be replaced by item NBT.
     */
    public static boolean bind(ItemStack stack, UUID owner, String ownerName) {
        if (stack == null || stack.isEmpty() || owner == null) return false;
        CompoundTag tag = customTag(stack);
        if (com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER_TAG) && !owner.equals(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER_TAG))) return false;
        com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(tag, OWNER_TAG, owner);
        normalizeOwnerName(ownerName).ifPresent(name -> tag.putString(OWNER_NAME_TAG, name));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static boolean isBoundTo(ItemStack stack, UUID owner) {
        return owner != null && SpiritDriveItem.owner(stack).filter(owner::equals).isPresent();
    }

    /** Non-zero discovery value lets standard fuel slots accept the credential. */
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return owner(stack).isPresent() ? 1 : 0;
    }

    @Override public boolean hasCraftingRemainingItem(ItemStack stack) {
        return owner(stack).isPresent();
    }

    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return owner(stack).isPresent() ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    /** Pays one reusable vanilla-furnace ignition only after vanilla confirms a valid recipe. */
    public static int payVanillaFurnaceFuel(ServerLevel level, BlockPos furnacePos, ItemStack drive) {
        UUID ownerId = owner(drive).orElse(null);
        if (level == null || furnacePos == null || ownerId == null) return 0;
        String retryKey = level.dimension().identifier() + ":" + furnacePos.asLong();
        long now = level.getGameTime();
        long nextRetry = VANILLA_RETRY_TICKS.getOrDefault(retryKey, Long.MIN_VALUE);
        if (now < nextRetry) return 0;

        ServerPlayer owner = PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), ownerId);
        ImmortalStoragePlayerData data = owner == null ? null : ImmortalStoragePlayerData.get(owner);
        if (data != null && !data.extractStack(new ItemStack(ModItems.IMMORTAL_YUAN.get()), 1).isEmpty()) {
            VANILLA_RETRY_TICKS.remove(retryKey);
            return ImmortalYuanItem.VANILLA_BURN_TICKS;
        }
        if (data != null && !data.extractStack(new ItemStack(ModItems.TRUE_YUAN.get()), 1).isEmpty()) {
            VANILLA_RETRY_TICKS.remove(retryKey);
            return TrueYuanItem.VANILLA_BURN_TICKS;
        }
        VANILLA_RETRY_TICKS.put(retryKey, now + 5L);
        return 0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        UUID stableOwner = PersistentPlayerIdentity.id(player);
        UUID storedOwner = owner(stack).orElse(null);
        if (storedOwner != null && PersistentPlayerIdentity.matches(player, storedOwner)
                && !stableOwner.equals(storedOwner)) {
            CompoundTag migrated = customTag(stack);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(migrated, OWNER_TAG, stableOwner);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(migrated));
        }
        boolean bound = bind(stack, stableOwner, player.getGameProfile().name());
        com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable(bound
                ? "message.immortalstorage.spirit_drive.bound"
                : "message.immortalstorage.spirit_drive.wrong_owner"), true);
        return bound ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Optional<UUID> owner = owner(stack);
        if (owner.isEmpty()) {
            lines.add(Component.translatable("tooltip.immortalstorage.spirit_drive.unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Object displayOwner = ownerName(stack).<Object>map(name -> name).orElse(owner.get());
        lines.add(Component.translatable("tooltip.immortalstorage.spirit_drive.owner", displayOwner)
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.immortalstorage.spirit_drive.fuel_priority")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static CompoundTag customTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static Optional<String> normalizeOwnerName(String ownerName) {
        if (ownerName == null) return Optional.empty();
        String normalized = ownerName.strip();
        return normalized.isEmpty() || normalized.length() > MAX_OWNER_NAME_LENGTH
                ? Optional.empty()
                : Optional.of(normalized);
    }
}
