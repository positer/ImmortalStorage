package com.immortalstorage.immortalstorage.compat.pattern;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Freezes the "no exchange pattern may be published without a paid input" rule in pure data.
 *
 * <p>The rule is the one thing both integrations must agree on, and it is exactly the rule neither
 * third-party API enforces for us on the AE2 side: a custom {@code IPatternDetails} may legally
 * return an empty {@code getInputs()}, and AE2's crafting tree then treats the product as
 * craftable from nothing. The document records the bytecode evidence; this test keeps the rule
 * executable without loading any AE2 or Refined Storage class.</p>
 *
 * <p>{@link ExchangeBatchPattern} is a test-local mirror of the published shape, not production
 * code: it exists to make "one pattern = one paid batch" refutable in a unit test. The remaining
 * assertions are source-level guards on the real data record so the mirror cannot drift away from
 * {@code SourceExchangeOffer}'s own invariants.</p>
 */
final class ExchangePatternFreeOfferPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final String OFFER_SOURCE =
            "project/neoforge-1.21.1-mdk/src/main/java/com/immortalstorage/immortalstorage/source/SourceExchangeOffer.java";

    /**
     * One publishable exchange pattern: it consumes {@code inputUnits} of a paid resource and
     * produces {@code outputUnits}. Construction is the only way to get one, so an input-free
     * shape cannot exist anywhere a provider could see it.
     */
    record ExchangeBatchPattern(long inputUnits, long outputUnits) {
        static ExchangeBatchPattern of(long inputUnits, long outputUnits) {
            if (inputUnits <= 0L) {
                throw new IllegalArgumentException(
                        "an exchange pattern without an input would mint resources for free");
            }
            if (outputUnits <= 0L) {
                throw new IllegalArgumentException("output units must be positive");
            }
            return new ExchangeBatchPattern(inputUnits, outputUnits);
        }
    }

    @Test
    void aPaidOfferBecomesExactlyOneBatchPattern() {
        ExchangeBatchPattern pattern = ExchangeBatchPattern.of(8L, 1L);
        assertEquals(8L, pattern.inputUnits(), "the pattern bills the offer's yuanPerBatch");
        assertEquals(1L, pattern.outputUnits(), "the pattern yields the offer's outputsPerBatch");
    }

    @Test
    void aFreeOfferCannotBecomeAPatternAtAll() {
        // yuanPerBatch == 0 is a legal SourceExchangeOffer (free veins exist), but it has no
        // expressible AE2 input, and AE2's crafting tree would happily craft it for nothing.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ExchangeBatchPattern.of(0L, 1L),
                "a zero-input shape must be unconstructable, not merely filtered later");
        assertTrue(thrown.getMessage().contains("for free"),
                "the failure must name the exploit it prevents: " + thrown.getMessage());
    }

    @Test
    void negativeInputsAndNonPositiveOutputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ExchangeBatchPattern.of(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> ExchangeBatchPattern.of(8L, 0L));
        assertThrows(IllegalArgumentException.class, () -> ExchangeBatchPattern.of(8L, -3L));
    }

    @Test
    void theOfferRecordGuardsTheSameInvariants() throws Exception {
        Path source = locateRepositoryRoot().resolve(OFFER_SOURCE);
        assertTrue(Files.isRegularFile(source), "missing offer record source: " + source);
        String text = Files.readString(source);
        assertTrue(text.contains("if (yuanPerBatch < 0L)"),
                "a negative yuanPerBatch must stay rejected by the record itself");
        assertTrue(text.contains("if (outputsPerBatch <= 0L)"),
                "a non-positive outputsPerBatch must stay rejected by the record itself"
                        + " (both integrations build ResourceAmount/Ingredient, which require > 0)");
        assertTrue(text.contains("public boolean paid()"),
                "the 'is this vein paid' predicate must stay on the record so both sides share it");
        assertTrue(text.contains("return yuanPerBatch > 0L;"),
                "paid() must remain yuanPerBatch > 0L; a free vein is storage-only, never a pattern");
    }

    private static Path locateRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("CHANGELOG.md"))
                    && Files.isDirectory(current.resolve("project/neoforge-1.21.1-mdk"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate the workspace root above "
                + Path.of("").toAbsolutePath());
    }
}
