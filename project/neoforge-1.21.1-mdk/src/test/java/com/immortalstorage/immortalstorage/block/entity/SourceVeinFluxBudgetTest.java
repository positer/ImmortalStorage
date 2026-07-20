package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SourceVeinFluxBudgetTest {
    @Test
    void multipleClaimsOnOneChannelShareItsTickAllowance() {
        SourceVeinFluxBudget budget = new SourceVeinFluxBudget();

        assertEquals(40, budget.claim(200, 64, 40, false)); // first call on one face/channel
        assertEquals(24, budget.claim(200, 64, 40, false)); // second call on that same face/channel
        assertEquals(0, budget.claim(200, 64, 1, false));
        assertEquals(64, budget.spent(200));
    }

    @Test
    void simulationNeverConsumesTheChannelBudget() {
        SourceVeinFluxBudget budget = new SourceVeinFluxBudget();

        assertEquals(64, budget.claim(9, 64, 64, true));
        assertEquals(64, budget.claim(9, 64, 64, true));
        assertEquals(64, budget.claim(9, 64, 64, false));
        assertEquals(0, budget.claim(9, 64, 1, false));
    }

    @Test
    void aNewGameTickResetsTheBudgetAndKnownRefusalsCanBeRefunded() {
        SourceVeinFluxBudget budget = new SourceVeinFluxBudget();

        assertEquals(64, budget.claim(20, 64, 64, false));
        budget.refund(20, 16);
        assertEquals(16, budget.claim(20, 64, 64, false));
        assertEquals(64, budget.claim(21, 64, 64, false));
    }

    @Test
    void loweringTheLimitMidTickCannotExposeMoreThanLimitMinusSpent() {
        SourceVeinFluxBudget budget = new SourceVeinFluxBudget();

        assertEquals(48, budget.claim(30, 64, 48, false));
        assertEquals(0, budget.available(30, 32));
        assertEquals(16, budget.available(30, 64));
    }
}
