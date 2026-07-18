package com.cultivation.cultivation.player.yuan;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class YuanAccountTest {
    @Test
    void generationReturnsMaterializationRequestsWithoutKeepingABalance() {
        YuanAccount account = new YuanAccount();
        account.configure(new YuanProfile(
                new YuanRule(4L, 3, 2L),
                YuanRule.DISABLED));

        assertEquals(YuanGeneration.NONE, account.advanceGeneration(2));
        assertEquals(2, account.progress(YuanKind.TRUE));
        assertEquals(new YuanGeneration(2L, 0L), account.advanceGeneration(1));
        assertEquals(0, account.progress(YuanKind.TRUE));

        // The scheduler deliberately requests more than the profile cap. The
        // item-backed owner decides how many stacks can actually be inserted.
        assertEquals(new YuanGeneration(8L, 0L), account.advanceGeneration(12));
    }

    @Test
    void generationCatchUpIsArithmeticAndSaturatesInsteadOfOverflowing() {
        YuanAccount account = new YuanAccount();
        account.configure(new YuanProfile(
                new YuanRule(YuanRule.UNBOUNDED_CAP, 1, Long.MAX_VALUE),
                YuanRule.DISABLED));

        assertEquals(new YuanGeneration(Long.MAX_VALUE, 0L),
                account.advanceGeneration(Integer.MAX_VALUE));
        assertEquals(0, account.progress(YuanKind.TRUE));
    }

    @Test
    void disabledGenerationProducesNothingAndDoesNotExposeDormantProgress() {
        YuanAccount account = new YuanAccount();
        account.configure(new YuanProfile(new YuanRule(64L, 10, 1L), YuanRule.DISABLED));
        assertEquals(YuanGeneration.NONE, account.advanceGeneration(7));
        assertEquals(7, account.progress(YuanKind.TRUE));

        account.configure(YuanProfile.DISABLED);
        assertEquals(0, account.progress(YuanKind.TRUE));
        assertEquals(YuanGeneration.NONE, account.advanceGeneration(100));

        account.configure(new YuanProfile(new YuanRule(64L, 5, 1L), YuanRule.DISABLED));
        assertEquals(4, account.progress(YuanKind.TRUE));
    }

    @Test
    void ascensionConversionDiscardsIncompleteGroupsAndKeepsNoRemainderState() {
        YuanAccount account = new YuanAccount();

        assertEquals(0L, account.convertTrueToImmortal(1L));
        CompoundTag saved = account.save();
        assertFalse(saved.contains("trueToImmortalRemainder"));

        YuanAccount restored = new YuanAccount();
        restored.load(saved, YuanProfile.forStage(6, false));
        assertEquals(0L, restored.convertTrueToImmortal(15L),
                "the discarded ascension remainder must not complete a later group");
        assertEquals(2L, restored.convertTrueToImmortal(32L));
    }

    @Test
    void oldNumericBalancesBecomeUnclampedOneShotMigrationPending() {
        CompoundTag old = new CompoundTag();
        old.putLong("trueBalance", 999L);
        old.putLong("immortalBalance", 12L);

        YuanAccount account = new YuanAccount();
        account.load(old, YuanProfile.forStage(1, false));

        // Stage one has a 64 true-yuan cap and disables immortal yuan, but old
        // data must not be truncated before the item owner can materialize or drop it.
        assertEquals(999L, account.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(12L, account.drainLegacyBalance(YuanKind.IMMORTAL));
        assertEquals(0L, account.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(0L, account.drainLegacyBalance(YuanKind.IMMORTAL));
    }

    @Test
    void pendingMigrationSurvivesNewSchemaSaveUntilDrained() {
        YuanAccount account = new YuanAccount();
        account.loadLegacy(123L, 456L, YuanProfile.DISABLED);

        CompoundTag saved = account.save();
        assertEquals(4, saved.getInt("version"));
        assertFalse(saved.contains("trueBalance"));
        assertFalse(saved.contains("immortalBalance"));

        YuanAccount restored = new YuanAccount();
        restored.load(saved, YuanProfile.forStage(9, false));
        assertEquals(123L, restored.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(456L, restored.drainLegacyBalance(YuanKind.IMMORTAL));

        YuanAccount drainedRoundTrip = new YuanAccount();
        drainedRoundTrip.load(restored.save(), YuanProfile.forStage(9, false));
        assertEquals(0L, drainedRoundTrip.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(0L, drainedRoundTrip.drainLegacyBalance(YuanKind.IMMORTAL));
    }

    @Test
    void generationNeverConsumesOrCountsLegacyPending() {
        YuanAccount account = new YuanAccount();
        YuanProfile profile = new YuanProfile(new YuanRule(1L, 2, 3L), YuanRule.DISABLED);
        account.loadLegacy(60L, 7L, profile);

        assertEquals(new YuanGeneration(6L, 0L), account.advanceGeneration(4));
        assertEquals(60L, account.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(7L, account.drainLegacyBalance(YuanKind.IMMORTAL));
    }

    @Test
    void negativeLegacyValuesAndInvalidConversionInputsAreIgnored() {
        YuanAccount account = new YuanAccount();
        account.loadLegacy(-1L, Long.MIN_VALUE, YuanProfile.forStage(6, false));

        assertEquals(0L, account.drainLegacyBalance(YuanKind.TRUE));
        assertEquals(0L, account.drainLegacyBalance(YuanKind.IMMORTAL));
        assertEquals(0L, account.convertTrueToImmortal(0L));
        assertEquals(0L, account.convertTrueToImmortal(-16L));
    }

    @Test
    void runtimeBalanceMutationMethodsAreAbsent() {
        Set<String> methodNames = Stream.of(YuanAccount.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertFalse(methodNames.contains("balance"));
        assertFalse(methodNames.contains("deposit"));
        assertFalse(methodNames.contains("withdraw"));
        assertFalse(methodNames.contains("removeUpTo"));
        assertFalse(methodNames.contains("acceptTrueYuanAsImmortal"));
        assertFalse(methodNames.contains("convertAndClearStoredTrueYuan"));
        assertFalse(methodNames.contains("absorbLegacyItem"));
    }

    @Test
    void stageProfilesIgnoreLegacySpiritCoreFlag() {
        YuanProfile stageOne = YuanProfile.forStage(1, false);
        assertEquals(64L, stageOne.trueYuan().cap());
        assertEquals(1200, stageOne.trueYuan().generationIntervalTicks());
        assertEquals(2L, stageOne.trueYuan().generationAmount());

        assertEquals(stageOne, YuanProfile.forStage(1, true));
        assertEquals(YuanRule.UNBOUNDED_CAP, YuanProfile.forStage(6, false).trueYuan().cap());
        assertFalse(YuanProfile.forStage(6, false).trueYuan().generates());
        assertEquals(YuanRule.UNBOUNDED_CAP, YuanProfile.forStage(6, true).trueYuan().cap());
        assertEquals(YuanRule.UNBOUNDED_CAP, YuanProfile.forStage(9, false).immortalYuan().cap());
        assertEquals(YuanRule.UNBOUNDED_CAP, YuanProfile.forStage(10, false).trueYuan().cap());
        YuanProfile virtualStageTen = YuanProfile.forStage(10, false, true);
        assertFalse(virtualStageTen.immortalYuan().generates());
        YuanProfile finiteStageTen = YuanProfile.forStage(10, false, false);
        assertEquals(YuanRule.UNBOUNDED_CAP, finiteStageTen.immortalYuan().cap());
        assertEquals(20, finiteStageTen.immortalYuan().generationIntervalTicks());
        assertEquals(256L, finiteStageTen.immortalYuan().generationAmount());
    }
}
