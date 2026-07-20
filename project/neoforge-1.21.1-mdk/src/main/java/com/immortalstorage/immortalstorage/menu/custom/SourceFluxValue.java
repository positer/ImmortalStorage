package com.immortalstorage.immortalstorage.menu.custom;

/** Shared validation for the source-vein throughput editor and server handler. */
public final class SourceFluxValue {
    public static final long MIN_VALUE = 0L;
    public static final long MAX_VALUE = Long.MAX_VALUE;

    public enum Error {
        NONE,
        EMPTY,
        NOT_AN_INTEGER,
        OUT_OF_RANGE
    }

    public record ParseResult(long value, Error error, boolean saturated) {
        public ParseResult(long value, Error error) {
            this(value, error, false);
        }

        public boolean valid() {
            return error == Error.NONE;
        }
    }

    public static ParseResult parse(String text) {
        if (text == null || text.isEmpty()) {
            return new ParseResult(0L, Error.EMPTY);
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character < '0' || character > '9') {
                return new ParseResult(0L, Error.NOT_AN_INTEGER);
            }
        }
        long parsed = 0L;
        for (int index = 0; index < text.length(); index++) {
            int digit = text.charAt(index) - '0';
            if (parsed > (MAX_VALUE - digit) / 10L) {
                return new ParseResult(MAX_VALUE, Error.NONE, true);
            }
            parsed = parsed * 10L + digit;
        }
        return new ParseResult(parsed, Error.NONE);
    }

    /** Treat every packet value as untrusted and force it into the persisted domain. */
    public static long clamp(long requested) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, requested));
    }

    private SourceFluxValue() {
    }
}
