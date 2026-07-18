package com.cultivation.cultivation.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class XianqiaoInterfacePullPlanTest {
    @Test
    void simulationReservesOnlyTheAmountAcceptedByOwnerStorage() {
        assertEquals(40L, XianqiaoInterfacePullPlan.stagedAmount(64L, 40L));
        assertEquals(0L, XianqiaoInterfacePullPlan.stagedAmount(64L, 0L));
        assertEquals(64L, XianqiaoInterfacePullPlan.stagedAmount(64L, 64L));
    }

    @Test
    void impossibleSimulationResultsFailClosed() {
        assertThrows(IllegalStateException.class,
                () -> XianqiaoInterfacePullPlan.stagedAmount(64L, 65L));
        assertThrows(IllegalStateException.class,
                () -> XianqiaoInterfacePullPlan.stagedAmount(64L, -1L));
    }

    @Test
    void rollbackIsTheDifferenceBetweenExtractedAndCommitted() {
        assertEquals(0L, XianqiaoInterfacePullPlan.rollbackAmount(40L, 40L));
        assertEquals(8L, XianqiaoInterfacePullPlan.rollbackAmount(40L, 32L));
        assertThrows(IllegalStateException.class,
                () -> XianqiaoInterfacePullPlan.rollbackAmount(40L, 41L));
    }
}
