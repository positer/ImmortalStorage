package com.cultivation.cultivation.item.custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpiritSwordCombatModelTest {
    @Test
    void stageProfilesMatchCombatAndTooltipContract() {
        var mortal = SpiritSwordCombatModel.forStage(0);
        assertEquals(SpiritSwordCombatModel.YuanCost.NONE, mortal.cost());
        assertEquals(0.0F, mortal.bonusDamage());

        var fifth = SpiritSwordCombatModel.forStage(5);
        assertEquals(SpiritSwordCombatModel.YuanCost.TRUE, fifth.cost());
        assertEquals(5L, fifth.costAmount());
        assertEquals(32.0F, fifth.bonusDamage());
        assertEquals(37.0F, fifth.successfulHitDamage());

        var ninth = SpiritSwordCombatModel.forStage(9);
        assertEquals(SpiritSwordCombatModel.YuanCost.IMMORTAL, ninth.cost());
        assertEquals(9L, ninth.costAmount());
        assertEquals(512.0F, ninth.bonusDamage());

        var tenth = SpiritSwordCombatModel.forStage(10);
        assertEquals(SpiritSwordCombatModel.YuanCost.NONE, tenth.cost());
        assertEquals(0L, tenth.costAmount());
        assertEquals(1024.0F, tenth.bonusDamage());
    }
}
