package com.immortalstorage.immortalstorage.block.custom;

/** Compile-time enumeration of every "   " vein kind. The block and its
 *  block entity both use this to decide what to produce, how to scale the
 *  internal buffer, and how to charge the player. */
public enum VeinKind {
    WATER(0, 64, true, 0, true, false),
    MILK(0, 64, true, 0, true, false),
    LAVA(0, 64, true, 6, true, false),
    COBBLE(0, 64, true, 0, false, false),
    STONE(1, 64, false, 6, false, false),
    SMOOTH_STONE(1, 64, false, 7, false, false),
    WHITE_CONCRETE(0, 64, true, 0, false, false),
    ORANGE_CONCRETE(0, 64, true, 0, false, false),
    MAGENTA_CONCRETE(0, 64, true, 0, false, false),
    LIGHT_BLUE_CONCRETE(0, 64, true, 0, false, false),
    YELLOW_CONCRETE(0, 64, true, 0, false, false),
    LIME_CONCRETE(0, 64, true, 0, false, false),
    PINK_CONCRETE(0, 64, true, 0, false, false),
    GRAY_CONCRETE(0, 64, true, 0, false, false),
    LIGHT_GRAY_CONCRETE(0, 64, true, 0, false, false),
    CYAN_CONCRETE(0, 64, true, 0, false, false),
    PURPLE_CONCRETE(0, 64, true, 0, false, false),
    BLUE_CONCRETE(0, 64, true, 0, false, false),
    BROWN_CONCRETE(0, 64, true, 0, false, false),
    GREEN_CONCRETE(0, 64, true, 0, false, false),
    RED_CONCRETE(0, 64, true, 0, false, false),
    BLACK_CONCRETE(0, 64, true, 0, false, false),
    DIRT(0, 64, true, 6, false, false),
    OAK_LOG(1, 64, false, 6, false, false),
    COAL(1, 32, false, 6, false, false),
    RAW_COPPER(1, 16, false, 6, false, false),
    RAW_IRON(1, 16, false, 7, false, false),
    RAW_GOLD(1, 8, false, 7, false, false),
    LAPIS(1, 32, false, 7, false, false),
    REDSTONE(1, 64, false, 7, false, false),
    CRUDE_SPIRIT_IRON(1, 16, false, 8, false, false),
    SPIRIT_CRYSTAL(1, 4, false, 8, false, false),
    DIAMOND(1, 4, false, 8, false, false),
    EMERALD(1, 4, false, 8, false, false),
    ECHO_SHARD(16, 1, false, 8, false, false),
    ANCIENT_DEBRIS(1, 1, false, 9, false, false),
    NETHER_STAR(8, 1, false, 9, false, false),
    ENCHANTED_GOLDEN_APPLE(64, 1, false, 9, false, false),
    DRAGON_EGG(64, 1, false, 9, false, false);

    /** Immortal yuan charged per output batch. 0 = free. */
    public final long yuanCostPerBatch;
    /** Number of outputs covered by one charged batch. */
    public final int outputsPerBatch;
    public final boolean noCost;
    public final int minStage;
    public final boolean fluid;
    public final boolean stickyFlag;

    VeinKind(long yuanCostPerBatch, int outputsPerBatch, boolean noCost, int minStage, boolean fluid, boolean sticky) {
        this.yuanCostPerBatch = yuanCostPerBatch;
        this.outputsPerBatch = Math.max(1, outputsPerBatch);
        this.noCost = noCost;
        this.minStage = minStage;
        this.fluid = fluid;
        this.stickyFlag = sticky;
    }
}
