package com.immortalstorage.immortalstorage.compressor;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpiritCompressionInputsTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private SpiritCompressionInputs.Requirement needs(long count, int... slots) {
        return new SpiritCompressionInputs.Requirement(count, slot -> java.util.Arrays.stream(slots).anyMatch(s -> s == slot));
    }
    @Test void activationConsumesEightYuanAndOneBasePerWholeBatch() {
        var plan = SpiritCompressionInputs.maximum(new int[]{64, 16, 64}, List.of(needs(8, 0, 1), needs(1, 2)));
        assertEquals(10, plan.multiplier()); assertArrayEquals(new int[]{64,16,10},plan.consumed());
        assertNull(SpiritCompressionInputs.maximum(new int[]{7,64}, List.of(needs(8,0),needs(1,1))));
    }
    @Test void overlappingTagsReassignStockInsteadOfGreedilyStarvingTheSpecificInput() {
        var plan=SpiritCompressionInputs.maximum(new int[]{4,4},List.of(needs(1,0,1),needs(1,0)));
        assertEquals(4,plan.multiplier());assertArrayEquals(new int[]{4,4},plan.consumed());
    }
    @Test void sharedStockIsNeverCountedTwiceAndPlanningDoesNotMutateIt() {
        int[] stock={9};var plan=SpiritCompressionInputs.maximum(stock,List.of(needs(8,0),needs(1,0)));
        assertEquals(1,plan.multiplier());assertArrayEquals(new int[]{9},plan.consumed());assertArrayEquals(new int[]{9},stock);
        assertNull(SpiritCompressionInputs.maximum(new int[]{8},List.of(needs(8,0),needs(1,0))));
    }
    @Test void singleInputStillProcessesTheEntireMatchingSlot() {
        var plan=SpiritCompressionInputs.maximum(new int[]{64},List.of(needs(1,0)));
        assertEquals(64,plan.multiplier());assertArrayEquals(new int[]{64},plan.consumed());
    }
}
