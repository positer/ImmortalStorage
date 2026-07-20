package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.compat.SpiritStaffWrenchCompat;
import com.immortalstorage.immortalstorage.enchantment.ModEnchantments;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Four-mode server-authoritative Spirit Staff. */
public class SpiritStaffItem extends Item {
    public static final int MODE_EXPLORE = 0;
    public static final int MODE_WRENCH = 1;
    public static final int MODE_PICK = 2;
    public static final int MODE_BUILD = 3;
    public static final int MODE_COUNT = 4;

    public SpiritStaffItem(Item.Properties props) {
        super(props.durability(3000));
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
        return new ItemStack(Items.NETHERITE_PICKAXE).supportsEnchantment(enchantment);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide) ModEnchantments.applySpiritRepair(stack, level.registryAccess());
    }

    public static int getMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int stored = data.copyTag().getInt("mode");
        return ((stored % MODE_COUNT) + MODE_COUNT) % MODE_COUNT;
    }

    public static void setMode(ItemStack stack, int mode) {
        net.minecraft.nbt.CompoundTag tag = stack
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("mode", ((mode % MODE_COUNT) + MODE_COUNT) % MODE_COUNT);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isExploreEnabled(ItemStack stack) {
        if (stack == null || stack.isEmpty() || getMode(stack) != MODE_EXPLORE) return false;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean("exploreEnabled");
    }

    public static void setExploreEnabled(ItemStack stack, boolean enabled) {
        net.minecraft.nbt.CompoundTag tag = stack
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("exploreEnabled", enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Component modeName(int mode) {
        return switch (mode) {
            case MODE_EXPLORE -> Component.translatable("item.immortalstorage.spirit_staff.mode.explore");
            case MODE_WRENCH -> Component.translatable("item.immortalstorage.spirit_staff.mode.wrench");
            case MODE_PICK -> Component.translatable("item.immortalstorage.spirit_staff.mode.pick");
            case MODE_BUILD -> Component.translatable("item.immortalstorage.spirit_staff.mode.build");
            default -> Component.literal("?");
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getMode(stack) != MODE_EXPLORE) return InteractionResultHolder.pass(stack);
        if (player.pick(player.blockInteractionRange(), 0.0F, false).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            boolean enabled = !isExploreEnabled(stack);
            setExploreEnabled(stack, enabled);
            player.displayClientMessage(Component.translatable(enabled
                    ? "message.immortalstorage.spirit_staff.explore.enabled"
                    : "message.immortalstorage.spirit_staff.explore.disabled"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(context.getLevel() instanceof ServerLevel serverLevel)
                || player.isSpectator()
                || player.distanceToSqr(context.getClickedPos().getCenter()) > 64.0D) {
            return InteractionResult.CONSUME;
        }

        return switch (getMode(context.getItemInHand())) {
            case MODE_EXPLORE -> InteractionResult.PASS;
            case MODE_WRENCH -> wrench(context, serverPlayer);
            case MODE_PICK -> preciseHarvest(context, serverPlayer);
            case MODE_BUILD -> build(context, serverPlayer);
            default -> InteractionResult.PASS;
        };
    }

    public static void transferOpenedLootMenu(ServerPlayer player, AbstractContainerMenu menu) {
        ItemStack held = enabledExploreInstrument(player);
        if (held.isEmpty() || !isLootPageMenu(menu, player)) return;
        PersonalStorageEndpoint endpoint = PersonalStorageApi.resolve(
                player.server, player.getUUID());
        if (endpoint == null) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.storage_unavailable"), true);
            return;
        }
        int inserted = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory() || !slot.hasItem() || !slot.mayPickup(player)) continue;
            OpenedLootSlotTransfer.Result moved = OpenedLootSlotTransfer.move(
                    new OpenedLootSlotSource(slot), endpoint::insert);
            inserted += moved.committed();
            slot.setChanged();
        }
        if (inserted <= 0) return;
        menu.broadcastChanges();
        held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(
                player.getMainHandItem() == held ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
        player.getCooldowns().addCooldown(held.getItem(), 4);
        player.displayClientMessage(Component.translatable(
                "message.immortalstorage.spirit_staff.explore.transferred", inserted), true);
    }

    private static ItemStack enabledExploreInstrument(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SpiritStaffItem && isExploreEnabled(main)) return main;
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() instanceof SpiritStaffItem && isExploreEnabled(offhand)
                ? offhand : ItemStack.EMPTY;
    }

    static boolean isLootPageMenu(AbstractContainerMenu menu, Player player) {
        if (menu instanceof ChestMenu) return true;
        if (menu == null || player == null) return false;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) continue;
            String containerType = slot.container.getClass().getName()
                    .toLowerCase(java.util.Locale.ROOT);
            if (containerType.startsWith("noobanidus.mods.lootr.")) return true;
        }
        return false;
    }

    private static void restoreOpenedSlot(Slot slot, ItemStack stack) {
        ItemStack current = slot.getItem();
        if (current.isEmpty()) {
            slot.set(stack);
            return;
        }
        if (!ItemStack.isSameItemSameComponents(current, stack)
                || (long) current.getCount() + stack.getCount() > slot.getMaxStackSize(stack)) {
            throw new IllegalStateException("Opened loot page changed during an atomic transfer");
        }
        current.grow(stack.getCount());
        slot.setChanged();
    }

    private record OpenedLootSlotSource(Slot slot) implements OpenedLootSlotTransfer.Source {
        @Override public ItemStack peek() { return slot.getItem().copy(); }
        @Override public ItemStack extract(int amount) { return slot.remove(amount); }
        @Override public void restore(ItemStack stack) { restoreOpenedSlot(slot, stack); }
    }

    private static InteractionResult wrench(UseOnContext context, ServerPlayer player) {
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (player.isShiftKeyDown() && isSafeWrenchTarget(blockEntity)) {
            return safelyDismantle(context, blockEntity, player);
        }
        // AE2, RS and Create-style blocks consume the c:tools/wrench tag before
        // Item#useOn. Mekanism's public IConfigurable API is dispatched here by
        // an optional module that is loaded only when Mekanism is present.
        InteractionResult result = SpiritStaffWrenchCompat.interact(context, player);
        if (result.consumesAction()) {
            context.getItemInHand().hurtAndBreak(
                    1, player, LivingEntity.getSlotForHand(context.getHand()));
            player.getCooldowns().addCooldown(context.getItemInHand().getItem(), 4);
        }
        return result;
    }

    private static InteractionResult preciseHarvest(UseOnContext context, ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.pick.stage"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos pos = context.getClickedPos();
        if (!player.canInteractWithBlock(pos, 1.0D)
                || !context.getLevel().mayInteract(player, pos)
                || context.getLevel().getBlockState(pos).isAir()) {
            return InteractionResult.CONSUME;
        }
        if (!data.consumeImmortalYuan(1L)) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.pick.no_yuan"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack staff = context.getItemInHand();
        ItemEnchantments original = staff.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        Holder<Enchantment> silkTouch = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        boolean destroyed;
        try {
            ItemEnchantments.Mutable skillEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            skillEnchantments.set(silkTouch, 1);
            staff.set(DataComponents.ENCHANTMENTS, skillEnchantments.toImmutable());
            destroyed = player.gameMode.destroyBlock(pos);
        } finally {
            if (!staff.isEmpty()) staff.set(DataComponents.ENCHANTMENTS, original);
        }
        if (!destroyed) {
            data.depositImmortalYuan(1L);
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.pick.denied"), true);
            return InteractionResult.CONSUME;
        }
        player.getCooldowns().addCooldown(staff.getItem(), 4);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult build(UseOnContext context, ServerPlayer player) {
        SpiritStaffBuildExecutor.Result result = SpiritStaffBuildExecutor.execute(
                context, ImmortalStorageConfig.SPIRIT_STAFF_BUILD_LIMIT.get());
        if (result.succeeded()) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.build.placed", result.placed()), true);
            return InteractionResult.CONSUME;
        }
        String key = switch (result.failure()) {
            case NOT_A_BLOCK_ITEM -> "message.immortalstorage.spirit_staff.build.not_block";
            case NO_MATERIALS -> "message.immortalstorage.spirit_staff.build.no_materials";
            case NO_TARGETS, BLOCKED -> "message.immortalstorage.spirit_staff.build.blocked";
            default -> "message.immortalstorage.spirit_staff.build.failed";
        };
        player.displayClientMessage(Component.translatable(key), true);
        return InteractionResult.CONSUME;
    }

    private static boolean isSafeWrenchTarget(BlockEntity blockEntity) {
        return blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity
                || blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerBlockEntity
                || blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity
                || blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
    }

    private static boolean isSafeWrenchState(BlockState state) {
        return state.getBlock() instanceof com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock
                || state.getBlock() instanceof com.immortalstorage.immortalstorage.block.custom.SourceVeinManagerBlock
                || state.getBlock() instanceof com.immortalstorage.immortalstorage.block.custom.XianqiaoInterfaceBlock
                || state.getBlock() instanceof com.immortalstorage.immortalstorage.block.custom.XianqiaoManagerBlock;
    }

    private static InteractionResult safelyDismantle(
            UseOnContext context, BlockEntity blockEntity, ServerPlayer player) {
        if (!ownsDismantleTarget(blockEntity, player)) {
            player.displayClientMessage(Component.translatable(
                    "message.immortalstorage.spirit_staff.dismantle.denied"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos pos = context.getClickedPos();
        if (!player.canInteractWithBlock(pos, 1.0D)
                || !context.getLevel().mayInteract(player, pos)) {
            return InteractionResult.CONSUME;
        }
        return player.gameMode.destroyBlock(pos)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    private static boolean ownsDismantleTarget(BlockEntity blockEntity, Player player) {
        if (player.hasPermissions(2)) return true;
        if (blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity source) {
            return source.isOwnedBy(player.getUUID());
        }
        if (blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerBlockEntity manager) {
            return player.getUUID().equals(manager.getOwner());
        }
        if (blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity xianqiaoInterface) {
            return player.getUUID().equals(xianqiaoInterface.getOwner());
        }
        if (blockEntity instanceof com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity manager) {
            return player.getUUID().equals(manager.getOwner());
        }
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getMode(stack) == MODE_PICK) {
            return Items.NETHERITE_PICKAXE.getDestroySpeed(new ItemStack(Items.NETHERITE_PICKAXE), state);
        }
        if (getMode(stack) == MODE_WRENCH && isSafeWrenchState(state)) return 8.0F;
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (getMode(stack) == MODE_PICK) {
            return Items.NETHERITE_PICKAXE.isCorrectToolForDrops(
                    new ItemStack(Items.NETHERITE_PICKAXE), state);
        }
        return getMode(stack) == MODE_WRENCH && isSafeWrenchState(state);
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        int mode = getMode(stack);
        if (!level.isClientSide && (mode == MODE_PICK
                || mode == MODE_WRENCH && isSafeWrenchState(state))) {
            stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (selected || player.getOffhandItem() == stack) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false, true));
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "item.immortalstorage.spirit_staff.mode_tooltip", modeName(getMode(stack))));
        if (getMode(stack) == MODE_EXPLORE) {
            tooltip.add(Component.translatable(isExploreEnabled(stack)
                    ? "item.immortalstorage.spirit_staff.explore_on"
                    : "item.immortalstorage.spirit_staff.explore_off"));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isExploreEnabled(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    public static void cycleMode(Player player, int delta) {
        if (!(player.getMainHandItem().getItem() instanceof SpiritStaffItem)) return;
        ItemStack stack = player.getMainHandItem();
        setMode(stack, getMode(stack) + (delta >= 0 ? 1 : -1));
        player.displayClientMessage(Component.translatable(
                "item.immortalstorage.spirit_staff.mode_switch", modeName(getMode(stack))), true);
    }

    public static void cycleMode(Player player) {
        cycleMode(player, 1);
    }
}
