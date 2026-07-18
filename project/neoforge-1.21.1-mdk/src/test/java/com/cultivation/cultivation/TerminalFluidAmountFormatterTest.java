package com.cultivation.cultivation;

import com.cultivation.cultivation.client.screen.TerminalFluidAmountFormatter;
import net.neoforged.neoforge.fluids.FluidType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalFluidAmountFormatterTest {
    @Test
    void formatsExactAndFractionalBucketsWithoutHardCodedBucketVolume() {
        int bucket = FluidType.BUCKET_VOLUME;
        assertEquals("0B", TerminalFluidAmountFormatter.format(0));
        assertEquals("0.001B", TerminalFluidAmountFormatter.format(1));
        assertEquals("0.25B", TerminalFluidAmountFormatter.format(bucket / 4L));
        assertEquals("0.999B", TerminalFluidAmountFormatter.format(bucket - 1L));
        assertEquals("1B", TerminalFluidAmountFormatter.format(bucket));
        assertEquals("1.25B", TerminalFluidAmountFormatter.format(bucket + bucket / 4L));
        assertEquals("64B", TerminalFluidAmountFormatter.format(bucket * 64L));
    }

    @Test
    void compactsLargeBucketCountsForSixteenPixelOverlays() {
        long bucket = FluidType.BUCKET_VOLUME;
        assertEquals("1.2kB", TerminalFluidAmountFormatter.format(bucket * 1_200L));
        assertEquals("3.4MB", TerminalFluidAmountFormatter.format(bucket * 3_400_000L));
        assertEquals("9.22PB", TerminalFluidAmountFormatter.format(Long.MAX_VALUE));
        assertTrue(TerminalFluidAmountFormatter.format(Long.MAX_VALUE).length() <= 6);
    }

    @Test
    void tooltipKeepsExactBucketAndMillibucketValues() {
        assertEquals("1.25 B", TerminalFluidAmountFormatter.exactBuckets(1_250L));
        assertEquals("1,250 mB", TerminalFluidAmountFormatter.exactMillibuckets(1_250L));
    }
}
