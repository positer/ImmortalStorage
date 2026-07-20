package com.immortalstorage.immortalstorage.block.entity;

/** Pure transaction arithmetic shared by active item/fluid pull paths. */
final class XianqiaoInterfacePullPlan {
    static long stagedAmount(long offered, long simulatedAcceptance) {
        requireBounded("simulation", simulatedAcceptance, offered);
        return simulatedAcceptance;
    }

    static long rollbackAmount(long extracted, long committed) {
        requireBounded("commit", committed, extracted);
        return extracted - committed;
    }

    private static void requireBounded(String phase, long value, long maximum) {
        if (maximum < 0L || value < 0L || value > maximum) {
            throw new IllegalStateException(
                    "interface pull " + phase + " returned " + value + " for " + maximum);
        }
    }

    private XianqiaoInterfacePullPlan() {
    }
}
