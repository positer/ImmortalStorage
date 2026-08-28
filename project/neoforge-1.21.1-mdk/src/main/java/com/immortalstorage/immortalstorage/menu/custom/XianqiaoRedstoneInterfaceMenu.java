package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoRedstoneInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;

/** One-slot Xianqiao-interface configuration semantics plus Schmitt thresholds. */
public final class XianqiaoRedstoneInterfaceMenu extends AbstractContainerMenu {
    private static final int CONFIGURATION_SYNC_MARKER = 0x58495253;
    public static final long DEFAULT_EXTERNAL_CACHE_AMOUNT = 1_000L;
    public static final int CONFIG_Y = 8;
    public static final int PLAYER_INVENTORY_Y = 65;
    public static final int HOTBAR_Y = 123;
    private static final String FLUID_DISPLAY_TAG = "ImmortalStorageInterfaceFluid";
    private static final String EXTERNAL_DISPLAY_TAG = "ImmortalStorageInterfaceExternalResource";

    private final XianqiaoRedstoneInterfaceBlockEntity blockEntity;
    private final ContainerData data;
    private final SimpleContainer mirror = new SimpleContainer(1);

    public XianqiaoRedstoneInterfaceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level().getBlockEntity(buffer.readBlockPos())
                instanceof XianqiaoRedstoneInterfaceBlockEntity found ? found : null);
    }

    public XianqiaoRedstoneInterfaceMenu(
            int id, Inventory inventory, XianqiaoRedstoneInterfaceBlockEntity blockEntity) {
        super(ModMenus.XIANQIAO_REDSTONE_INTERFACE.get(), id);
        this.blockEntity = blockEntity;
        if (blockEntity != null) mirror.setItem(0, displayTarget(blockEntity));
        addSlot(new Slot(mirror, 0, 8, CONFIG_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean isFake() { return true; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        data = blockEntity == null || blockEntity.getLevel() == null
                || blockEntity.getLevel().isClientSide ? new SimpleContainerData(11) : new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) blockEntity.lowThreshold();
                    case 1 -> (int) (blockEntity.lowThreshold() >>> 32);
                    case 2 -> (int) blockEntity.highThreshold();
                    case 3 -> (int) (blockEntity.highThreshold() >>> 32);
                    case 4 -> blockEntity.inverted() ? 1 : 0;
                    case 5 -> (int) blockEntity.getConfigRevision();
                    case 6 -> (int) (blockEntity.getConfigRevision() >>> 32);
                    case 7 -> blockEntity.getInventory().getExternalTarget(0) != null ? 2
                            : blockEntity.getInventory().getFluidTarget(0).isEmpty() ? 0 : 1;
                    case 8 -> (int) configuredAmount(blockEntity);
                    case 9 -> (int) (configuredAmount(blockEntity) >>> 32);
                    case 10 -> CONFIGURATION_SYNC_MARKER;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 11; }
        };
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && blockEntity.getLevel() != null
                && !blockEntity.getLevel().isClientSide) {
            ItemStack target = displayTarget(blockEntity);
            if (!ItemStack.matches(target, mirror.getItem(0))) mirror.setItem(0, target);
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0) {
            if (clickType != ClickType.PICKUP || blockEntity == null || !stillValid(player)) return;
            if (XianqiaoInterfaceMenu.configureTargetFromCarried(
                    blockEntity.getInventory(), 0, getCarried(), button)) {
                mirror.setItem(0, displayTarget(blockEntity));
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public long low() { return decodeLong(0, 1); }
    public long high() { return decodeLong(2, 3); }
    public boolean inverted() { return data.get(4) != 0; }
    public long configRevision() { return decodeLong(5, 6); }
    public long getConfiguredAmount() { return decodeLong(8, 9); }
    public boolean configurationSynchronized() {
        return data.get(10) == CONFIGURATION_SYNC_MARKER;
    }
    public boolean isFluidTarget() { return data.get(7) == 1; }
    public boolean isExternalTarget() { return data.get(7) == 2; }
    public ResourceChannelKey getExternalTarget() {
        if (!isExternalTarget()) return null;
        CompoundTag marker = mirror.getItem(0).getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!marker.contains(EXTERNAL_DISPLAY_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag encoded = marker.getCompound(EXTERNAL_DISPLAY_TAG);
        try {
            return new ResourceChannelKey(
                    encoded.getString("Channel"), encoded.getString("Resource"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static long configuredAmount(XianqiaoRedstoneInterfaceBlockEntity blockEntity) {
        ResourceChannelKey external = blockEntity.getInventory().getExternalTarget(0);
        if (external != null) return blockEntity.getInventory().getExternalDesiredAmount(0);
        FluidStack fluid = blockEntity.getInventory().getFluidTarget(0);
        if (!fluid.isEmpty()) return fluid.getAmount();
        return blockEntity.getInventory().getTarget(0).isEmpty() ? 0L : 1L;
    }
    public net.minecraft.core.BlockPos blockPos() {
        return blockEntity == null ? net.minecraft.core.BlockPos.ZERO : blockEntity.getBlockPos();
    }
    public List<ResourceChannelKey> availableExternalResources() {
        return ExternalResourceCatalog.available().stream()
                .filter(key -> !"mekanism_chemical".equals(key.channel()))
                .toList();
    }

    @Override public boolean canDragTo(Slot slot) { return slot != slots.get(0); }
    @Override public boolean stillValid(Player player) {
        return blockEntity == null
                || player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    private long decodeLong(int lowIndex, int highIndex) {
        return Integer.toUnsignedLong(data.get(lowIndex)) | ((long) data.get(highIndex) << 32);
    }

    private static ItemStack displayTarget(XianqiaoRedstoneInterfaceBlockEntity blockEntity) {
        var backend = blockEntity.getInventory();
        ItemStack item = backend.getTarget(0);
        if (!item.isEmpty()) return item.copyWithCount(1);
        FluidStack fluid = backend.getFluidTarget(0);
        if (!fluid.isEmpty()) {
            ItemStack display = FluidUtil.getFilledBucket(fluid);
            if (display.isEmpty()) display = new ItemStack(Items.BUCKET);
            if (blockEntity.getLevel() != null) {
                Tag encoded = fluid.copyWithAmount(1)
                        .saveOptional(blockEntity.getLevel().registryAccess());
                if (encoded instanceof CompoundTag compound) {
                    CompoundTag marker = display.getOrDefault(
                            DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    marker.put(FLUID_DISPLAY_TAG, compound.copy());
                    display.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
                }
            }
            return display.copyWithCount(1);
        }
        ResourceChannelKey external = backend.getExternalTarget(0);
        if (external == null) return ItemStack.EMPTY;
        ItemStack display = new ItemStack(
                com.immortalstorage.immortalstorage.block.ModBlocks.XIANQIAO_INTERFACE.get());
        CompoundTag marker = display.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag encoded = new CompoundTag();
        encoded.putString("Channel", external.channel());
        encoded.putString("Resource", external.resourceId());
        marker.put(EXTERNAL_DISPLAY_TAG, encoded);
        display.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        return display;
    }
}
