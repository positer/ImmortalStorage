package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;

/** One shared, server-owned limit source for interface storage and packets. */
public final class XianqiaoInterfaceLimits {
    public static final int DEFAULT_ITEM_TARGET = 128;
    public static final int DEFAULT_FLUID_TARGET_MB = 16_000;

    private XianqiaoInterfaceLimits() {
    }

    public static Snapshot current() {
        if (!ImmortalStorageConfig.SPEC.isLoaded()) return defaults();
        return new Snapshot(
                ImmortalStorageConfig.XIANQIAO_INTERFACE_ITEM_SLOT_LIMIT.get(),
                ImmortalStorageConfig.XIANQIAO_INTERFACE_FLUID_SLOT_LIMIT_MB.get());
    }

    public static Snapshot defaults() {
        return new Snapshot(DEFAULT_ITEM_TARGET, DEFAULT_FLUID_TARGET_MB);
    }

    public static int itemTargetLimit() {
        return current().itemTargetLimit();
    }

    public static int fluidTargetLimitMb() {
        return current().fluidTargetLimitMb();
    }

    public record Snapshot(int itemTargetLimit, int fluidTargetLimitMb) {
        public Snapshot {
            itemTargetLimit = Math.max(1, itemTargetLimit);
            fluidTargetLimitMb = Math.max(1, fluidTargetLimitMb);
        }
    }
}
