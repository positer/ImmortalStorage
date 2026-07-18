package com.cultivation.cultivation.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalRealmTickBudgetTest {
    @Test
    void clampsDimensionScaleWithoutTouchingGlobalTickRate() {
        assertEquals(0.0D, PersonalRealmServerLevel.clampTickScale(-5.0D));
        assertEquals(1.0D, PersonalRealmServerLevel.clampTickScale(Double.NaN));
        assertEquals(32.0D, PersonalRealmServerLevel.clampTickScale(Double.POSITIVE_INFINITY));
        assertEquals(32.0D, PersonalRealmServerLevel.clampTickScale(200.0D));
        assertEquals(4.0D, PersonalRealmServerLevel.clampTickScale(4.0D));
    }

    @Test
    void zeroScaleFreezesOnlyThisDimensionBudget() {
        PersonalRealmServerLevel.TickBudget budget = new PersonalRealmServerLevel.TickBudget();
        budget.activate(0.0D);

        for (int frame = 0; frame < 40; frame++) {
            assertEquals(0, budget.consumePasses());
        }
    }

    @Test
    void fractionalSlowdownUsesDeterministicDimensionPacing() {
        PersonalRealmServerLevel.TickBudget budget = new PersonalRealmServerLevel.TickBudget();
        budget.activate(0.1D);

        int passes = 0;
        for (int frame = 0; frame < 100; frame++) {
            passes += budget.consumePasses();
        }
        assertEquals(10, passes);

        budget.activate(0.2D);
        passes = 0;
        for (int frame = 0; frame < 100; frame++) {
            passes += budget.consumePasses();
        }
        assertEquals(20, passes);

        budget.activate(0.5D);
        passes = 0;
        for (int frame = 0; frame < 100; frame++) {
            passes += budget.consumePasses();
        }
        assertEquals(50, passes);
    }

    @Test
    void preservesFractionalPassesAcrossServerFrames() {
        PersonalRealmServerLevel.TickBudget budget = new PersonalRealmServerLevel.TickBudget();
        budget.activate(1.5D);

        assertEquals(1, budget.consumePasses());
        assertEquals(2, budget.consumePasses());
        assertEquals(1, budget.consumePasses());
        assertEquals(2, budget.consumePasses());
    }

    @Test
    void capsWorkAndRestoresExactlyOnePass() {
        PersonalRealmServerLevel.TickBudget budget = new PersonalRealmServerLevel.TickBudget();
        budget.activate(999.0D);
        assertEquals(32, budget.consumePasses());
        assertEquals(32, budget.consumePasses());

        budget.restore();
        assertEquals(1, budget.consumePasses());
        assertEquals(1, budget.consumePasses());
    }
}
