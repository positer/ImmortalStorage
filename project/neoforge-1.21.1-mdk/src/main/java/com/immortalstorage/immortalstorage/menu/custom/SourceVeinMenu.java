package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Slotless source-vein menu: it synchronizes six face modes and exact per-tick throughput. */
public class SourceVeinMenu extends AbstractContainerMenu {
    private final SourceVeinBlockEntity blockEntity;
    private final DataSlot[] sideModeSlots = new DataSlot[Direction.values().length];
    private final DataSlot[] sideFaultSlots = new DataSlot[Direction.values().length];
    private final DataSlot[] sideInFlightLowSlots = new DataSlot[Direction.values().length];
    private final DataSlot[] sideInFlightHighSlots = new DataSlot[Direction.values().length];
    private final int[] clientSideFaults = new int[Direction.values().length];
    private final long[] clientSideInFlight = new long[Direction.values().length];
    private long clientFluxLimit;
    private final DataSlot fluxLimitLowSlot;
    private final DataSlot fluxLimitHighSlot;

    public SourceVeinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, inventory.player, resolveBlockEntity(inventory.player, buffer));
    }

    public SourceVeinMenu(int id, Inventory inventory, Player player, SourceVeinBlockEntity blockEntity) {
        super(ModMenus.SOURCE_VEIN.get(), id);
        this.blockEntity = blockEntity;

        for (Direction direction : Direction.values()) {
            DataSlot mode = new DataSlot() {
                @Override
                public int get() {
                    return blockEntity == null
                            ? SourceVeinBlockEntity.SourceSideMode.DISABLED.persistedId()
                            : blockEntity.getSideMode(direction).persistedId();
                }

                @Override
                public void set(int value) {
                    if (blockEntity != null
                            && (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide())) {
                        blockEntity.setSideMode(direction, SourceVeinBlockEntity.SourceSideMode.byId(value));
                    }
                }
            };
            sideModeSlots[direction.ordinal()] = mode;
            this.addDataSlot(mode);

            int index = direction.ordinal();
            DataSlot fault = new DataSlot() {
                @Override
                public int get() {
                    return serverBacked()
                            ? (blockEntity.isFaceFaulted(direction) ? 1 : 0)
                            : clientSideFaults[index];
                }

                @Override
                public void set(int value) {
                    clientSideFaults[index] = value == 0 ? 0 : 1;
                }
            };
            sideFaultSlots[index] = fault;
            this.addDataSlot(fault);

            DataSlot inFlightLow = new DataSlot() {
                @Override
                public int get() {
                    long value = serverBacked()
                            ? blockEntity.uncertainInFlight(direction)
                            : clientSideInFlight[index];
                    return (int) value;
                }

                @Override
                public void set(int value) {
                    clientSideInFlight[index] = (clientSideInFlight[index] & 0xFFFFFFFF00000000L)
                            | (value & 0xFFFFFFFFL);
                }
            };
            sideInFlightLowSlots[index] = inFlightLow;
            this.addDataSlot(inFlightLow);

            DataSlot inFlightHigh = new DataSlot() {
                @Override
                public int get() {
                    long value = serverBacked()
                            ? blockEntity.uncertainInFlight(direction)
                            : clientSideInFlight[index];
                    return (int) (value >>> 32);
                }

                @Override
                public void set(int value) {
                    clientSideInFlight[index] = ((long) value << 32)
                            | (clientSideInFlight[index] & 0xFFFFFFFFL);
                }
            };
            sideInFlightHighSlots[index] = inFlightHigh;
            this.addDataSlot(inFlightHigh);
        }
        this.fluxLimitLowSlot = new DataSlot() {
            @Override
            public int get() {
                long value = serverBacked() ? blockEntity.getFluxLimit() : clientFluxLimit;
                return (int) value;
            }

            @Override
            public void set(int value) {
                clientFluxLimit = (clientFluxLimit & 0x7FFFFFFF00000000L)
                        | (value & 0xFFFFFFFFL);
            }
        };
        this.addDataSlot(fluxLimitLowSlot);
        this.fluxLimitHighSlot = new DataSlot() {
            @Override
            public int get() {
                long value = serverBacked() ? blockEntity.getFluxLimit() : clientFluxLimit;
                return (int) (value >>> 32);
            }

            @Override
            public void set(int value) {
                clientFluxLimit = ((long) (value & 0x7FFFFFFF) << 32)
                        | (clientFluxLimit & 0xFFFFFFFFL);
            }
        };
        this.addDataSlot(fluxLimitHighSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && blockEntity.isOwnedBy(player.getUUID())
                && blockEntity.getLevel() == player.level()
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    public SourceVeinBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getSideModeId(Direction direction) {
        return direction == null
                ? SourceVeinBlockEntity.SourceSideMode.DISABLED.persistedId()
                : sideModeSlots[direction.ordinal()].get();
    }

    public long getFluxLimit() {
        if (serverBacked()) return blockEntity.getFluxLimit();
        long low = fluxLimitLowSlot.get() & 0xFFFFFFFFL;
        long high = (long) (fluxLimitHighSlot.get() & 0x7FFFFFFF) << 32;
        return high | low;
    }

    public boolean isSideFaulted(Direction direction) {
        return direction != null && sideFaultSlots[direction.ordinal()].get() != 0;
    }

    public long getSideUncertainInFlight(Direction direction) {
        if (direction == null) return 0L;
        int index = direction.ordinal();
        long low = sideInFlightLowSlots[index].get() & 0xFFFFFFFFL;
        long high = (long) sideInFlightHighSlots[index].get() << 32;
        return Math.max(0L, high | low);
    }

    private boolean serverBacked() {
        return blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide();
    }

    private static SourceVeinBlockEntity resolveBlockEntity(Player player, FriendlyByteBuf buffer) {
        if (player == null || buffer == null) return null;
        if (player.level().getBlockEntity(buffer.readBlockPos()) instanceof SourceVeinBlockEntity sourceVein) {
            return sourceVein;
        }
        return null;
    }
}
