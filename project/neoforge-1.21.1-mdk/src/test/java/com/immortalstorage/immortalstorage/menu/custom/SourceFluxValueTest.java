package com.immortalstorage.immortalstorage.menu.custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceFluxValueTest {
    @Test
    void parserAcceptsBothPersistedBoundariesAndLeadingZeroes() {
        SourceFluxValue.ParseResult minimum = SourceFluxValue.parse("0");
        SourceFluxValue.ParseResult maximum = SourceFluxValue.parse("9223372036854775807");
        SourceFluxValue.ParseResult padded = SourceFluxValue.parse("00064");

        assertTrue(minimum.valid());
        assertEquals(SourceFluxValue.MIN_VALUE, minimum.value());
        assertTrue(maximum.valid());
        assertEquals(SourceFluxValue.MAX_VALUE, maximum.value());
        assertTrue(padded.valid());
        assertEquals(64L, padded.value());
    }

    @Test
    void parserRejectsEmptyNonIntegerAndNegativeInput() {
        assertInvalid("", SourceFluxValue.Error.EMPTY);
        assertInvalid(" ", SourceFluxValue.Error.NOT_AN_INTEGER);
        assertInvalid("-1", SourceFluxValue.Error.NOT_AN_INTEGER);
        assertInvalid("1.5", SourceFluxValue.Error.NOT_AN_INTEGER);
        assertInvalid("64/t", SourceFluxValue.Error.NOT_AN_INTEGER);
    }

    @Test
    void parserPreservesLongValuesAndSaturatesOnlyBeyondTheSignedLongMaximum() {
        SourceFluxValue.ParseResult onePastInt = SourceFluxValue.parse("2147483648");
        SourceFluxValue.ParseResult onePastLong = SourceFluxValue.parse("9223372036854775808");
        SourceFluxValue.ParseResult farPastLong = SourceFluxValue.parse(
                "9999999999999999999999999999999999999999999999999999999999999999");
        SourceFluxValue.ParseResult paddedLong = SourceFluxValue.parse(
                "0000000000000000000000000000000000000000000000000000002147483648");

        assertTrue(onePastInt.valid());
        assertEquals(2_147_483_648L, onePastInt.value());
        assertFalse(onePastInt.saturated());
        assertSaturated(onePastLong);
        assertSaturated(farPastLong);
        assertEquals(2_147_483_648L, paddedLong.value());
        assertFalse(paddedLong.saturated());
    }

    @Test
    void serverClampContainsEveryUntrustedPacketValue() {
        assertEquals(SourceFluxValue.MIN_VALUE, SourceFluxValue.clamp(Long.MIN_VALUE));
        assertEquals(SourceFluxValue.MIN_VALUE, SourceFluxValue.clamp(-1L));
        assertEquals(64L, SourceFluxValue.clamp(64L));
        assertEquals((long) Integer.MAX_VALUE + 1L,
                SourceFluxValue.clamp((long) Integer.MAX_VALUE + 1L));
        assertEquals(SourceFluxValue.MAX_VALUE, SourceFluxValue.clamp(Long.MAX_VALUE));
    }

    private static void assertInvalid(String input, SourceFluxValue.Error expected) {
        SourceFluxValue.ParseResult parsed = SourceFluxValue.parse(input);
        assertFalse(parsed.valid());
        assertEquals(expected, parsed.error());
    }

    private static void assertSaturated(SourceFluxValue.ParseResult parsed) {
        assertTrue(parsed.valid(), "digit-only values above the supported range are valid saturated input");
        assertEquals(SourceFluxValue.MAX_VALUE, parsed.value());
    }
}
