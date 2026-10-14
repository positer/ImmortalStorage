package com.immortalstorage.immortalstorage.dimension;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class RealmTickSafetyTest {
    @Test void denyTagsAffectExtraPassesOnlyAndOtherLevelsRemainUntouched() {
        Object level = new Object(), other = new Object(), target = new Object(); var calls = new AtomicInteger();
        try (var scope = RealmTickSafety.open(level)) {
            RealmTickSafety.execute(level, target, true, calls::incrementAndGet, failure -> fail(failure));
            RealmTickSafety.extra(level, () -> {
                RealmTickSafety.execute(level, target, true, calls::incrementAndGet, failure -> fail(failure));
                RealmTickSafety.execute(other, target, true, calls::incrementAndGet, failure -> fail(failure));
            });
        }
        assertEquals(2, calls.get()); assertFalse(RealmTickSafety.scoped(level));
    }
    @Test void mutualAndSelfReentryAreSkippedWithoutLosingSubsequentPasses() {
        Object level = new Object(), a = new Object(), b = new Object(); var calls = new AtomicInteger();
        try (var scope = RealmTickSafety.open(level)) {
            RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, a, false, () -> {
                calls.incrementAndGet();
                RealmTickSafety.execute(level, b, false, () -> {
                    calls.incrementAndGet();
                    RealmTickSafety.execute(level, a, false, calls::incrementAndGet, failure -> fail(failure));
                }, failure -> fail(failure));
            }, failure -> fail(failure)));
            RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, a, false, calls::incrementAndGet, failure -> fail(failure)));
        }
        assertEquals(3, calls.get());
    }
    @Test void failingExtraTickerIsQuarantinedAcrossFramesButKeepsItsOrdinaryTick() {
        Object level = new Object(), target = new Object(); var failures = new AtomicInteger(); var calls = new AtomicInteger();
        try (var scope = RealmTickSafety.open(level)) {
            RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, target, false,
                    () -> { throw new IllegalStateException("non-repeatable foreign ticker"); }, ignored -> failures.incrementAndGet()));
        }
        try (var scope = RealmTickSafety.open(level)) {
            RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, target, false, calls::incrementAndGet, ignored -> failures.incrementAndGet()));
            RealmTickSafety.execute(level, target, false, calls::incrementAndGet, ignored -> failures.incrementAndGet());
        }
        assertEquals(1, failures.get()); assertEquals(1, calls.get());
    }
    @Test void overflowInExtraTicksIsContainedAndScopeAlwaysUnwinds() {
        Object level = new Object(); var failures = new AtomicInteger();
        try (var scope = RealmTickSafety.open(level)) {
            RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, new Object(), false,
                    () -> { throw new StackOverflowError("foreign acceleration cycle"); }, ignored -> failures.incrementAndGet()));
        }
        assertEquals(1, failures.get()); assertFalse(RealmTickSafety.scoped(level));
    }
    @Test void ordinaryFailuresAndFatalErrorsAreNotSilenced() {
        Object level = new Object();
        try (var scope = RealmTickSafety.open(level)) {
            assertThrows(IllegalStateException.class, () -> RealmTickSafety.execute(level, new Object(), false,
                    () -> { throw new IllegalStateException(); }, ignored -> fail("ordinary errors must propagate")));
            assertThrows(OutOfMemoryError.class, () -> RealmTickSafety.extra(level, () -> RealmTickSafety.execute(level, new Object(), false,
                    () -> { throw new OutOfMemoryError("synthetic fatal error"); }, ignored -> fail("fatal errors must propagate"))));
        }
        assertFalse(RealmTickSafety.scoped(level));
    }
}
