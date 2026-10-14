package com.immortalstorage.immortalstorage.compressor;

/** Exact batch arithmetic shared by local-slot and native Xianqiao transactions. */
public final class SpiritCompressionBatch {
    public static long multiplier(long available, long inputPerBatch, long outputPerBatch, long outputCapacity) {
        if (available < 0 || inputPerBatch <= 0 || outputPerBatch <= 0 || outputCapacity <= 0) return 0;
        return Math.min(available / inputPerBatch, outputCapacity / outputPerBatch);
    }
    private SpiritCompressionBatch() {}
}
