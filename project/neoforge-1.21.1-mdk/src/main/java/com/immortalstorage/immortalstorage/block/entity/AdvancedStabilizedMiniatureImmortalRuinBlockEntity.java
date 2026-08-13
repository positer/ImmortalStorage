package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * Blue-framed stabilized ruin that schedules every container in its operation
 * range instead of collecting item entities. This ruin is an independent
 * container: normal mode pulls one allowed stack from each nearby container into
 * its own inventory; reversed mode pushes its own inventory out into the
 * containers. Access logic (poll-skip / force-poll), equal split (item-by-item /
 * group-by-group) and access order (far-first / near-first by Manhattan
 * distance) are configurable. No item is ever dropped into the world; leftovers
 * stay in its own inventory for the next operation.
 */
public final class AdvancedStabilizedMiniatureImmortalRuinBlockEntity
        extends StabilizedMiniatureImmortalRuinBlockEntity implements MenuProvider {

    public static final int ACCESS_POLL_SKIP = 0, ACCESS_FORCE_POLL = 1;
    public static final int SPLIT_ITEM_BY_ITEM = 0, SPLIT_GROUP_BY_GROUP = 1;
    public static final int ORDER_FAR_FIRST = 0, ORDER_NEAR_FIRST = 1;

    private int accessMode = ACCESS_POLL_SKIP;
    private int splitMode = SPLIT_ITEM_BY_ITEM;
    private int orderMode = ORDER_NEAR_FIRST;
    private final int[] groupCursor = {0};

    public AdvancedStabilizedMiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    public int accessMode() { return accessMode; }
    public int splitMode() { return splitMode; }
    public int orderMode() { return orderMode; }

    @Override
    public void toggleReversed() {
        reversed = !reversed;
        setChangedAndSync();
    }

    @Override
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || !enabled || frequency <= 0
                || serverLevel.getGameTime() % frequency != 0) return;
        List<AdvancedRuinScheduler.Target> targets = AdvancedRuinScheduler.scan(
                serverLevel, worldPosition, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ,
                orderMode == ORDER_FAR_FIRST, faceMask());
        boolean forcePoll = accessMode == ACCESS_FORCE_POLL;
        boolean itemByItem = splitMode == SPLIT_ITEM_BY_ITEM;
        if (reversed) {
            for (int group = 0; group < reinforcementMultiplier(); group++)
                if (!AdvancedRuinScheduler.eject(itemHandler(), targets, forcePoll, itemByItem,
                        this::allows, groupCursor)) break;
        } else {
            AdvancedRuinScheduler.collect(itemHandler(), targets, forcePoll, itemByItem,
                    this::allows);
        }
    }

    @Override
    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                    case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                    case 6 -> frequency; case 7 -> preview ? 1 : 0; case 8 -> enabled ? 1 : 0;
                    case 9 -> reversed ? 1 : 0;
                    case 10 -> accessMode; case 11 -> splitMode; case 12 -> orderMode;
                    case 13 -> faceMask();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> sizeX = clamp(value, 1, 13); case 1 -> sizeY = clamp(value, 1, 13); case 2 -> sizeZ = clamp(value, 1, 13);
                    case 3 -> offsetX = clamp(value, -13, 13); case 4 -> offsetY = clamp(value, -13, 13); case 5 -> offsetZ = clamp(value, -13, 13);
                    case 6 -> frequency = clamp(value, 1, 72_000); case 7 -> preview = value != 0; case 8 -> enabled = value != 0;
                    case 10 -> accessMode = value == 0 ? ACCESS_POLL_SKIP : ACCESS_FORCE_POLL;
                    case 11 -> splitMode = value == 0 ? SPLIT_ITEM_BY_ITEM : SPLIT_GROUP_BY_GROUP;
                    case 12 -> orderMode = value == 0 ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
                    case 13 -> { faceMask = value & AdvancedRuinScheduler.ALL_FACES; setChangedAndSync(); }
                    default -> { }
                }
                setChangedAndSync();
            }
            @Override public int getCount() { return 14; }
        };
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.advanced_stabilized_miniature_immortal_ruin");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.immortalstorage.immortalstorage.menu.custom.AdvancedStabilizedMiniatureImmortalRuinMenu(
                id, inventory, this, menuData());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AccessMode", accessMode);
        tag.putInt("SplitMode", splitMode);
        tag.putInt("OrderMode", orderMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Reversed mode keeps an adjustable range; restore sizes the superclass forced to 1.
        sizeX = clamp(tag.getInt("SizeX"), 1, 13);
        sizeY = clamp(tag.getInt("SizeY"), 1, 13);
        sizeZ = clamp(tag.getInt("SizeZ"), 1, 13);
        accessMode = tag.getInt("AccessMode") == ACCESS_FORCE_POLL ? ACCESS_FORCE_POLL : ACCESS_POLL_SKIP;
        splitMode = tag.getInt("SplitMode") == SPLIT_GROUP_BY_GROUP ? SPLIT_GROUP_BY_GROUP : SPLIT_ITEM_BY_ITEM;
        orderMode = tag.getInt("OrderMode") == ORDER_FAR_FIRST ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
    }
}
