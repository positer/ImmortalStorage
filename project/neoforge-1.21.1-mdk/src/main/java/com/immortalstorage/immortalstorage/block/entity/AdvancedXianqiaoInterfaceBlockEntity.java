package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Advanced owner-bound Xianqiao Interface. Keeps the plain interface's
 * six-face PULL/PUSH/DISABLED modes and the active pull/push toggles, but runs
 * them against every container inside a configurable bounding box instead of
 * only the adjacent block: PULL extracts all interactive content
 * (items/fluids/power/chemicals) from each in-area container's configured face,
 * PUSH exports the interface cache slots that allow that face. The
 * advanced-stabilized-ruin polling options (xyz/+xzy range, frequency, preview,
 * enabled, access/split/order) remain configurable.
 */
public final class AdvancedXianqiaoInterfaceBlockEntity
        extends XianqiaoInterfaceBlockEntity implements MenuProvider {

    public static final int ACCESS_POLL_SKIP = 0, ACCESS_FORCE_POLL = 1;
    public static final int SPLIT_ITEM_BY_ITEM = 0, SPLIT_GROUP_BY_GROUP = 1;
    public static final int ORDER_FAR_FIRST = 0, ORDER_NEAR_FIRST = 1;
    /** sizeX/Y/Z, offsetX/Y/Z, frequency, preview, enabled, access, split, order. */
    public static final int RUIN_DATA_COUNT = 12;

    private static final String SIZE_X_TAG = "RuinSizeX";
    private static final String SIZE_Y_TAG = "RuinSizeY";
    private static final String SIZE_Z_TAG = "RuinSizeZ";
    private static final String OFFSET_X_TAG = "RuinOffsetX";
    private static final String OFFSET_Y_TAG = "RuinOffsetY";
    private static final String OFFSET_Z_TAG = "RuinOffsetZ";
    private static final String FREQUENCY_TAG = "RuinFrequency";
    private static final String PREVIEW_TAG = "RuinPreview";
    private static final String ENABLED_TAG = "RuinEnabled";
    private static final String ACCESS_MODE_TAG = "RuinAccessMode";
    private static final String SPLIT_MODE_TAG = "RuinSplitMode";
    private static final String ORDER_MODE_TAG = "RuinOrderMode";

    private int sizeX = 1, sizeY = 1, sizeZ = 1;
    private int offsetX, offsetY, offsetZ;
    private int frequency = 20;
    private boolean preview;
    private boolean enabled;
    private int accessMode = ACCESS_POLL_SKIP;
    private int splitMode = SPLIT_ITEM_BY_ITEM;
    private int orderMode = ORDER_NEAR_FIRST;
    private final int[] groupCursor = {0};

    public AdvancedXianqiaoInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get(), pos, state);
    }

    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public int offsetZ() { return offsetZ; }
    public int frequency() { return frequency; }
    public boolean previewEnabled() { return preview; }
    public boolean schedulingEnabled() { return enabled; }
    public int accessMode() { return accessMode; }
    public int splitMode() { return splitMode; }
    public int orderMode() { return orderMode; }
    public int[] groupCursor() { return groupCursor; }

    @Override
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        getInventory().replenishAllSlots(TerminalStorageAction.EXECUTE);
        if (!enabled || frequency <= 0 || serverLevel.getGameTime() % frequency != 0) return;
        AdvancedXianqiaoInterfaceScheduler.tick(this, serverLevel);
        XianqiaoInterfaceCompatHooks.serverTick(this, serverLevel);
    }

    /** Ruin-style ContainerData for the scheduling options (indices 0..11). */
    public ContainerData ruinMenuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                    case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                    case 6 -> frequency; case 7 -> preview ? 1 : 0;
                    case 8 -> enabled ? 1 : 0; case 9 -> accessMode;
                    case 10 -> splitMode; case 11 -> orderMode;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> sizeX = clamp(value, 1, 13);
                    case 1 -> sizeY = clamp(value, 1, 13);
                    case 2 -> sizeZ = clamp(value, 1, 13);
                    case 3 -> offsetX = clamp(value, -13, 13);
                    case 4 -> offsetY = clamp(value, -13, 13);
                    case 5 -> offsetZ = clamp(value, -13, 13);
                    case 6 -> frequency = clamp(value, 1, 72_000);
                    case 7 -> preview = value != 0;
                    case 8 -> enabled = value != 0;
                    case 9 -> accessMode = value == 0 ? ACCESS_POLL_SKIP : ACCESS_FORCE_POLL;
                    case 10 -> splitMode = value == 0 ? SPLIT_ITEM_BY_ITEM : SPLIT_GROUP_BY_GROUP;
                    case 11 -> orderMode = value == 0 ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
                    default -> { return; }
                }
                setChangedAndSync();
            }
            @Override public int getCount() { return RUIN_DATA_COUNT; }
        };
    }

    public void setMenuValue(int index, int value) {
        ruinMenuData().set(index, value);
    }

    /**
     * Re-syncs the block entity to the client whenever a face mode changes so
     * the preview highlights (green for PULL, red for PUSH) update live.
     */
    @Override
    public void setSideMode(@org.jetbrains.annotations.Nullable Direction side,
                            @org.jetbrains.annotations.Nullable SideMode mode) {
        super.setSideMode(side, mode);
        setChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.advanced_xianqiao_interface");
    }

    @Override
    public @org.jetbrains.annotations.Nullable AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        if (!canUse(player)) return null;
        return new com.immortalstorage.immortalstorage.menu.custom.AdvancedXianqiaoInterfaceMenu(
                id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(SIZE_X_TAG, sizeX); tag.putInt(SIZE_Y_TAG, sizeY); tag.putInt(SIZE_Z_TAG, sizeZ);
        tag.putInt(OFFSET_X_TAG, offsetX); tag.putInt(OFFSET_Y_TAG, offsetY); tag.putInt(OFFSET_Z_TAG, offsetZ);
        tag.putInt(FREQUENCY_TAG, frequency);
        tag.putBoolean(PREVIEW_TAG, preview);
        tag.putBoolean(ENABLED_TAG, enabled);
        tag.putInt(ACCESS_MODE_TAG, accessMode);
        tag.putInt(SPLIT_MODE_TAG, splitMode);
        tag.putInt(ORDER_MODE_TAG, orderMode);
    }

    /**
     * Targeted block-entity sync so the client renderer sees preview/size/offset
     * changes the moment the configuration page toggles them (the plain
     * interface base has no update packet; without this the range box would
     * never appear).
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sizeX = clamp(tag.getInt(SIZE_X_TAG), 1, 13);
        sizeY = clamp(tag.getInt(SIZE_Y_TAG), 1, 13);
        sizeZ = clamp(tag.getInt(SIZE_Z_TAG), 1, 13);
        offsetX = clamp(tag.getInt(OFFSET_X_TAG), -13, 13);
        offsetY = clamp(tag.getInt(OFFSET_Y_TAG), -13, 13);
        offsetZ = clamp(tag.getInt(OFFSET_Z_TAG), -13, 13);
        frequency = clamp(tag.getInt(FREQUENCY_TAG), 1, 72_000);
        preview = tag.getBoolean(PREVIEW_TAG);
        enabled = tag.getBoolean(ENABLED_TAG);
        accessMode = tag.getInt(ACCESS_MODE_TAG) == ACCESS_FORCE_POLL ? ACCESS_FORCE_POLL : ACCESS_POLL_SKIP;
        splitMode = tag.getInt(SPLIT_MODE_TAG) == SPLIT_GROUP_BY_GROUP ? SPLIT_GROUP_BY_GROUP : SPLIT_ITEM_BY_ITEM;
        orderMode = tag.getInt(ORDER_MODE_TAG) == ORDER_FAR_FIRST ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
